package org.example;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenericDecoder {

    // 1. 内置一个常用函数“字典” (4-Byte Database)
    // 虽然不能解所有，但能覆盖 90% 的标准操作 (ERC20, 权限控制等)
    private static final Map<String, String> KNOWN_SIGNATURES = new HashMap<>();

    static {
        // 标准转账/授权
        KNOWN_SIGNATURES.put("0xa9059cbb", "transfer(address,uint256)");
        KNOWN_SIGNATURES.put("0x095ea7b3", "approve(address,uint256)");
        KNOWN_SIGNATURES.put("0x23b872dd", "transferFrom(address,address,uint256)");
        // 您的自定义合约
        KNOWN_SIGNATURES.put("0x4ed3885e", "set(string)");
        KNOWN_SIGNATURES.put("0x6d4ce63c", "get()");
        // 常见攻击/管理
        KNOWN_SIGNATURES.put("0xf2fde38b", "transferOwnership(address)");
        KNOWN_SIGNATURES.put("0x8da5cb5b", "owner()");
        // ... 您可以从网上下载 "Ethereum 4byte directory.json" 填充这里
        KNOWN_SIGNATURES.put("0x9b80b050", "transfer(string,string,uint256)"); //
        KNOWN_SIGNATURES.put("0xd22e0e56", "register(string,uint256)");        // 注册资产
        // 核心交易
        // 查询状态 (通常是读操作，但也可能上链)
        KNOWN_SIGNATURES.put("0x70a08231", "balanceOf(address)");                 // 查余额
        KNOWN_SIGNATURES.put("0x18160ddd", "totalSupply()");                      // 查总量
        KNOWN_SIGNATURES.put("0xdd62ed3e", "allowance(address,address)");         // 查授权额度

        KNOWN_SIGNATURES.put("0x715018a6", "renounceOwnership()");                // 放弃管理员权限
        KNOWN_SIGNATURES.put("0x24d7806c", "pause()");                            // 暂停合约 (紧急熔断)
        KNOWN_SIGNATURES.put("0x3f4ba83a", "unpause()");                          // 恢复合约
    }

    /**
     * 主入口：通用解码
     */
    public static void decode(String input) {
        if (input == null || input.length() < 10) return;

        // 1. 提取函数签名
        String methodId = input.substring(0, 10);
        String functionName = KNOWN_SIGNATURES.getOrDefault(methodId, "UnknownFunction-" + methodId);

        System.out.println(">>> [通用解码] 指令: " + functionName);

        // 2. 切片参数数据 (去掉前10位)
        String paramsRaw = input.substring(10);

        // EVM 参数通常是 32 字节 (64字符) 一组
        List<String> chunks = splitTo32Bytes(paramsRaw);

        // 3. 逐个分析参数类型 (启发式猜测)
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            String guess = guessType(chunk);
            System.out.printf("    -> 参数[%d]: %s%n", i, guess);
        }
    }

    /**
     * 核心逻辑：像切香肠一样把数据切成 64 字符一段
     */
    private static List<String> splitTo32Bytes(String data) {
        List<String> chunks = new ArrayList<>();
        int length = data.length();
        for (int i = 0; i < length; i += 64) {
            // 防止最后一段不足 64 字符
            int end = Math.min(length, i + 64);
            chunks.add(data.substring(i, end));
        }
        return chunks;
    }

    /**
     * 核心算法：启发式猜测 (Heuristic Guessing)
     * 根据数据的长相，猜它是地址、数字、还是字符串指针
     */
    private static String guessType(String hex32Bytes) {
        // 转为 BigInteger 方便判断
        BigInteger val;
        try {
            val = new BigInteger(hex32Bytes, 16);
        } catch (Exception e) {
            return "Raw Hex: " + hex32Bytes;
        }

        // 规则 1: 看起来像 Boolean (0 或 1)
        if (val.equals(BigInteger.ZERO)) return "Value: 0 (False/Zero)";
        if (val.equals(BigInteger.ONE)) return "Value: 1 (True/One)";

        // 规则 2: 看起来像 Address (地址)
        // 特征：高位全是0，只有低位有值，且非超小数值
        // 地址是 20 字节 (40字符)，所以前 24 个字符应该是 0
        if (hex32Bytes.startsWith("000000000000000000000000")) {
            // 且值比较大，不像普通计数器
            if (hex32Bytes.length() == 64) {
                String potentialAddress = "0x" + hex32Bytes.substring(24);
                return "Address?: " + potentialAddress;
            }
        }

        // 规则 3: 看起来像指针 (指向字符串长度)
        // 通常是 0x20, 0x40, 0x60 这种整齐的偏移量
        if (val.compareTo(BigInteger.valueOf(32)) == 0 ||
                val.compareTo(BigInteger.valueOf(64)) == 0 ||
                val.compareTo(BigInteger.valueOf(96)) == 0) {
            return "Pointer/Offset?: " + val + " (可能指向后续的长文本)";
        }

        // 规则 4: 看起来像普通数值 (Timestamp 或 Amount)
        // 时间戳通常是 10 亿以上 (1600000000+)
        if (val.compareTo(BigInteger.valueOf(1600000000L)) > 0 &&
                val.compareTo(BigInteger.valueOf(2000000000L)) < 0) {
            return "Timestamp?: " + val + " (约 " + new java.util.Date(val.longValue()*1000) + ")";
        }

        // 默认：当成大整数处理
        return "Number/UID: " + val;
    }
}