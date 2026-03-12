package org.example;

import java.math.BigInteger;

public class InstructionAnalyzer {
    public static class DecodeResult {
        public String callType = "UNKNOWN";
        public String methodId = "";
        public String methodName = "Unknown";
        public int isSensitive = 0;
        public int decodeSuccess = 0;
        public String argSummary = "";
    }

    /**
     * 对外的主入口方法
     * @param input 交易的原始 input 数据
     * @param toAddress 交易的接收方地址 (用于判断是否为部署交易)
     */
    public static void decode(String input, String toAddress) {
        DecodeResult result = analyze(input, toAddress);
        System.out.println(">>> 指令类型: [" + result.callType + "]");
        if (result.methodId != null && !result.methodId.isEmpty()) {
            System.out.println("    -> 函数签名 Hash: " + result.methodId);
        }
        if (result.methodName != null && !result.methodName.isEmpty()) {
            System.out.println("    -> 识别函数: " + result.methodName);
        }
        if (result.argSummary != null && !result.argSummary.isEmpty()) {
            System.out.println("    -> 参数摘要: " + result.argSummary);
        }
    }

    /**
     * 结构化语义分析入口：供实时监控/数据库写入使用。
     */
    public static DecodeResult analyze(String input, String toAddress) {
        DecodeResult result = new DecodeResult();

        // 1. 基础校验
        if (input == null || input.length() < 10) {
            result.callType = "INVALID_INPUT";
            result.methodName = "InvalidInput";
            result.argSummary = "input empty/short";
            return result;
        }

        // 2. 判断是否为【合约部署】
        // 逻辑：To 地址为空/全0，或者 Input 以 Solidity 标准部署头(60806040)开头
        if (isContractDeploy(toAddress, input)) {
            result.callType = "CONTRACT_DEPLOYMENT";
            result.methodName = "ContractDeployment";
            result.decodeSuccess = 1;
            result.argSummary = "bytecode_len=" + input.length();
            return result;
        }

        // 3. 判断是否为【普通转账】(非合约调用)
        // 逻辑：To 有地址，但 Input 只有 "0x" 或为空
        if (input.equals("0x")) {
            result.callType = "NATIVE_TRANSFER";
            result.methodName = "NativeTransfer";
            result.decodeSuccess = 1;
            return result;
        }

        // 4. 进入【函数调用】解析流程
        result.callType = "FUNCTION_CALL";
        parseFunctionCall(input, result);
        return result;
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
    private static void parseFunctionCall(String input, DecodeResult result) {
        // 截取函数签名 Hash (前10位)
        String methodId = input.substring(0, 10);
        result.methodId = methodId;

        // --- 开始 Case 匹配 ---

        // Case 1: set(string) - ID: 0x4ed3885e
        if ("0x4ed3885e".equals(methodId)) {
            result.methodName = "set(string)";
            result.isSensitive = 1;
            result.decodeSuccess = 1;
            String val = decodeStringParam(input);
            result.argSummary = val.isEmpty() ? "set(<decode_failed>)" : ("set(\"" + val + "\")");
        }
        // Case 2: transfer(address,uint256) - ID: 0xa9059cbb
        else if ("0xa9059cbb".equals(methodId)) {
            result.methodName = "transfer(address,uint256)";
            result.decodeSuccess = 1;
            result.argSummary = decodeTransferParams(input);
        }
        // Case 3: get() - ID: 0x6d4ce63c
        else if ("0x6d4ce63c".equals(methodId)) {
            result.methodName = "get()";
            result.decodeSuccess = 1;
            result.argSummary = "no_args";
        }
        // 未知函数
        else {
            result.methodName = "UnknownFunction";
            result.decodeSuccess = 0;
            result.argSummary = "fallback_generic_decode";
            GenericDecoder.decode(input);
        }
    }

    // --- 下面是具体的参数解码工具方法 ---

    private static String decodeStringParam(String input) {
        try {
            String params = input.substring(10);
            // 简单校验 string 编码长度
            if (params.length() < 128) return "";

            // 读取长度 (跳过前64位偏移量，取后64位长度)
            String lengthHex = params.substring(64, 128);
            int length = new BigInteger(lengthHex, 16).intValue();

            // 读取内容
            int dataStart = 128;
            int dataEnd = dataStart + (length * 2);
            if (params.length() >= dataEnd) {
                String contentHex = params.substring(dataStart, dataEnd);
                return hexToAscii(contentHex);
            }
        } catch (Exception e) {
            return "";
        }
        return "";
    }

    private static String decodeTransferParams(String input) {
        try {
            String params = input.substring(10);
            if (params.length() < 128) return "decode_short_params";

            String toHex = params.substring(0, 64);
            String amountHex = params.substring(64, 128);

            String toAddress = "0x" + toHex.substring(24);
            BigInteger amount = new BigInteger(amountHex, 16);
            return "to=" + toAddress + ",amount=" + amount;
        } catch (Exception e) {
            return "decode_transfer_error";
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