package org.example;

import java.math.BigInteger;

public class ManualInputDecoder {

    public static void main(String[] args) {
        // 您提供的 input 字符串
        String input = "";
        System.out.println("原始 Input: " + input);
        System.out.println("--------------------------------------------------");

        // 执行解码
        decode(input);
    }

    /**
     * 手动解码核心逻辑
     */
    public static void decode(String input) {
        if (input == null || !input.startsWith("0x")) {
            System.out.println("格式错误：必须以 0x 开头");
            return;
        }

        // 1. 提取 Method ID (前10个字符: 0x + 8位Hex)
        // 0x4ed3885e -> set(string)
        String methodId = input.substring(0, 10);
        System.out.println("[1] 函数签名 Hash: " + methodId);

        // 简单的硬编码匹配（模拟查表）
        if ("0x4ed3885e".equals(methodId)) {
            System.out.println("    -> 识别为函数: set(string)");

            // 2. 解析后续的参数部分 (去除前10位)
            String params = input.substring(10);

            // 对于 string 类型的参数，ABI 编码规则如下：
            // 第 1 部分 (64字符): 数据偏移量 (Offset) -> 通常是 00...20 (表示32字节后开始)
            // 第 2 部分 (64字符): 字符串长度 (Length)
            // 第 3 部分 (N字符):  字符串真正的 Hex 内容

            // 跳过第 1 部分 (偏移量)，直接看第 2 部分 (长度)
            // 偏移量通常占 64 个字符
            if (params.length() < 128) {
                System.out.println("数据过短，无法解析");
                return;
            }

            // 获取长度部分的 Hex (索引 64 到 128)
            String lengthHex = params.substring(64, 128);
            BigInteger lengthBI = new BigInteger(lengthHex, 16);
            int length = lengthBI.intValue();
            System.out.println("[2] 字符串长度: " + length);

            // 获取内容部分的 Hex (索引 128 开始，截取 length * 2 个字符)
            int dataStartIndex = 128;
            int dataEndIndex = dataStartIndex + (length * 2);

            if (params.length() >= dataEndIndex) {
                String contentHex = params.substring(dataStartIndex, dataEndIndex);
                System.out.println("[3] 内容 Hex: " + contentHex);

                // 将 Hex 转为可读 ASCII 字符串
                String result = hexToAscii(contentHex);
                System.out.println("\n>>> ✅ 最终指令信息: set(\"" + result + "\")");
            } else {
                System.out.println("数据长度不足，解析被截断");
            }

        } else {
            System.out.println("    -> 未知函数，需要补充 Case");
        }
    }

    /**
     * 工具方法：十六进制转字符串
     * 例如: "68656c6c6f" -> "hello"
     */
    public static String hexToAscii(String hexStr) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < hexStr.length(); i += 2) {
            String str = hexStr.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }
        return output.toString();
    }
}