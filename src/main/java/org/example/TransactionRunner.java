package org.example;

import org.fisco.bcos.sdk.v3.BcosSDK;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;

//调用helloworld.java来set一个值

public class TransactionRunner {
    public static void main(String[] args) throws Exception {
        // 1. 初始化 SDK 和 账户
        BcosSDK sdk = BcosSDK.build("config.toml");
        Client client = sdk.getClient("group0");
        CryptoKeyPair keyPair = client.getCryptoSuite().generateRandomKeyPair(); // 创建一个随机账户

        System.out.println(">>> 当前账户: " + keyPair.getAddress());

        // 2. 部署 HelloWorld 合约
        // 利用您提供的 HelloWorld.java 中的 deploy 方法
        System.out.println(">>> 正在部署合约...");
        HelloWorld helloWorld = HelloWorld.deploy(client, keyPair);

        System.out.println("✅ 合约部署成功!");
        System.out.println("   合约地址: " + helloWorld.getContractAddress());

        // 3. 发起 set("xyz") 交易
        System.out.println("\n>>> 正在调用 set(\"xyz\")...");
        // 直接调用包装好的 set 方法
        TransactionReceipt receipt = helloWorld.set("xyz");

        // 4. 打印结果 (发送端确认)
        System.out.println("✅ 交易已上链!");
        System.out.println("   交易哈希: " + receipt.getTransactionHash());
        System.out.println("   执行状态: " + receipt.getStatus() + " (0=成功)");
        System.out.println("   Input数据: " + receipt.getInput()); // 这里应该是 set("xyz") 的编码

        // 5. (可选) 立即查询一下验证
        String currentValue = helloWorld.get();
        System.out.println("   链上最新值: " + currentValue);

        System.exit(0);
    }
}