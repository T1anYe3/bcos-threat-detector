package org.example;

import org.fisco.bcos.sdk.v3.BcosSDK;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosBlock;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosBlock.Block;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosBlock.TransactionResult;

import java.math.BigInteger;
import java.util.List;

public class HelloWorldBlockInfo {
    public static void main(String[] args) throws Exception {
        // 1. 初始化SDK和Client
        String configFile = "config.toml"; // 替换为你的配置文件路径
        BcosSDK sdk = BcosSDK.build(configFile);
        Client client = sdk.getClient("group0");

        // 2. 获取当前账户密钥对
        CryptoKeyPair keyPair = client.getCryptoSuite().getCryptoKeyPair();

        // 3. 部署HelloWorld合约
        HelloWorld helloWorld = HelloWorld.deploy(client, keyPair);
        System.out.println("合约部署成功，地址: " + helloWorld.getContractAddress());

        // 4. 调用set方法发起一笔交易
        TransactionReceipt receipt = helloWorld.set("hello, block!");
        System.out.println("交易哈希: " + receipt.getTransactionHash());
        System.out.println("区块号: " + receipt.getBlockNumber());

        // 5. 直接获取区块号，无需解析
        BigInteger blockNumber = receipt.getBlockNumber();

        // 6. 等待区块上链
        while (true) {
            BigInteger latestBlock = client.getBlockNumber().getBlockNumber();
            if (latestBlock.compareTo(blockNumber) >= 0) {
                break;
            }
            System.out.println("等待区块产生... 当前块号: " + latestBlock + " 目标块号: " + blockNumber);
            Thread.sleep(2000);
        }

        // 7. 区块查询与输出
        BcosBlock blockResponse = client.getBlockByNumber(blockNumber, false, false);
        Block block = blockResponse.getBlock();

        System.out.println("区块号: " + block.getNumber());
        System.out.println("时间戳: " + block.getTimestamp());
        long timestamp = block.getTimestamp();
        java.util.Date date = new java.util.Date(timestamp);
        System.out.println(date);
// 或格式化输出
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println(sdf.format(date));
        System.out.println("父区块哈希: " + block.getParentInfo());
        System.out.println("区块哈希: " + block.getHash());

        List<TransactionResult> transactionResults = block.getTransactions();
        System.out.println("交易哈希列表:");
        for (TransactionResult transactionResult : transactionResults) {
            System.out.println(transactionResult.get());
        }

        String value = helloWorld.get();
        System.out.println("合约当前存储值: " + value);
    }
}