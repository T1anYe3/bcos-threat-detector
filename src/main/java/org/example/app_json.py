from flask import Flask, jsonify, request, send_file
from flask_cors import CORS
import sqlite3
import pandas as pd
from datetime import datetime
from pathlib import Path
import subprocess
import os

from datadetecter import (
    detect_db_data,
    prepare_features,
    run_detection,
    summarize_detection,
)

BASE_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = (BASE_DIR / "../../../../../").resolve()
DB_PATH = str((BASE_DIR / "../../../../../" / "blockchain_data.db").resolve())
WHITELIST_SYNC_STATE = {"done": False, "last_message": "not started"}

app = Flask(__name__, static_folder="static", static_url_path="/static")
CORS(app)


def sync_chain_whitelist():
    """
    启动前主动同步一次链上共识节点白名单。
    通过 Maven 直接运行 NodeInfoCollector，避免依赖 RealTimeMonitor。
    """
    cmd = [
        "mvn",
        "-q",
        "-DskipTests",
        "-Dexec.mainClass=org.example.NodeInfoCollector",
        "org.codehaus.mojo:exec-maven-plugin:3.1.0:java",
    ]
    try:
        result = subprocess.run(
            cmd,
            cwd=str(PROJECT_ROOT),
            capture_output=True,
            text=True,
            timeout=90,
            check=False,
        )
        if result.returncode == 0:
            WHITELIST_SYNC_STATE["done"] = True
            WHITELIST_SYNC_STATE["last_message"] = "ok"
            print("[Whitelist] 已完成链上白名单同步。")
            return True

        err = (result.stderr or result.stdout or "").strip()
        WHITELIST_SYNC_STATE["done"] = False
        WHITELIST_SYNC_STATE["last_message"] = err[:300] if err else "command failed"
        print("[Whitelist] 同步失败，将使用数据库现有名单或静态回退。")
        return False
    except Exception as e:
        WHITELIST_SYNC_STATE["done"] = False
        WHITELIST_SYNC_STATE["last_message"] = str(e)
        print(f"[Whitelist] 同步异常: {e}")
        return False


def _short_hash(text, size=12):
    if text is None:
        return "-"
    s = str(text)
    return s if len(s) <= size else s[:size] + "..."


def _risk_from_score(score):
    if score >= 0.9:
        return "CRITICAL"
    if score >= 0.75:
        return "HIGH"
    if score >= 0.55:
        return "MEDIUM"
    return "INFO"


def _build_realtime_rows(limit=20):
    if not Path(DB_PATH).exists():
        return []

    sql = """
    SELECT tradehash, from_addr, to_addr, status, timestamp, blocknumber
    FROM transactions
    ORDER BY id DESC
    LIMIT ?
    """
    conn = sqlite3.connect(DB_PATH)
    try:
        df = pd.read_sql_query(sql, conn, params=(int(limit),))
    finally:
        conn.close()

    if df.empty:
        return []

    rows = []
    for _, row in df.iterrows():
        ts_val = row.get("timestamp", 0)
        try:
            # FISCO 常见为毫秒时间戳
            ts_ms = int(float(ts_val))
            time_text = datetime.fromtimestamp(ts_ms / 1000.0).strftime("%H:%M:%S")
        except Exception:
            time_text = str(ts_val)
        rows.append(
            {
                "time": time_text,
                "hash": str(row.get("tradehash", "")),
                "hash_short": _short_hash(row.get("tradehash", "")),
                "from": _short_hash(row.get("from_addr", ""), 18),
                "status": str(row.get("status", "")),
                "blocknumber": int(row.get("blocknumber", 0)),
            }
        )
    return rows


def _build_payload(df_result, mode):
    if df_result is None or df_result.empty:
        return {
            "mode": mode,
            "network_status": "NO_DATA",
            "security_score": 100.0,
            "summary": summarize_detection(pd.DataFrame(columns=["anomaly_type", "anomaly_level"])),
            "transactions": [],
            "sealers": [],
            "trend": {"x": [], "tx_count": [], "avg_gas": []},
            "threat_distribution": [],
            "threat_logs": [],
            "risk_levels": {},
        }

    summary = summarize_detection(df_result)
    security_score = max(0.0, round(100.0 * (1.0 - summary["anomaly_rate"]), 2))

    # 趋势图：按区块聚合
    trend_df = (
        df_result.groupby("blocknumber", as_index=False)
        .agg(tx_count=("blocknumber", "size"), avg_gas=("feat_gas_used", "mean"))
        .sort_values("blocknumber")
        .tail(40)
    )

    # 右侧安全分析（玫瑰图数据）
    threat_df = (
        df_result[df_result["anomaly_type"] != "Normal"]["anomaly_type"]
        .value_counts()
        .head(8)
        .reset_index()
    )
    threat_df.columns = ["name", "value"]

    # 实时日志/历史日志
    anoms = df_result[df_result["anomaly_type"] != "Normal"].copy()
    if "tradehash" in anoms.columns:
        anoms = anoms.sort_values("tradehash", ascending=False)
    logs = []
    for _, row in anoms.tail(50).iterrows():
        score = float(row.get("anomaly_score", 0.0))
        logs.append(
            {
                "time": datetime.now().strftime("%H:%M:%S"),
                "level": _risk_from_score(score),
                "type": str(row.get("anomaly_type", "Unknown")),
                "hash": str(row.get("tradehash", "")),
                "hash_short": _short_hash(row.get("tradehash", ""), 16),
                "message": f"检测到疑似异常交易，评分={score:.2f}",
            }
        )

    sealers = sorted([str(s) for s in df_result["sealer"].dropna().unique().tolist()])[:12]
    return {
        "mode": mode,
        "network_status": "CONNECTED",
        "security_score": security_score,
        "summary": summary,
        "transactions": _build_realtime_rows(20),
        "sealers": sealers,
        "trend": {
            "x": trend_df["blocknumber"].astype(str).tolist(),
            "tx_count": trend_df["tx_count"].astype(int).tolist(),
            "avg_gas": trend_df["avg_gas"].round(2).tolist(),
        },
        "threat_distribution": threat_df.to_dict(orient="records"),
        "threat_logs": logs,
        "risk_levels": summary["risk_levels"],
    }


@app.route("/")
def index():
    return send_file(str(BASE_DIR / "dashboard_json.html"))


@app.route("/api/realtime", methods=["GET"])
def api_realtime():
    try:
        if not WHITELIST_SYNC_STATE["done"]:
            sync_chain_whitelist()
        limit = int(request.args.get("limit", 500))
        df_result = detect_db_data(db_path=DB_PATH, limit=limit)
        payload = _build_payload(df_result, mode="REALTIME_DB")
        payload["whitelist_sync"] = WHITELIST_SYNC_STATE
        return jsonify({"status": "success", "data": payload})
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500


@app.route("/api/import_csv", methods=["POST"])
def api_import_csv():
    if "file" not in request.files:
        return jsonify({"status": "error", "message": "未找到上传文件"}), 400

    file = request.files["file"]
    if not file or file.filename == "":
        return jsonify({"status": "error", "message": "请选择 CSV 文件"}), 400

    try:
        raw_df = pd.read_csv(file)
        feature_df = prepare_features(raw_df)
        result_df = run_detection(feature_df)
        payload = _build_payload(result_df, mode="CSV_HISTORY")
        payload["csv_meta"] = {
            "filename": file.filename,
            "rows": int(len(raw_df)),
            "columns": list(raw_df.columns),
        }
        return jsonify({"status": "success", "data": payload})
    except Exception as e:
        return jsonify({"status": "error", "message": f"CSV 分析失败: {e}"}), 500


@app.route("/api/refresh_whitelist", methods=["POST"])
def api_refresh_whitelist():
    ok = sync_chain_whitelist()
    code = 200 if ok else 500
    return jsonify({"status": "success" if ok else "error", "data": WHITELIST_SYNC_STATE}), code


if __name__ == "__main__":
    # debug reloader 会启动两次进程，仅在主进程启动时执行同步
    should_sync = (not app.debug) or (os.environ.get("WERKZEUG_RUN_MAIN") == "true")
    if should_sync:
        sync_chain_whitelist()
    print("启动统一安全监控后端: http://127.0.0.1:5000")
    print("实时模式接口: GET /api/realtime")
    print("CSV 导入接口: POST /api/import_csv")
    print("白名单手动刷新: POST /api/refresh_whitelist")
    app.run(debug=True, host="0.0.0.0", port=5000)