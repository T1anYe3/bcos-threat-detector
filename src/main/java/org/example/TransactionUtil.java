package org.example;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 这是一个交易相关的工具类，提供一些可重用的方法。
 */
public class TransactionUtil {

    /**
     * 从一个包含 TransactionHash 对象的打印字符串中，提取出 'value' 字段的哈希值。
     * * @param inputString 形如 "[TransactionHash{value='0x...'}]" 的字符串。
     * @return 提取出的哈希字符串；如果找不到则返回 null。
     */
    public static String extractHashFromLogString(String inputString) {
        if (inputString == null || inputString.isEmpty()) {
            return null;
        }

        // 使用正则表达式来精确匹配单引号之间的内容
        Pattern pattern = Pattern.compile("'([^']*)'");
        Matcher matcher = pattern.matcher(inputString);

        // 如果找到了匹配项
        if (matcher.find()) {
            // group(1) 获取第一个捕获组的内容，即两个单引号之间的哈希
            return matcher.group(1);
        }

        // 如果没有找到匹配项
        return null;
    }

    // 您未来还可以添加更多工具方法在这里...
    // public static void anotherUtilityMethod() { ... }
}