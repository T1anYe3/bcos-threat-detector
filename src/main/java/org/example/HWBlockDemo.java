package org.example;

import org.fisco.bcos.sdk.v3.BcosSDK;
import org.fisco.bcos.sdk.v3.client.Client;
//import org.fisco.bcos.sdk.v3.client.protocol.request.Transaction;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosBlock;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosBlock.Block;
//import org.fisco.bcos.sdk.v3.client.protocol.response.BcosBlock.TransactionObject;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosTransaction;
//import org.fisco.bcos.sdk.v3.transaction.model.dto.TransactionResponse;
import java.util.Optional;
import org.fisco.bcos.sdk.v3.client.protocol.model.JsonTransactionResponse;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
//import java.util.Collections;
import java.util.Date;
import java.util.List;

public class HWBlockDemo {
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
        System.out.println("\n========= 交易回执信息 =========");
        System.out.println("交易哈希: " + receipt.getTransactionHash());
        System.out.println("区块号: " + receipt.getBlockNumber());
        System.out.println("发起人: " + receipt.getFrom());
        System.out.println("目标地址: " + receipt.getTo());
        //System.out.println("合约地址(如是部署): " + receipt.getContractAddress());
        System.out.println("gas消耗: " + receipt.getGasUsed());
        System.out.println("状态: " + receipt.getStatus());
        System.out.println("输入: " + receipt.getInput());
        System.out.println("输出: " + receipt.getOutput());
        System.out.println("错误消息: " + receipt.getMessage());

        // 5. 获取区块号并等待区块上链
        BigInteger blockNumber = receipt.getBlockNumber();
        while (true) {
            BigInteger latestBlock = client.getBlockNumber().getBlockNumber();
            if (latestBlock.compareTo(blockNumber) >= 0) {
                break;
            }
            System.out.println("等待区块产生... 当前块号: " + latestBlock + " 目标块号: " + blockNumber);
            Thread.sleep(2000);
        }

        // 6. 区块查询与输出
        BcosBlock blockResponse = client.getBlockByNumber(blockNumber, false, true);
        BcosBlock.Block block = blockResponse.getBlock();

        System.out.println("\n========= 区块信息 =========");
        System.out.println("区块号: " + block.getNumber());
        System.out.println("区块哈希: " + block.getHash());
        System.out.println("父区块哈希: " + block.getParentInfo());
        System.out.println("交易哈希: " + block.getTransactionHashes());
        System.out.println("Gas消耗: " + block.getGasUsed());


        long timestamp = block.getTimestamp();
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("时间戳: " + timestamp + " (" + sdf.format(date) + ")");
        //System.out.println("区块日志过滤器 logsBloom: " + block.getLogsBloom());
        // 这是修正后的代码（增加了null检查）
        List<BcosBlock.TransactionResult> txs = block.getTransactions();
        String txHash = receipt.getTransactionHash();

// 增加一个健壮性判断，防止区块返回的交易列表为null
        if (txs != null) {
            System.out.println("区块内交易数量: " + txs.size());

            // 所有交易哈希
            //System.out.println("区块内交易哈希列表:");
            int txIndex = -1;


            for (int i = 0; i < txs.size(); i++) {
                // 确保列表中的元素也不为null，虽然通常不会发生
                if (txs.get(i) != null) {
                    String hash = ((BcosBlock.TransactionHash) txs.get(i)).get();
                    System.out.println("  " + hash);
                    if (hash.equals(txHash)) {
                        txIndex = i;
                    }
                }
            }
            System.out.println("本交易在区块内序号: " + (txIndex >= 0 ? txIndex : "未找到"));
        } else {
            System.out.println("区块内交易数量: 0 (交易列表为null)");
        }

        // 7. 通过哈希获取完整交易对象
        //System.out.println("\n========= 交易详细信息 =========");
        String groupId = "group0"; // 如果你是默认创建的链，groupId 就是 group0
        BcosTransaction txResp = client.getTransaction(txHash, false);
       //System.out.println("BcosTransaction 原始响应: " + txResp);


        Optional<JsonTransactionResponse> txOptional= txResp.getTransaction();
        if (txOptional.isPresent()) {
            JsonTransactionResponse tx = txOptional.get(); // 从Optional中获取JsonTransactionResponse对象
            System.out.println("交易哈希: " + tx.getHash());
            // 注意：区块号和区块哈希是交易被打包后的信息，属于交易回执(Receipt)，
            // 通过getTransaction获取的交易对象本身不包含这些信息。
            System.out.println("from: " + tx.getFrom());
            System.out.println("to: " + tx.getTo());
            System.out.println("nonce: " + tx.getNonce());
            System.out.println("gasLimit: " + tx.getGasLimit());
            System.out.println("gasPrice: " + tx.getGasPrice());
            System.out.println("input: " + tx.getInput());
            //System.out.println("groupId: " + tx.getGroupID());
            //System.out.println("chainId: " + tx.getChainID());
            // 注意：根据API，签名(signature)和导入时间(importTime)等字段可能不在这个对象中
            // 如果需要这些信息，可能需要从其他返回对象中获取
        } else {
            System.out.println("未能查到该交易详细信息。");
        }

        // 8. 合约数据
        String value = helloWorld.get();
        System.out.println("\n========= 合约内容 =========");
        System.out.println("合约当前存储值: " + value);
    }
}