import pandas as pd

# --- 配置区域 ---
# 请将 'your_data.tsv' 替换为您实际的TSV文件名
TSV_FILE_PATH = 'blockchain_data_custom.tsv'


# 输出文件名（您希望生成的CSV文件名）
OUTPUT_CSV_FILE = 'blockchain_data_output.csv'


# --- 配置结束 ---

def convert_tsv_to_csv(input_path, output_path):
    """
    读取一个TSV文件，并将其内容保存为一个CSV文件。

    Args:
        input_path (str): 输入的TSV文件路径。
        output_path (str): 输出的CSV文件路径。
    """
    try:
        # 1. 使用pandas读取TSV文件
        # sep='\t' 表示这是一个使用制表符分隔的文件
        df = pd.read_csv(input_path, sep='\t', header=0)

        print(f"成功从 '{input_path}' 文件中加载数据！")

        # 打印一些信息以确认数据已加载
        print(f"共加载了 {len(df)} 行数据。")
        print("数据预览（前3行）:")
        print(df.head(3))

        # 2. 将DataFrame写入CSV文件
        # index=False 表示不将DataFrame的行索引（0, 1, 2...）写入文件
        df.to_csv(output_path, index=False)

        print(f"\n数据已成功转换并保存到 '{output_path}' 文件中！")

    except FileNotFoundError:
        print(f"错误：输入文件未找到，请确认 '{input_path}' 文件与此脚本在同一目录下。")
    except Exception as e:
        print(f"处理文件时发生错误: {e}")


# 主程序入口
if __name__ == "__main__":
    convert_tsv_to_csv(TSV_FILE_PATH, OUTPUT_CSV_FILE)