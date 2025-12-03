package org.example;

import org.fisco.bcos.sdk.v3.BcosSDK;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosBlock;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosBlock.Block;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosBlock.TransactionResult;
import java.math.BigInteger;
import java.util.List;

public class GetBlockInfo {
    public static void main(String[] args) {
        // 加载 SDK 配置文件
        String configFile = "config.toml"; // 请替换为你的配置文件路径
        BcosSDK sdk = BcosSDK.build(configFile);

        // 获取群组1的client对象
        Client client = sdk.getClient("group0");

        try {
            // 以区块号获取区块信息，比如取最新块
            //BigInteger blockNumber = client.getBlockNumber().getBlockNumber();
            BigInteger blockNumber = BigInteger.valueOf(151);
            BcosBlock blockResponse = client.getBlockByNumber(blockNumber, false, false);
            Block block = blockResponse.getBlock();

            System.out.println("区块号: " + block.getNumber());
            System.out.println("时间戳: " + block.getTimestamp());
            //System.out.println("父区块哈希: " + block.getParentHash());
            System.out.println("区块哈希: " + block.getHash());

            // 获取交易哈希
            List<TransactionResult> transactionResults = block.getTransactions();
            System.out.println("交易哈希列表:");
            for (TransactionResult transactionResult : transactionResults) {
                System.out.println(transactionResult.get());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}