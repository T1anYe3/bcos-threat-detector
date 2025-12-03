import pandas as pd

# --- 配置 ---
INPUT_FILE = 'blockchain_data_output.csv'
# 显示发送交易数量最多的前N个账户
TOP_N_ACCOUNTS = 10
# ---

def detect_spamming_accounts():
    print(f"--- 算法二：网络安全 - 交易刷量账户识别 ---")
    try:
        df = pd.read_csv(INPUT_FILE)
    except FileNotFoundError:
        print(f"错误: 输入文件 {INPUT_FILE} 未找到。")
        return

    print(f"在 {len(df)} 笔交易中分析账户频率...")
    
    # 计算每个 'from' 地址出现的次数
    account_counts = df['from'].value_counts()
    
    if account_counts.empty:
        print("数据中没有交易发起方信息。")
        return

    print(f"\n发送交易数量最多的前 {TOP_N_ACCOUNTS} 个账户:")
    print(account_counts.head(TOP_N_ACCOUNTS))

if __name__ == "__main__":
    detect_spamming_accounts()