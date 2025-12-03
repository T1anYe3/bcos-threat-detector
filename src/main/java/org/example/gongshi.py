import pandas as pd

# --- 配置 ---
INPUT_FILE = 'blockchain_data_output.csv'
# 定义异常阈值：超过 平均值 + N倍标准差 的出块间隔被视为异常
# N值可以根据您的链的出块稳定性进行调整，3是一个常用的值
STD_MULTIPLIER = 3.0 
# ---

def detect_block_interval_anomalies():
    print("--- 算法一：共识活性 - 出块间隔异常检测 ---")
    try:
        df = pd.read_csv(INPUT_FILE)
    except FileNotFoundError:
        print(f"错误: 输入文件 {INPUT_FILE} 未找到。")
        return

    # 确保数据按区块号排序
    df = df.sort_values(by='blocknumber').reset_index(drop=True)

    # 将时间戳从毫秒转为日期时间对象
    df['TimestampDT'] = pd.to_datetime(df['timestamp'], unit='ms')
    
    # 计算出块间隔（秒）
    # 我们只在区块号变化时计算，以处理一个区块有多笔交易的情况
    block_intervals = df.groupby('blocknumber')['TimestampDT'].first().diff().dt.total_seconds().dropna()

    if block_intervals.empty:
        print("数据不足，无法计算出块间隔。")
        return

    # 计算正常间隔的统计数据
    mean_interval = block_intervals.mean()
    std_interval = block_intervals.std()
    threshold = mean_interval + STD_MULTIPLIER * std_interval

    print(f"平均出块间隔: {mean_interval:.2f} 秒")
    print(f"异常阈值 (超过 {STD_MULTIPLIER} 倍标准差): > {threshold:.2f} 秒")

    # 找出所有出块间隔超过阈值的区块
    anomalous_block_numbers = block_intervals[block_intervals > threshold].index
    
    if not anomalous_block_numbers.empty:
        print(f"\n检测到 {len(anomalous_block_numbers)} 个出块间隔异常的区块:")
        # 从原始DataFrame中获取这些异常区块的信息
        anomalous_blocks = df[df['blocknumber'].isin(anomalous_block_numbers)]
        print(anomalous_blocks[['blocknumber', 'timestamp']].drop_duplicates())
    else:
        print("\n未检测到出块间隔异常。")

if __name__ == "__main__":
    detect_block_interval_anomalies()