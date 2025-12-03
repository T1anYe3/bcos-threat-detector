import pandas as pd

# --- 配置区域 ---
# 输入文件名
INPUT_FILE = 'blockchain_data_output.csv' 
# 时间窗口大小，'1Min'代表1分钟, '10S'代表10秒, '1H'代表1小时
TIME_WINDOW = '1Min'
# 异常阈值的标准差倍数
STD_MULTIPLIER = 3.0
# --- 配置结束 ---

def detect_dos_by_rate():
    print(f"--- 算法四：网络安全 - 交易速率异常检测 ---")
    try:
        df = pd.read_csv(INPUT_FILE)
    except FileNotFoundError:
        print(f"错误: 输入文件 {INPUT_FILE} 未找到。")
        return

    if 'timestamp' not in df.columns:
        print("错误: 数据中未找到 'timestamp' 列。")
        return

    # 1. 将时间戳列转换为日期时间对象，并设为索引
    df['TimestampDT'] = pd.to_datetime(df['timestamp'], unit='ms')
    df = df.set_index('TimestampDT')

    # 2. 按指定的时间窗口进行重采样(resample)，并计算每个窗口的交易数
    # 我们使用 '交易哈希' 列来进行计数
    transaction_rate = df['tradehash'].resample(TIME_WINDOW).count()
    
    if transaction_rate.empty:
        print("数据不足，无法计算交易速率。")
        return

    # 3. 计算正常速率的统计数据
    mean_rate = transaction_rate.mean()
    std_rate = transaction_rate.std()
    # 定义异常阈值
    threshold = mean_rate + STD_MULTIPLIER * std_rate

    print(f"\n分析时间窗口: {TIME_WINDOW}")
    print(f"平均交易速率: {mean_rate:.2f} 笔 / {TIME_WINDOW}")
    print(f"异常阈值 (超过 {STD_MULTIPLIER} 倍标准差): > {threshold:.2f} 笔 / {TIME_WINDOW}")

    # 4. 找出所有交易速率超过阈值的时间窗口
    anomalous_periods = transaction_rate[transaction_rate > threshold]
    
    if not anomalous_periods.empty:
        print(f"\n检测到 {len(anomalous_periods)} 个交易速率异常的时间段:")
        print(anomalous_periods)
    else:
        print("\n未检测到交易速率异常的时间段。")

if __name__ == "__main__":
    detect_dos_by_rate()