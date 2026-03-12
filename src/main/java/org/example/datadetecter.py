# -*- coding: utf-8 -*-
"""
代码逻辑总览（datadetecter）：
1. 读取交易数据并做特征工程，将原始字段统一转换为可计算的数值特征。
2. 先执行规则引擎（硬规则）快速筛出高置信异常，例如非法节点、Gas 超限、状态码异常等。
3. 在未被规则命中的“相对干净样本”上，依次运行多种统计/机器学习检测器：
   Isolation Forest、Autoencoder 重构误差、LOF、EllipticEnvelope。
4. 再补充两类行为检测：账户 Nonce 序列异常、区块突发/失败风暴异常。
5. 每个检测器只记录自己的命中信号（hit），并通过统一的 reason_chain 追踪证据链。
6. 最后做加权融合（ensemble），计算投票数与融合分，输出异常类型、异常等级和最终报告文件。

设计原则：
- 规则命中为强证据，优先标记为异常；
- 统计/行为信号可作为软证据，由融合阶段统一晋升；
- 新检测器不覆盖已有硬命中，尽量减少相互“抢标签”导致的信息丢失。
"""

import pandas as pd
import numpy as np
import warnings
import sqlite3
from sklearn.ensemble import IsolationForest
from sklearn.neighbors import LocalOutlierFactor
from sklearn.covariance import EllipticEnvelope
from sklearn.neural_network import MLPRegressor
from sklearn.preprocessing import MinMaxScaler

# ================= 配置区 =================
INPUT_FILE = "fisco_bcos_mock_data.csv"
OUTPUT_FILE = "final_detection_report.csv"
DEFAULT_DB_PATH = "blockchain_data.db"

# 按实际环境替换为合法共识节点（sealer）ID。
VALID_SEALERS = [
    "0x8546e111693c04f9853926647c87765c",
    "0x1257f222784d15e0964137758d98876d",
    "0x3368a333895e26f1075248869e09987e",
    "0x4479b444906f37a2186359970f1aa98f",
]

THRESHOLDS = {
    # 规则与模型阈值，集中管理便于统一调参
    "MAX_GAS": 25_000_000,
    "MAX_INPUT_LEN": 10_000,
    "FORBIDDEN_STATUS": ["0x16", "0x1", "1"],
    "MAX_GAS_RATIO": 1.15,
    "IFOREST_CONTAMINATION": 0.02,
    "LOF_CONTAMINATION": 0.015,
    "ELLIPTIC_CONTAMINATION": 0.01,
    "ROBUST_Z_MAX": 5.0,
    "BLOCK_TX_Z_MAX": 4.5,
    "BLOCK_FAIL_RATE_MAX": 0.80,
    "MIN_SAMPLE_ML": 30,
    "ENSEMBLE_SCORE_MIN": 0.72,
    "ENSEMBLE_VOTE_MIN": 2,
    "SEMANTIC_BURST_Z_MAX": 4.0,
    "NETWORK_LATENCY_MULTIPLIER": 3.0,
    "NETWORK_BLOCK_INTERVAL_MULTIPLIER": 3.0,
}

DETECTOR_WEIGHTS = {
    # 融合阶段的检测器权重（值越大，影响越强）
    "rule": 1.00,
    "iforest": 0.60,
    "autoencoder": 0.68,
    "lof": 0.55,
    "elliptic": 0.58,
    "robust": 0.45,
    "nonce": 0.48,
    "block": 0.52,
    "semantic_rule": 0.66,
    "semantic_anomaly": 0.58,
    "network": 0.54,
}

RUNTIME_VALID_SEALERS = None


# ================= 工具函数 =================
def parse_numeric(value):
    """将 int/float/十六进制字符串统一解析为 float，失败时回退为 0。"""
    try:
        if pd.isna(value):
            return 0.0
        s = str(value).strip().lower()
        if s == "" or s == "none" or s == "nan":
            return 0.0
        if s.startswith("0x"):
            return float(int(s, 16))
        return float(s)
    except Exception:
        return 0.0


def normalize_node_id(node_id):
    """统一节点 ID 格式，便于不同来源匹配。"""
    if node_id is None:
        return ""
    s = str(node_id).strip().lower()
    if s.startswith("0x"):
        s = s[2:]
    return s


def load_valid_sealers_from_db(db_path=DEFAULT_DB_PATH):
    """
    从 Java 侧写入的 nodes 表读取共识节点白名单。
    表结构参考 NodeInfoCollector.java: nodes(node_id, role, update_time)
    """
    try:
        conn = sqlite3.connect(db_path)
        try:
            df = pd.read_sql_query(
                "SELECT node_id FROM nodes WHERE role='Sealer' AND node_id IS NOT NULL",
                conn,
            )
        finally:
            conn.close()
        ids = [normalize_node_id(v) for v in df["node_id"].tolist()]
        ids = [v for v in ids if v]
        return sorted(set(ids))
    except Exception:
        return []


def get_effective_valid_sealers(db_path=DEFAULT_DB_PATH):
    """优先使用链上同步白名单，失败时回退到静态配置。"""
    global RUNTIME_VALID_SEALERS
    if RUNTIME_VALID_SEALERS is not None:
        return RUNTIME_VALID_SEALERS

    db_sealers = load_valid_sealers_from_db(db_path)
    if db_sealers:
        RUNTIME_VALID_SEALERS = db_sealers
        print(f"   [Whitelist] Loaded {len(db_sealers)} sealers from DB nodes table.")
    else:
        RUNTIME_VALID_SEALERS = [normalize_node_id(v) for v in VALID_SEALERS if normalize_node_id(v)]
        print("   [Whitelist] nodes table empty/unavailable, fallback to static VALID_SEALERS.")
    return RUNTIME_VALID_SEALERS


def robust_zscore(series):
    """
    基于中位数和 MAD 的稳健 z-score。
    公式：z = 0.6745 * (x - median) / MAD
    相比均值/标准差，对极端值更不敏感。
    """
    s = pd.to_numeric(series, errors="coerce").fillna(0.0)
    median = np.median(s)
    mad = np.median(np.abs(s - median))
    if mad < 1e-9:
        return pd.Series(np.zeros(len(s)), index=s.index)
    return 0.6745 * (s - median) / mad


def mark_new_anomalies(df, candidate_indices, detected_indices, label, score):
    """
    仅标记“新增异常”，避免覆盖此前已经命中的强证据标签。
    """
    new_indices = list(set(candidate_indices) - set(detected_indices))
    if new_indices:
        df.loc[new_indices, "anomaly_type"] = label
        df.loc[new_indices, "anomaly_score"] = float(score)
    detected_indices = list(set(detected_indices) | set(new_indices))
    return df, detected_indices, len(new_indices)


def append_reason_series(reason_series, detector_tag):
    """将检测器标签追加到 reason_chain（分号分隔）中，用于证据追踪。"""
    tag = str(detector_tag)
    return reason_series.apply(lambda x: tag if not x else f"{x};{tag}")


def init_tracking_columns(df):
    """初始化检测追踪字段：各检测器命中位、融合分、等级、原因链。"""
    track_cols = [
        "rule_hit",
        "semantic_rule_hit",
        "semantic_anomaly_hit",
        "iforest_hit",
        "autoencoder_hit",
        "lof_hit",
        "elliptic_hit",
        "robust_hit",
        "nonce_hit",
        "block_hit",
        "network_hit",
    ]
    for col in track_cols:
        df[col] = 0
    df["ensemble_votes"] = 0
    df["ensemble_score"] = 0.0
    df["anomaly_level"] = "INFO"
    df["reason_chain"] = ""
    return df


def mark_detector_hits(df, candidate_indices, hit_col, reason_tag):
    """记录某检测器命中，不直接决定最终结论，由融合阶段统一裁决。"""
    if not candidate_indices:
        return df
    df.loc[candidate_indices, hit_col] = 1
    df.loc[candidate_indices, "reason_chain"] = append_reason_series(
        df.loc[candidate_indices, "reason_chain"], reason_tag
    )
    return df


def finalize_ensemble(df):
    """
    融合多检测器证据，生成最终分数和风险等级。
    - 规则命中视为硬异常；
    - 软信号通过投票数 + 加权分进行晋升。
    """
    hit_cols = [
        "rule_hit",
        "semantic_rule_hit",
        "semantic_anomaly_hit",
        "iforest_hit",
        "autoencoder_hit",
        "lof_hit",
        "elliptic_hit",
        "robust_hit",
        "nonce_hit",
        "block_hit",
        "network_hit",
    ]

    df["ensemble_votes"] = df[hit_cols].sum(axis=1)

    weighted = (
        df["rule_hit"] * DETECTOR_WEIGHTS["rule"]
        + df["semantic_rule_hit"] * DETECTOR_WEIGHTS["semantic_rule"]
        + df["semantic_anomaly_hit"] * DETECTOR_WEIGHTS["semantic_anomaly"]
        + df["iforest_hit"] * DETECTOR_WEIGHTS["iforest"]
        + df["autoencoder_hit"] * DETECTOR_WEIGHTS["autoencoder"]
        + df["lof_hit"] * DETECTOR_WEIGHTS["lof"]
        + df["elliptic_hit"] * DETECTOR_WEIGHTS["elliptic"]
        + df["robust_hit"] * DETECTOR_WEIGHTS["robust"]
        + df["nonce_hit"] * DETECTOR_WEIGHTS["nonce"]
        + df["block_hit"] * DETECTOR_WEIGHTS["block"]
        + df["network_hit"] * DETECTOR_WEIGHTS["network"]
    )
    df["ensemble_score"] = weighted.clip(upper=1.0)
    df["anomaly_score"] = np.maximum(df["anomaly_score"], df["ensemble_score"])

    # 当软信号足够一致时，将 Normal 晋升为融合异常
    promote_mask = (
        (df["anomaly_type"] == "Normal")
        & (df["ensemble_votes"] >= THRESHOLDS["ENSEMBLE_VOTE_MIN"])
        & (df["ensemble_score"] >= THRESHOLDS["ENSEMBLE_SCORE_MIN"])
    )
    if promote_mask.any():
        df.loc[promote_mask, "anomaly_type"] = "Fusion: MultiSignal_Anomaly"

    # 根据最终分数映射风险等级
    df["anomaly_level"] = np.where(
        df["anomaly_score"] >= 0.90,
        "CRITICAL",
        np.where(
            df["anomaly_score"] >= 0.75,
            "HIGH",
            np.where(df["anomaly_score"] >= 0.55, "MEDIUM", "INFO"),
        ),
    )
    df.loc[df["anomaly_type"] == "Normal", "anomaly_level"] = "INFO"
    return df


# ================= 数据加载与特征工程 =================
def prepare_features(df):
    """对原始交易数据做统一字段对齐与特征工程。"""
    required = ["sealer", "status", "input_data", "gas_used", "nonce"]
    missing = [c for c in required if c not in df.columns]
    if missing:
        raise ValueError(f"Missing required columns: {missing}")

    # DB 数据使用 from_addr/to_addr，检测逻辑统一使用 from/to。
    if "from" not in df.columns and "from_addr" in df.columns:
        df["from"] = df["from_addr"]
    if "to" not in df.columns and "to_addr" in df.columns:
        df["to"] = df["to_addr"]

    # 基础特征：长度、Gas、Nonce、状态码
    df["feat_input_len"] = df["input_data"].astype(str).apply(len)
    df["feat_gas_used"] = df["gas_used"].apply(parse_numeric)
    df["feat_nonce"] = df["nonce"].apply(parse_numeric)
    df["feat_status_code"] = df["status"].apply(
        lambda x: 1 if str(x).lower() in {"0x16", "0x1", "1", "16"} else 0
    )

    # 扩展特征：Gas 使用比例、区块交易数
    if "gas_limit" in df.columns:
        df["feat_gas_limit"] = df["gas_limit"].apply(parse_numeric)
    else:
        df["feat_gas_limit"] = 0.0

    df["feat_gas_ratio"] = np.where(
        df["feat_gas_limit"] > 0,
        df["feat_gas_used"] / df["feat_gas_limit"],
        0.0,
    )

    if "block_trade_count" in df.columns:
        df["feat_block_trade_count"] = df["block_trade_count"].apply(parse_numeric)
    else:
        df["feat_block_trade_count"] = 0.0

    # 时间特征：用于后续序列行为分析（无 timestamp 时用行号代替）
    if "timestamp" in df.columns:
        df["feat_ts"] = df["timestamp"].apply(parse_numeric)
    else:
        df["feat_ts"] = np.arange(len(df), dtype=float)

    # 语义层特征（由 Java 语义解码层写库后关联得到）
    if "method_name" in df.columns:
        df["semantic_method_name"] = df["method_name"].fillna("Unknown")
    else:
        df["semantic_method_name"] = "Unknown"
    if "method_id" in df.columns:
        df["semantic_method_id"] = df["method_id"].fillna("")
    else:
        df["semantic_method_id"] = ""
    if "is_sensitive" in df.columns:
        df["feat_semantic_sensitive"] = df["is_sensitive"].apply(parse_numeric)
    else:
        df["feat_semantic_sensitive"] = 0.0
    if "decode_success" in df.columns:
        df["feat_semantic_decode_success"] = df["decode_success"].apply(parse_numeric)
    else:
        df["feat_semantic_decode_success"] = 0.0

    # 网络层观测特征（由 Java 实时采集层写入 network_metrics）
    if "rpc_latency_ms" in df.columns:
        df["feat_rpc_latency"] = df["rpc_latency_ms"].apply(parse_numeric)
    else:
        df["feat_rpc_latency"] = 0.0
    if "block_interval_ms" in df.columns:
        df["feat_block_interval"] = df["block_interval_ms"].apply(parse_numeric)
    else:
        df["feat_block_interval"] = 0.0
    if "net_fail_rate" in df.columns:
        df["feat_network_fail_rate"] = df["net_fail_rate"].apply(parse_numeric)
    else:
        df["feat_network_fail_rate"] = 0.0

    print(f"   Loaded {len(df)} rows.")
    return df


def load_data(filepath):
    print("1. [Data Load] Loading transaction data from CSV...")
    return prepare_features(pd.read_csv(filepath))


def load_data_from_db(db_path=DEFAULT_DB_PATH, table_name="transactions", limit=None):
    print("1. [Data Load] Loading transaction data from DB...")
    conn = sqlite3.connect(db_path)
    try:
        sql = f"SELECT * FROM {table_name}"
        if limit is not None and int(limit) > 0:
            sql += f" ORDER BY id DESC LIMIT {int(limit)}"
        raw_df = pd.read_sql_query(sql, conn)
    finally:
        conn.close()

    if raw_df.empty:
        print("   Loaded 0 rows.")
        return raw_df

    if limit is not None and int(limit) > 0:
        raw_df = raw_df.sort_values("id").reset_index(drop=True)
    raw_df = attach_semantic_features_from_db(raw_df, db_path=db_path)
    raw_df = attach_network_features_from_db(raw_df, db_path=db_path)
    return prepare_features(raw_df)


def attach_semantic_features_from_db(df, db_path=DEFAULT_DB_PATH):
    """按 tradehash 关联语义解码层输出。"""
    if df.empty or "tradehash" not in df.columns:
        return df
    conn = sqlite3.connect(db_path)
    try:
        semantic_df = pd.read_sql_query(
            """
            SELECT tradehash, method_id, method_name, is_sensitive, decode_success, call_type
            FROM semantic_calls
            """,
            conn,
        )
    except Exception:
        conn.close()
        return df
    finally:
        try:
            conn.close()
        except Exception:
            pass
    if semantic_df.empty:
        return df
    semantic_df = (
        semantic_df.dropna(subset=["tradehash"])
        .drop_duplicates(subset=["tradehash"], keep="last")
        .reset_index(drop=True)
    )
    return df.merge(semantic_df, on="tradehash", how="left")


def attach_network_features_from_db(df, db_path=DEFAULT_DB_PATH):
    """按 blocknumber 关联网络观测指标。"""
    if df.empty or "blocknumber" not in df.columns:
        return df
    conn = sqlite3.connect(db_path)
    try:
        network_df = pd.read_sql_query(
            """
            SELECT blocknumber, rpc_latency_ms, block_interval_ms, tx_count, fail_rate
            FROM network_metrics
            """,
            conn,
        )
    except Exception:
        conn.close()
        return df
    finally:
        try:
            conn.close()
        except Exception:
            pass
    if network_df.empty:
        return df
    network_df = (
        network_df.groupby("blocknumber", as_index=False)
        .agg(
            rpc_latency_ms=("rpc_latency_ms", "max"),
            block_interval_ms=("block_interval_ms", "max"),
            net_tx_count=("tx_count", "max"),
            net_fail_rate=("fail_rate", "max"),
        )
    )
    return df.merge(network_df, on="blocknumber", how="left")


# ================= 阶段 1：规则引擎 =================
def run_rule_detection(df, db_path=DEFAULT_DB_PATH):
    print("\n2. [Stage 1] Rule-based detection...")
    df["anomaly_type"] = "Normal"
    df["anomaly_score"] = 0.0
    valid_sealers = get_effective_valid_sealers(db_path=db_path)

    rule_anoms = []
    for idx, row in df.iterrows():
        reasons = []

        if row["feat_gas_used"] > THRESHOLDS["MAX_GAS"]:
            reasons.append("DoS_Exploit")

        if row["feat_input_len"] > THRESHOLDS["MAX_INPUT_LEN"]:
            reasons.append("Buffer_Overflow")

        # sealer 不在白名单中，视为可疑节点来源
        current_sealer = normalize_node_id(row["sealer"])
        is_valid_sealer = False
        for valid_id in valid_sealers:
            if valid_id in current_sealer or current_sealer in valid_id:
                is_valid_sealer = True
                break
        if not is_valid_sealer:
            reasons.append("Illegal_Node")

        if str(row["status"]).lower() in set(THRESHOLDS["FORBIDDEN_STATUS"]):
            reasons.append("Fuzzing_Fail")

        if row["feat_gas_ratio"] > THRESHOLDS["MAX_GAS_RATIO"]:
            reasons.append("Gas_OverLimit")

        if reasons:
            df.at[idx, "anomaly_type"] = "Rule: " + "+".join(reasons)
            df.at[idx, "anomaly_score"] = 1.0
            rule_anoms.append(idx)

    df = mark_detector_hits(df, rule_anoms, "rule_hit", "rule")
    print(f"   >> Rule stage found: {len(rule_anoms)}")
    return df, rule_anoms


# ================= 语义层：规则检测 =================
def run_semantic_rule_detection(df, detected_indices):
    print("\n2.1 [Semantic Rule] Contract semantic rules...")
    clean_indices = set(df.index.difference(detected_indices))
    semantic_indices = []
    for idx, row in df.iterrows():
        if idx not in clean_indices:
            continue
        reasons = []
        method = str(row.get("semantic_method_name", "Unknown"))
        decode_success = float(row.get("feat_semantic_decode_success", 0.0))
        sensitive = float(row.get("feat_semantic_sensitive", 0.0))
        status_bad = float(row.get("feat_status_code", 0.0)) > 0.0
        input_len = float(row.get("feat_input_len", 0.0))

        if sensitive > 0 and status_bad:
            reasons.append("SensitiveCallFailed")
        if decode_success <= 0 and input_len > 64:
            reasons.append("SemanticDecodeFailed")
        if method.lower().startswith("unknown") and input_len > THRESHOLDS["MAX_INPUT_LEN"] * 0.4:
            reasons.append("UnknownMethodLargePayload")

        if reasons:
            semantic_indices.append(idx)

    df = mark_detector_hits(df, semantic_indices, "semantic_rule_hit", "semantic_rule")
    df, detected_indices, count = mark_new_anomalies(
        df,
        semantic_indices,
        detected_indices,
        "SemanticRule: Contract_Semantic_Risk",
        0.88,
    )
    print(f"   >> Semantic rules found: {count}")
    return df, detected_indices


# ================= 语义层：异常检测 =================
def run_semantic_anomaly_detection(df, detected_indices):
    print("\n2.2 [Semantic Anomaly] Method burst checks...")
    if "semantic_method_name" not in df.columns or "blocknumber" not in df.columns:
        print("   (Skipped: missing semantic columns)")
        return df, detected_indices

    clean_indices = df.index.difference(detected_indices)
    subset = df.loc[clean_indices]
    if subset.empty:
        print("   (Skipped: no clean samples)")
        return df, detected_indices

    grp = (
        subset.groupby(["blocknumber", "semantic_method_name"])
        .size()
        .reset_index(name="method_count")
    )
    if grp.empty:
        print("   (Skipped: no semantic records)")
        return df, detected_indices

    z = robust_zscore(grp["method_count"])
    burst_pairs = grp.loc[z > THRESHOLDS["SEMANTIC_BURST_Z_MAX"], ["blocknumber", "semantic_method_name"]]
    if burst_pairs.empty:
        print("   >> Semantic anomaly candidates: 0")
        return df, detected_indices

    burst_set = set(zip(burst_pairs["blocknumber"], burst_pairs["semantic_method_name"]))
    sem_indices = [
        idx for idx, row in subset.iterrows()
        if (row["blocknumber"], row["semantic_method_name"]) in burst_set
    ]

    df = mark_detector_hits(df, sem_indices, "semantic_anomaly_hit", "semantic_anomaly")
    df, detected_indices, count = mark_new_anomalies(
        df,
        sem_indices,
        detected_indices,
        "SemanticML: Method_Burst_Anomaly",
        0.74,
    )
    print(f"   >> Semantic anomaly found: {count}")
    return df, detected_indices


# ================= 阶段 2：Isolation Forest =================
def run_iforest(df, detected_indices):
    print("\n3. [Stage 2] Isolation Forest...")
    clean_indices = df.index.difference(detected_indices)
    subset = df.loc[clean_indices]

    if len(subset) < THRESHOLDS["MIN_SAMPLE_ML"]:
        print("   (Skipped: not enough clean samples)")
        return df, detected_indices

    features = [
        "feat_gas_used",
        "feat_input_len",
        "feat_nonce",
        "feat_gas_ratio",
        "feat_status_code",
    ]
    X = subset[features].values

    clf = IsolationForest(
        contamination=THRESHOLDS["IFOREST_CONTAMINATION"],
        random_state=42,
        n_jobs=1,
    )
    preds = clf.fit_predict(X)
    if_indices = list(subset.index[preds == -1])
    df = mark_detector_hits(df, if_indices, "iforest_hit", "iforest")

    df, detected_indices, count = mark_new_anomalies(
        df,
        if_indices,
        detected_indices,
        "ML: Statistical_Outlier",
        0.8,
    )
    print(f"   >> IForest found: {count}")
    return df, detected_indices


# ================= 阶段 3：Autoencoder（MLP 重构） =================
def run_autoencoder(df, detected_indices):
    print("\n4. [Stage 3] Autoencoder (MLP reconstruction)...")
    clean_indices = df.index.difference(detected_indices)
    train_subset = df.loc[clean_indices]

    if len(train_subset) < 100:
        print("   (Hint: clean set is small, fallback to full dataset for AE training)")
        train_subset = df

    features = [
        "feat_gas_used",
        "feat_input_len",
        "feat_nonce",
        "feat_status_code",
        "feat_gas_ratio",
        "feat_block_trade_count",
    ]
    X_train = train_subset[features].values
    X_all = df[features].values

    scaler = MinMaxScaler()
    scaler.fit(X_train)
    X_train_scaled = scaler.transform(X_train)
    X_all_scaled = scaler.transform(X_all)

    input_dim = X_train.shape[1]
    encoding_dim = max(int(input_dim / 2), 2)
    ae = MLPRegressor(
        hidden_layer_sizes=(encoding_dim,),
        activation="relu",
        solver="adam",
        max_iter=500,
        random_state=42,
    )
    ae.fit(X_train_scaled, X_train_scaled)

    X_reconstructed = ae.predict(X_all_scaled)
    reconstruction_errors = np.mean(
        np.power(X_all_scaled - X_reconstructed, 2),
        axis=1,
    )

    # 阈值由“干净样本”的误差分布给出：均值 + 3*标准差
    clean_mask = df.index.isin(clean_indices)
    train_errors = reconstruction_errors[clean_mask]
    if len(train_errors) == 0:
        print("   (Skipped: no clean samples for AE threshold)")
        return df, detected_indices

    threshold = np.mean(train_errors) + 3 * np.std(train_errors)
    ae_indices = list(df.index[reconstruction_errors > threshold])
    df = mark_detector_hits(df, ae_indices, "autoencoder_hit", "autoencoder")

    df, detected_indices, count = mark_new_anomalies(
        df,
        ae_indices,
        detected_indices,
        "DL: Reconstruction_Anomaly",
        0.9,
    )
    print(f"   [AE] threshold={threshold:.6f}, found={count}")
    return df, detected_indices


# ================= 阶段 4：LOF（局部离群因子） =================
def run_lof(df, detected_indices):
    print("\n5. [Stage 4] Local Outlier Factor...")
    clean_indices = df.index.difference(detected_indices)
    subset = df.loc[clean_indices]

    if len(subset) < THRESHOLDS["MIN_SAMPLE_ML"]:
        print("   (Skipped: not enough clean samples)")
        return df, detected_indices

    features = [
        "feat_gas_used",
        "feat_input_len",
        "feat_nonce",
        "feat_gas_ratio",
        "feat_block_trade_count",
    ]
    X = subset[features].values
    n_neighbors = min(max(10, int(np.sqrt(len(subset)))), len(subset) - 1)
    if n_neighbors < 5:
        print("   (Skipped: not enough neighbors)")
        return df, detected_indices

    lof = LocalOutlierFactor(
        n_neighbors=n_neighbors,
        contamination=THRESHOLDS["LOF_CONTAMINATION"],
    )
    preds = lof.fit_predict(X)  # 1 normal, -1 anomaly
    lof_indices = list(subset.index[preds == -1])
    df = mark_detector_hits(df, lof_indices, "lof_hit", "lof")

    df, detected_indices, count = mark_new_anomalies(
        df,
        lof_indices,
        detected_indices,
        "ML: Local_Density_Outlier",
        0.75,
    )
    print(f"   >> LOF found: {count}")
    return df, detected_indices


# ================= 阶段 5：Elliptic Envelope（协方差异常） =================
def run_elliptic(df, detected_indices):
    print("\n6. [Stage 5] Elliptic Envelope...")
    clean_indices = df.index.difference(detected_indices)
    subset = df.loc[clean_indices]

    if len(subset) < THRESHOLDS["MIN_SAMPLE_ML"]:
        print("   (Skipped: not enough clean samples)")
        return df, detected_indices

    features = [
        "feat_gas_used",
        "feat_input_len",
        "feat_nonce",
        "feat_gas_ratio",
        "feat_block_trade_count",
    ]
    # 仅保留方差非零特征，避免协方差矩阵奇异
    valid_features = [f for f in features if subset[f].std(ddof=0) > 1e-9]
    if len(valid_features) < 3:
        print("   (Skipped: insufficient independent features)")
        return df, detected_indices

    X = subset[valid_features].values

    scaler = MinMaxScaler()
    X_scaled = scaler.fit_transform(X)

    try:
        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            model = EllipticEnvelope(
                contamination=THRESHOLDS["ELLIPTIC_CONTAMINATION"],
                random_state=42,
                support_fraction=0.9,
            )
            preds = model.fit_predict(X_scaled)  # 1 normal, -1 anomaly
    except Exception as e:
        print(f"   (Skipped: elliptic failed: {e})")
        return df, detected_indices

    cov_indices = list(subset.index[preds == -1])
    df = mark_detector_hits(df, cov_indices, "elliptic_hit", "elliptic")

    df, detected_indices, count = mark_new_anomalies(
        df,
        cov_indices,
        detected_indices,
        "Stat: Covariance_Outlier",
        0.66,
    )
    print(f"   >> Elliptic found: {count}")
    return df, detected_indices


# ================= 阶段 6：稳健统计极值（MAD） =================
def run_robust_stats(df, detected_indices):
    print("\n7. [Stage 6] Robust statistics (MAD-based)...")
    clean_indices = df.index.difference(detected_indices)
    subset = df.loc[clean_indices]
    if subset.empty:
        print("   (Skipped: no clean samples)")
        return df, detected_indices

    check_features = [
        "feat_gas_used",
        "feat_input_len",
        "feat_gas_ratio",
        "feat_block_trade_count",
    ]

    extreme_mask = pd.Series(False, index=subset.index)
    for col in check_features:
        z = robust_zscore(subset[col])
        extreme_mask = extreme_mask | (z.abs() > THRESHOLDS["ROBUST_Z_MAX"])

    robust_indices = list(subset.index[extreme_mask])
    df = mark_detector_hits(df, robust_indices, "robust_hit", "robust")
    # 该阶段只提供软证据，不单独判定硬异常
    print(f"   >> Robust-stat candidates: {len(robust_indices)}")
    return df, detected_indices


# ================= 阶段 7：Nonce 序列行为检测 =================
def run_nonce_behavior(df, detected_indices):
    print("\n8. [Stage 7] Nonce behavior checks...")
    if "from" not in df.columns:
        print("   (Skipped: missing `from` column)")
        return df, detected_indices

    clean_indices = set(df.index.difference(detected_indices))
    nonce_anom = []

    # 按发送方分组分析 nonce 序列跳变/回退
    grouped = df.groupby("from")
    for _, grp in grouped:
        if len(grp) < 8:
            continue
        sorted_grp = grp.sort_values(["feat_ts", "feat_nonce"])
        nonce_diff = sorted_grp["feat_nonce"].diff()

        pos_diff = nonce_diff[nonce_diff > 0].dropna()
        if len(pos_diff) < 5:
            continue

        q1 = float(np.quantile(pos_diff, 0.25))
        q3 = float(np.quantile(pos_diff, 0.75))
        iqr = max(q3 - q1, 1.0)
        jump_threshold = q3 + 8.0 * iqr

        bad = (nonce_diff < 0) | (nonce_diff > jump_threshold)
        bad = bad.fillna(False)
        bad_count = int(bad.sum())
        bad_ratio = bad_count / max(len(sorted_grp), 1)

        # 仅在异常模式足够明显时才标记，降低误报
        if bad_count >= 5 and bad_ratio >= 0.30:
            bad_grp = sorted_grp.loc[bad]
            bad_diff = nonce_diff.loc[bad]
            # 每个发送方只保留最极端的少量样本，进一步抑制误报
            severity = bad_diff.apply(lambda d: abs(d) if d < 0 else max(d - jump_threshold, 0))
            topk = min(3, len(severity))
            chosen_idx = list(severity.sort_values(ascending=False).head(topk).index)
            nonce_anom.extend([i for i in chosen_idx if i in clean_indices])

    df = mark_detector_hits(df, nonce_anom, "nonce_hit", "nonce")
    # Nonce 模式单独出现时作为软证据，交给融合阶段统一判断
    print(f"   >> Nonce-behavior candidates: {len(set(nonce_anom))}")
    return df, detected_indices


# ================= 阶段 8：区块突发/失败风暴检测 =================
def run_block_behavior(df, detected_indices):
    print("\n9. [Stage 8] Block-level behavior checks...")
    if "blocknumber" not in df.columns:
        print("   (Skipped: missing `blocknumber` column)")
        return df, detected_indices

    clean_indices = df.index.difference(detected_indices)
    subset = df.loc[clean_indices]
    if subset.empty:
        print("   (Skipped: no clean samples)")
        return df, detected_indices

    block_agg = subset.groupby("blocknumber").agg(
        tx_count=("blocknumber", "size"),
        fail_rate=("feat_status_code", "mean"),
    )

    tx_z = robust_zscore(block_agg["tx_count"])
    burst_blocks = block_agg.index[tx_z > THRESHOLDS["BLOCK_TX_Z_MAX"]]
    fail_blocks = block_agg.index[block_agg["fail_rate"] > THRESHOLDS["BLOCK_FAIL_RATE_MAX"]]
    anomaly_blocks = set(burst_blocks).union(set(fail_blocks))

    if not anomaly_blocks:
        print("   >> Block-behavior candidates: 0")
        return df, detected_indices

    block_indices = list(subset.index[subset["blocknumber"].isin(anomaly_blocks)])
    df = mark_detector_hits(df, block_indices, "block_hit", "block")
    # 区块级异常作为软证据，与其他检测器共同融合
    print(f"   >> Block-behavior candidates: {len(block_indices)}")
    return df, detected_indices


def run_network_detection(df, detected_indices):
    print("\n10. [Stage 9] Network observation checks...")
    clean_indices = df.index.difference(detected_indices)
    subset = df.loc[clean_indices]
    if subset.empty:
        print("   (Skipped: no clean samples)")
        return df, detected_indices

    latency = subset["feat_rpc_latency"] if "feat_rpc_latency" in subset.columns else pd.Series([], dtype=float)
    interval = subset["feat_block_interval"] if "feat_block_interval" in subset.columns else pd.Series([], dtype=float)
    fail_rate = subset["feat_network_fail_rate"] if "feat_network_fail_rate" in subset.columns else pd.Series([], dtype=float)
    if latency.empty and interval.empty and fail_rate.empty:
        print("   (Skipped: no network features)")
        return df, detected_indices

    latency_med = float(np.median(latency)) if len(latency) else 0.0
    interval_med = float(np.median(interval)) if len(interval) else 0.0
    latency_th = max(120.0, latency_med * THRESHOLDS["NETWORK_LATENCY_MULTIPLIER"])
    interval_th = max(5000.0, interval_med * THRESHOLDS["NETWORK_BLOCK_INTERVAL_MULTIPLIER"])

    network_mask = (
        (subset["feat_rpc_latency"] > latency_th)
        | (subset["feat_block_interval"] > interval_th)
        | (subset["feat_network_fail_rate"] > THRESHOLDS["BLOCK_FAIL_RATE_MAX"])
    )
    network_indices = list(subset.index[network_mask])
    if not network_indices:
        print("   >> Network candidates: 0")
        return df, detected_indices

    df = mark_detector_hits(df, network_indices, "network_hit", "network")
    df, detected_indices, count = mark_new_anomalies(
        df,
        network_indices,
        detected_indices,
        "Network: Node_Stress_Anomaly",
        0.72,
    )
    print(f"   >> Network candidates: {count}")
    return df, detected_indices


def run_detection(df, db_path=DEFAULT_DB_PATH):
    """执行完整多阶段检测流程并返回结果 DataFrame。"""
    if df is None or df.empty:
        return pd.DataFrame()

    df = df.copy()
    df = init_tracking_columns(df)
    df, detected = run_rule_detection(df, db_path=db_path)
    df, detected = run_semantic_rule_detection(df, detected)
    df, detected = run_semantic_anomaly_detection(df, detected)
    df, detected = run_iforest(df, detected)
    df, detected = run_autoencoder(df, detected)
    df, detected = run_lof(df, detected)
    df, detected = run_elliptic(df, detected)
    df, detected = run_robust_stats(df, detected)
    df, detected = run_nonce_behavior(df, detected)
    df, detected = run_block_behavior(df, detected)
    df, detected = run_network_detection(df, detected)
    df = finalize_ensemble(df)
    return df


def summarize_detection(df):
    total = len(df)
    normal = int((df["anomaly_type"] == "Normal").sum()) if total > 0 else 0
    anomalies = total - normal
    risk_levels = df["anomaly_level"].value_counts().to_dict() if total > 0 else {}
    anomaly_types = df["anomaly_type"].value_counts().head(10).to_dict() if total > 0 else {}
    return {
        "total": total,
        "anomalies": anomalies,
        "anomaly_rate": (anomalies / max(total, 1)),
        "risk_levels": risk_levels,
        "top_anomaly_types": anomaly_types,
    }


def detect_csv_file(input_file=INPUT_FILE, output_file=None):
    df = load_data(input_file)
    df = run_detection(df, db_path=DEFAULT_DB_PATH)
    if output_file:
        df.to_csv(output_file, index=False)
    return df


def detect_db_data(db_path=DEFAULT_DB_PATH, output_file=None, limit=None):
    global RUNTIME_VALID_SEALERS
    RUNTIME_VALID_SEALERS = None
    df = load_data_from_db(db_path=db_path, limit=limit)
    if df.empty:
        return df
    df = run_detection(df, db_path=db_path)
    if output_file:
        df.to_csv(output_file, index=False)
    return df


# ================= 主流程入口 =================
if __name__ == "__main__":
    try:
        df = detect_csv_file(INPUT_FILE)
    except FileNotFoundError:
        print("File not found. Please generate or provide input data first.")
        raise SystemExit(1)
    except Exception as e:
        print(f"Failed to load data: {e}")
        raise SystemExit(1)

    # 结果汇总与落盘
    print("\n" + "=" * 48)
    print("Final Detection Report")
    print("=" * 48)
    total = len(df)
    normal = int((df["anomaly_type"] == "Normal").sum())
    anomalies = total - normal
    print(f"Total samples: {total}")
    print(f"Anomalies: {anomalies} (Rate: {anomalies / max(total, 1):.2%})")
    print("-" * 32)
    print("Contributions by detector:")
    print(df["anomaly_type"].apply(lambda x: x.split(":")[0]).value_counts())
    print("-" * 32)
    print("Risk levels:")
    print(df["anomaly_level"].value_counts())
    fusion_hits = int((df["anomaly_type"] == "Fusion: MultiSignal_Anomaly").sum())
    print(f"Fusion promoted anomalies: {fusion_hits}")

    df.to_csv(OUTPUT_FILE, index=False)
    print(f"\nSaved result: {OUTPUT_FILE}")
    print("Included columns: anomaly_type, anomaly_score, anomaly_level, ensemble_score, ensemble_votes")
