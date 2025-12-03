package org.example;

import java.math.BigInteger;

public class InstructionAnalyzer {

    /**
     * 对外的主入口方法
     * @param input 交易的原始 input 数据
     * @param toAddress 交易的接收方地址 (用于判断是否为部署交易)
     */
    public static void decode(String input, String toAddress) {
        // 1. 基础校验
        if (input == null || input.length() < 10) {
            System.out.println("    -> [状态] Input 为空或格式不正确");
            return;
        }

        // 2. 判断是否为【合约部署】
        // 逻辑：To 地址为空/全0，或者 Input 以 Solidity 标准部署头(60806040)开头
        if (isContractDeploy(toAddress, input)) {
            System.out.println(">>> 指令类型: [合约部署] (Contract Deployment)");
            System.out.println("    -> 详情: 正在链上创建新合约");
            System.out.println("    -> 数据: 包含合约二进制代码 (Bytecode)，长度 " + input.length() + " 字符");
            return; // 部署交易不需要解析参数，直接结束
        }

        // 3. 判断是否为【普通转账】(非合约调用)
        // 逻辑：To 有地址，但 Input 只有 "0x" 或为空
        if (input.equals("0x")) {
            System.out.println(">>> 指令类型: [原生代币转账] (Native Token Transfer)");
            return;
        }

        // 4. 进入【函数调用】解析流程
        System.out.println(">>> 指令类型: [函数调用] (Function Call)");
        parseFunctionCall(input);
    }

    /**
     * 辅助方法：判断是否为部署交易
     */
    private static boolean isContractDeploy(String to, String input) {
        // 这里的 0x0...0 是 FISCO BCOS 或以太坊的空地址标准
        boolean isZeroAddress = to == null ||
                to.isEmpty() ||
                "0x0000000000000000000000000000000000000000".equals(to);

        // 0x60806040 是 Solidity 编译器生成的合约代码常见开头
        boolean isDeployCode = input.startsWith("0x60806040");

        return isZeroAddress || isDeployCode;
    }

    /**
     * 核心解析逻辑：识别函数签名并解码参数
     */
    private static void parseFunctionCall(String input) {
        // 截取函数签名 Hash (前10位)
        String methodId = input.substring(0, 10);
        System.out.println("    -> 函数签名 Hash: " + methodId);

        // --- 开始 Case 匹配 ---

        // Case 1: set(string) - ID: 0x4ed3885e
        if ("0x4ed3885e".equals(methodId)) {
            System.out.println("    -> 识别为函数: set(string)");
            decodeStringParam(input);
        }
        // Case 2: transfer(address,uint256) - ID: 0xa9059cbb
        else if ("0xa9059cbb".equals(methodId)) {
            System.out.println("    -> 识别为函数: transfer(address,uint256)");
            decodeTransferParams(input);
        }
        // Case 3: get() - ID: 0x6d4ce63c
        else if ("0x6d4ce63c".equals(methodId)) {
            System.out.println("    -> 识别为函数: get()");
            System.out.println("    -> 参数: 无");
        }
        // 未知函数
        else {
            System.out.println("    -> 未知函数 (盲解)");
            GenericDecoder.decode(input);
        }
    }

    // --- 下面是具体的参数解码工具方法 ---

    private static void decodeStringParam(String input) {
        try {
            String params = input.substring(10);
            // 简单校验 string 编码长度
            if (params.length() < 128) return;

            // 读取长度 (跳过前64位偏移量，取后64位长度)
            String lengthHex = params.substring(64, 128);
            int length = new BigInteger(lengthHex, 16).intValue();

            // 读取内容
            int dataStart = 128;
            int dataEnd = dataStart + (length * 2);
            if (params.length() >= dataEnd) {
                String contentHex = params.substring(dataStart, dataEnd);
                String result = hexToAscii(contentHex);
                System.out.println("    -> 解析内容: set(\"" + result + "\")");
            }
        } catch (Exception e) {
            System.out.println("    -> String 解析错误");
        }
    }

    private static void decodeTransferParams(String input) {
        try {
            String params = input.substring(10);
            if (params.length() < 128) return;

            String toHex = params.substring(0, 64);
            String amountHex = params.substring(64, 128);

            String toAddress = "0x" + toHex.substring(24);
            BigInteger amount = new BigInteger(amountHex, 16);

            System.out.println("    -> 解析内容: 向 " + toAddress + " 转账 " + amount);
        } catch (Exception e) {
            System.out.println("    -> Transfer 解析错误");
        }
    }

    public static String hexToAscii(String hexStr) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < hexStr.length(); i += 2) {
            String str = hexStr.substring(i, i + 2);
            // 过滤掉不可见字符 (00)
            if (!str.equals("00")) {
                output.append((char) Integer.parseInt(str, 16));
            }
        }
        return output.toString();
    }
}