import pandas as pd

def detect_high_frequency_accounts(df, top_n=10):
    """
    分析输入的DataFrame，找出发送交易频率最高的Top-N个账户。

    Args:
        df (pd.DataFrame): 包含交易数据的DataFrame，必须含有 'from' 列。
        top_n (int, optional): 需要返回的排名最高的账户数量。默认为 10。

    Returns:
        pd.DataFrame: 一个新的DataFrame，包含两列：'账户地址 (Account)' 和 '交易次数 (TransactionCount)'。
                      如果输入数据无效，则返回一个空的DataFrame。
    """
    if df is None or 'from' not in df.columns:
        print("错误：输入数据无效或缺少 'from' 列。")
        return pd.DataFrame(columns=['账户地址 (Account)', '交易次数 (TransactionCount)'])

    # 计算每个 'from' 地址出现的次数
    account_counts = df['from'].value_counts()
    
    # 将结果转换为DataFrame并重命名列
    top_accounts_df = account_counts.head(top_n).reset_index()
    top_accounts_df.columns = ['账户地址 (Account)', '交易次数 (TransactionCount)']
    
    return top_accounts_df

# --- 以下代码仅在直接运行 account.py 时才会执行，用于独立测试 ---
if __name__ == "__main__":
    print("--- 正在独立运行 account.py进行测试 ---")
    try:
        # 尝试加载一个示例文件进行测试
        test_df = pd.read_csv('blockchain_data_output.csv')
        print("加载测试数据成功。")
        
        # 调用检测函数
        detected_accounts = detect_high_frequency_accounts(test_df)
        
        print("\n检测到的高频账户:")
        print(detected_accounts)

    except FileNotFoundError:
        print("错误：未找到测试文件 'blockchain_data_output.csv'。")
    except Exception as e:
        print(f"测试时发生错误: {e}")