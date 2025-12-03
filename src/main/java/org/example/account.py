import pandas as pd
from sklearn.ensemble import IsolationForest

# --- 配置区域 ---
INPUT_FILE = 'blockchain_data_output.csv' # 请确保这是您包含数据的CSV文件名
# 用于检测的数值特征
# 之前这里是['gas消耗', 'gaslimit', 'gasprice']，现在我们先处理这些列
FEATURE_COLUMNS = ['gasused', 'gaslimit', 'gasprice']
# 预估数据中异常点的比例
CONTAMINATION_RATE = 0.01 
# --- 配置结束 ---

def detect_transaction_outliers():
    print(f"--- 算法三：交易安全 - 交易参数异常检测 ---")
    try:
        df = pd.read_csv(INPUT_FILE)
    except FileNotFoundError:
        print(f"错误: 输入文件 {INPUT_FILE} 未找到。")
        return

    # ======================== 关键修改点开始 ========================
    print("正在将十六进制字符串列转换为十进制数字...")
    
    # 定义哪些列可能包含十六进制字符串
    hex_columns_to_convert = ['gaslimit', 'gasprice']
    
    for col in hex_columns_to_convert:
        if col in df.columns:
            # .apply() 方法会对列中的每一个元素执行一个函数
            # lambda x: int(str(x), 16) ... 的意思是：
            #   - str(x) 确保x是字符串
            #   - x.startswith('0x') 检查是否是十六进制
            #   - int(x, 16) 将16进制字符串x，转换为10进制整数
            df[col] = df[col].apply(lambda x: int(str(x), 16) if isinstance(x, str) and x.startswith('0x') else x)
            print(f" - 列 '{col}' 转换完成。")
        else:
            print(f" - 警告：数据中未找到列 '{col}'。")

    # 将所有特征列转换为数值类型，无法转换的将变成NaN（空值）
    df[FEATURE_COLUMNS] = df[FEATURE_COLUMNS].apply(pd.to_numeric, errors='coerce')
    
    print("十六进制转换完成！")
    # ======================== 关键修改点结束 ========================

    features = df[FEATURE_COLUMNS].dropna()
    if features.empty:
        print("错误：选择的特征列中没有有效的数值数据。")
        return

    print(f"使用 {FEATURE_COLUMNS} 特征进行异常交易检测...")
    model = IsolationForest(contamination=CONTAMINATION_RATE, random_state=42)
    predictions = model.fit_predict(features)
    
    features['Anomaly'] = predictions
    
    anomalies = features[features['Anomaly'] == -1]

    if not anomalies.empty:
        print(f"\n检测到 {len(anomalies)} 筆参数异常的交易:")
        print(df.loc[anomalies.index])
    else:
        print("\n未检测到参数异常的交易。")

if __name__ == "__main__":
    detect_transaction_outliers()