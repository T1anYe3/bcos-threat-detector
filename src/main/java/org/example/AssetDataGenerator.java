package org.example;

import com.Asset;
import org.fisco.bcos.sdk.v3.BcosSDK;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AssetDataGenerator {
    // 模拟的用户数量
    private static final int USER_COUNT = 10;
    // 要生成的总交易数量
    private static final int TOTAL_TX_COUNT = 100;

    private static Client client;
    private static CryptoKeyPair keyPair;
    private static Asset assetContract;
    private static Random random = new Random();
    private static List<String> userList = new ArrayList<>();

    public static void main(String[] args) {
        try {
            // 1. 初始化
            BcosSDK sdk = BcosSDK.build("config.toml");
            client = sdk.getClient("group0");
            keyPair = client.getCryptoSuite().generateRandomKeyPair();

            System.out.println(">>> [初始化] 正在使用账户: " + keyPair.getAddress());

            // 2. 部署 Asset 合约
            System.out.println(">>> [1/4] 正在部署 Asset 合约...");
            assetContract = Asset.deploy(client, keyPair);
            System.out.println("✅ Asset 合约地址: " + assetContract.getContractAddress());

            // 3. 阶段一：注册用户 (资产发行)
            System.out.println("\n>>> [2/4] 阶段一：批量注册用户 (Register)...");
            for (int i = 0; i < USER_COUNT; i++) {
                String username = "User" + i;
                userList.add(username);
                // 每个人初始 10000 块钱
                BigInteger initAmount = BigInteger.valueOf(10000);

                TransactionReceipt receipt = assetContract.register(username, initAmount);
                printLog("注册", username, receipt);
            }

            // 4. 阶段二：混合流量模拟 (转账 + 攻击)
            System.out.println("\n>>> [3/4] 阶段二：开始高频交易与异常模拟 (" + TOTAL_TX_COUNT + "笔)...");

            for (int i = 0; i < TOTAL_TX_COUNT; i++) {
                int action = random.nextInt(100); // 0-99

                if (action < 70) {
                    // [70% 概率] 正常转账 (A 转给 B)
                    doNormalTransfer();
                } else if (action < 90) {
                    // [20% 概率] 异常攻击：透支转账 (Money Not Enough)
                    doOverdraftAttack();
                } else {
                    // [10% 概率] 异常攻击：重复注册 (Account Exists)
                    doRepeatRegisterAttack();
                }

                // 模拟真实间隔 (50ms - 200ms)
                Thread.sleep(50 + random.nextInt(150));
            }

            System.out.println("\n>>> [4/4] ✅ 数据生成完成！请运行 New.java 进行采集。");
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 模拟正常转账：随机选两个人，转随机金额
     */
    private static void doNormalTransfer() {
        String from = getRandomUser();
        String to = getRandomUser();
        // 避免自己转给自己
        while (from.equals(to)) {
            to = getRandomUser();
        }

        // 转账 1-100 块
        BigInteger amount = BigInteger.valueOf(random.nextInt(100) + 1);

        TransactionReceipt receipt = assetContract.transfer(from, to, amount);
        printLog("转账", from + " -> " + to + " (" + amount + ")", receipt);
    }

    /**
     * 模拟透支攻击：穷人非要转巨款
     * 预期结果：Status != 0 (失败)
     */
    private static void doOverdraftAttack() {
        String from = getRandomUser();
        String to = getRandomUser();
        // 试图转账 1 个亿
        BigInteger hugeAmount = BigInteger.valueOf(100000000);

        TransactionReceipt receipt = assetContract.transfer(from, to, hugeAmount);
        printLog("异常-透支", from + " 试图转账 1亿", receipt);
    }

    /**
     * 模拟重复注册攻击：注册一个已经存在的用户
     * 预期结果：Status != 0 (失败)
     */
    private static void doRepeatRegisterAttack() {
        String existingUser = getRandomUser();
        BigInteger amount = BigInteger.valueOf(1000);

        TransactionReceipt receipt = assetContract.register(existingUser, amount);
        printLog("异常-重注册", "恶意抢注 " + existingUser, receipt);
    }

    // 辅助：随机选人
    private static String getRandomUser() {
        return userList.get(random.nextInt(userList.size()));
    }

    // 辅助：打印日志，特别是高亮失败交易
    private static void printLog(String type, String detail, TransactionReceipt receipt) {
        String status = receipt.getStatus() == 0 ? "成功" : "❌失败(" + receipt.getStatus() + ")";
        String txHash = receipt.getTransactionHash().substring(0, 8) + "...";
        System.out.printf("[%s] %-20s | Hash: %s | 状态: %s%n", type, detail, txHash, status);
    }
}