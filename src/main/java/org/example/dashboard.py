import streamlit as st
import pandas as pd
import plotly.express as px  # <-- 【修正】确保导入了 plotly.express 并命名为 px
import numpy as np           # <-- 【修正】确保导入了 numpy 并命名为 np

# ======================= 已根据您的新文件名修改 =======================
from func_account import detect_high_frequency_accounts
# ===================================================================

# --- 页面基础配置 ---
st.set_page_config(
    page_title="联盟链威胁情报仪表盘",
    page_icon="🛡️",
    layout="wide"
)

# --- 标题 ---
st.title("🛡️ 联盟链威胁情报仪表盘")

# --- 加载数据 ---
@st.cache_data
def load_data(file_path):
    try:
        df = pd.read_csv(file_path)
        if '时间戳' in df.columns:
            df['TimestampDT'] = pd.to_datetime(df['时间戳'], unit='ms')
        
        # 为了演示，模拟Anomaly和ThreatType列，实际应由检测脚本生成
        if 'Anomaly' not in df.columns:
            df['Anomaly'] = 1 # 默认为正常
        if 'BlockInterval' not in df.columns:
             df['BlockInterval'] = 0 # 模拟数据
        if 'gas消耗' not in df.columns:
            df['gas消耗'] = 0 # 模拟数据

        conditions = [
            (df['Anomaly'] == -1) & (df['BlockInterval'] > 10),
            (df['Anomaly'] == -1) 
        ]
        choices = ['共识层威胁', '交易层威胁']
        df['ThreatType'] = np.select(conditions, choices, default='正常')

        return df
    except FileNotFoundError:
        st.error(f"错误: 数据文件 '{file_path}' 未找到。")
        return None

DATA_FILE = 'blockchain_data_output.csv' # 请确保这是您的主数据文件名
data_df = load_data(DATA_FILE)

if data_df is None:
    st.stop()

# 筛选出异常数据
anomalies_df = data_df[data_df['Anomaly'] == -1].copy()

# --- 威胁总览 ---
st.header("威胁总览")
col1, col2, col3 = st.columns(3)
with col1:
    st.metric(label="总交易数", value=f"{len(data_df):,}")
with col2:
    st.metric(label="检测到的异常数", value=f"{len(anomalies_df):,}")
with col3:
    st.metric(label="最活跃的发送方 (From)", value=data_df['from'].mode()[0][:10] + "...")

st.markdown("---") 

# --- 可视化图表 ---
col_chart1, col_chart2 = st.columns(2)
with col_chart1:
    st.subheader("威胁类型分布")
    threat_counts = anomalies_df['ThreatType'].value_counts().reset_index()
    if not threat_counts.empty:
        fig_pie = px.pie(threat_counts, names='index', values='ThreatType', hole=.4, title="检测到的威胁类型分布")
        st.plotly_chart(fig_pie, use_container_width=True)
    else:
        st.info("暂无异常数据用于生成威胁类型分布图。")

with col_chart2:
    st.subheader("威胁数量趋势 (按天)")
    if not anomalies_df.empty:
        anomalies_df.loc[:, 'Date'] = anomalies_df['TimestampDT'].dt.date
        daily_anomalies = anomalies_df.groupby('Date').size().reset_index(name='count')
        fig_bar = px.bar(daily_anomalies, x='Date', y='count', title="每日检测到的威胁数量")
        st.plotly_chart(fig_bar, use_container_width=True)
    else:
        st.info("暂无异常数据用于生成威胁趋势图。")

# --- 高频账户检测 ---
st.markdown("---")
st.header("高频交易账户检测 (异常账户)")

# 直接调用从 func_account.py 导入的函数
suspicious_accounts_df = detect_high_frequency_accounts(data_df, top_n=10)
st.write("以下是发送交易次数最多的账户，可能是潜在的交易刷量或DoS攻击者。")
st.dataframe(suspicious_accounts_df)

# --- 威胁列表 ---
st.markdown("---")
st.header("威胁列表（所有检测到的异常交易）")
st.dataframe(anomalies_df)