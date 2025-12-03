package org.example;

import org.fisco.bcos.sdk.v3.BcosSDK;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.client.protocol.model.JsonTransactionResponse;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosBlock;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosBlock.Block;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosBlock.TransactionObject;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosBlock.TransactionResult;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosTransaction;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

public class OP { // 您可以根据您的文件名修改这里的类名
    public static void main(String[] args) {
        String configFile = "config.toml";
        BcosSDK sdk = BcosSDK.build(configFile);

        String outputFileName = "blockchain_data_custom.tsv";

        try (FileWriter fileWriter = new FileWriter(outputFileName);
             PrintWriter printWriter = new PrintWriter(fileWriter)) {

            // 1. 按照您指定的顺序定义TSV文件的表头
            StringJoiner header = new StringJoiner("\t");
            header.add("blocknumber").add("blockhash").add("parentblockhash").add("tradehash");
            header.add("gasused").add("timestamp");
            header.add("from").add("to").add("nonce").add("gaslimit").add("gasprice").add("blocktradenumber");
            printWriter.println(header.toString());
            System.out.println("开始将数据写入到 " + outputFileName + " ...");

            Client client = sdk.getClient("group0");
            BigInteger latestBlockNumber = client.getBlockNumber().getBlockNumber();
            System.out.println("当前最新区块号: " + latestBlockNumber);
            System.out.println("开始从创世区块遍历至最新区块...");

            for (BigInteger i = BigInteger.ONE; i.compareTo(latestBlockNumber) <= 0; i = i.add(BigInteger.ONE)) {
                System.out.println("\n========== 正在处理区块: " + i + " ==========");
                BcosBlock blockResponse = client.getBlockByNumber(i, false, true);
                Block block = blockResponse.getBlock();
//                System.out.println("当前最新区块号: " + block.getTransactionHashes());
                String extractedHash = TransactionUtil.extractHashFromLogString(block.getTransactionHashes().toString());
                String parextractedHash = TransactionUtil.extractHashFromLogString(block.getParentInfo().toString());

                long timestamp = block.getTimestamp();
                List<BcosBlock.TransactionResult> txs = block.getTransactions();
                String txHash = extractedHash;
                String txsize= String.valueOf(txs.size());
                System.out.println("区块内交易数量:" + txsize);
                if (txs != null) {
//                    System.out.println("区块内交易数量: " + txs.size());
                    int txIndex = -1;

                    for (int j = 0; j < txs.size(); j++) {
                        // 确保列表中的元素也不为null，虽然通常不会发生
                        if (txs.get(j) != null) {
                            String hash = ((BcosBlock.TransactionHash) txs.get(j)).get();
                            System.out.println("区块内交易哈希列表:" + hash);
                            if (hash.equals(txHash)) {
                                txIndex = j;
                            }
                        }
                    }
                    System.out.println("本交易在区块内序号: " + (txIndex >= 0 ? txIndex : "未找到"));
                } else {
                    System.out.println("区块内交易数量: 0 (交易列表为null)");
                }

                BcosTransaction txResp = client.getTransaction(txHash, false);
                Optional<JsonTransactionResponse> txOptional = txResp.getTransaction();
                if (txOptional.isPresent()) {
                    JsonTransactionResponse tx = txOptional.get(); // 从Optional中获取JsonTransactionResponse对象

                    StringJoiner row = new StringJoiner("\t");
                    row.add(String.valueOf(block.getNumber())); // 区块号
                    row.add(block.getHash()); // 区块哈希
                    row.add(parextractedHash); // 父区块哈希
                    row.add(extractedHash); // 交易哈希
                    row.add(block.getGasUsed()); // gas消耗
                    row.add(String.valueOf(timestamp)); // 时间戳
                    row.add(tx.getFrom()); // from
                    row.add(tx.getTo()); // to
                    row.add(tx.getNonce()); // nonce
                    row.add(String.valueOf(tx.getGasLimit())); // gaslimit
                    row.add(tx.getGasPrice()); // gasprice
                    row.add(txsize); // 区块内交易数量
                    // 4. 将该行数据写入文件
                    printWriter.println(row.toString());
                }
            }

            System.out.println("\n所有区块处理完毕！数据已成功保存到 " + outputFileName);

        } catch (IOException e) {
            System.err.println("写入文件时发生错误: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (sdk != null) {
                sdk.stopAll();
            }
        }
    }
}