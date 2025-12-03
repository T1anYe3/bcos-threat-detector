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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class New {
    public static void main(String[] args) throws Exception {
        // 1. 初始化SDK和Client
        String configFile = "config.toml"; // 替换为你的配置文件路径
        BcosSDK sdk = BcosSDK.build(configFile);
        Client client = sdk.getClient("group0");


        BigInteger BigblockNumber = BigInteger.valueOf(10);
//          BigInteger blockNumber = receipt.getBlockNumber();
        for (BigInteger blockNumber = BigInteger.ONE; blockNumber.compareTo(BigblockNumber) <= 0; blockNumber = blockNumber.add(BigInteger.ONE)) {
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

            Block block = blockResponse.getBlock();
            String extractedHash = TransactionUtil.extractHashFromLogString(block.getTransactionHashes().toString());
            String parextractedHash = TransactionUtil.extractHashFromLogString(block.getParentInfo().toString());

            System.out.println("\n========= 区块信息 =========");
            System.out.println("区块号: " + block.getNumber());
            System.out.println("区块哈希: " + block.getHash());
            System.out.println("父区块哈希: " + parextractedHash);
            System.out.println("交易哈希: " + extractedHash);
            System.out.println("Gas消耗: " + block.getGasUsed());

            long timestamp = block.getTimestamp();
            Date date = new Date(timestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//            System.out.println("时间戳: " + timestamp + " (" + sdf.format(date) + ")");
            System.out.println("时间戳: " + timestamp );
            List<BcosBlock.TransactionResult> txs = block.getTransactions();
            String txHash = extractedHash;
// 增加一个健壮性判断，防止区块返回的交易列表为null
            if (txs != null) {
                System.out.println("区块内交易数量: " + txs.size());
                int txIndex = -1;

                for (int i = 0; i < txs.size(); i++) {
                    // 确保列表中的元素也不为null，虽然通常不会发生
                    if (txs.get(i) != null) {
                        String hash = ((BcosBlock.TransactionHash) txs.get(i)).get();
//                        System.out.println("区块内交易哈希列表:" + hash);
                        if (hash.equals(txHash)) {
                            txIndex = i;
                        }
                    }
                }
                System.out.println("本交易在区块内序号: " + (txIndex >= 0 ? txIndex : "未找到"));
            } else {
                System.out.println("区块内交易数量: 0 (交易列表为null)");
            }
            String groupId = "group0"; // 如果你是默认创建的链，groupId 就是 group0
            BcosTransaction txResp = client.getTransaction(txHash, false);

            Optional<JsonTransactionResponse> txOptional = txResp.getTransaction();
            if (txOptional.isPresent()) {
                JsonTransactionResponse tx = txOptional.get(); // 从Optional中获取JsonTransactionResponse对象
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
            } else {
                System.out.println("未能查到该交易详细信息。");
            }
        }
    }
}