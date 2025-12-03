package org.example;

import org.fisco.bcos.sdk.v3.BcosSDK;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.client.protocol.model.JsonTransactionResponse;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosBlock;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosBlock.Block;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosTransaction;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosTransactionReceipt; // [新增] 用于获取交易状态

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class test1202 {
    public static void main(String[] args) throws Exception {
        // 1. 初始化SDK和Client
        String configFile = "config.toml";
        BcosSDK sdk = BcosSDK.build(configFile);
        Client client = sdk.getClient("group0");

        BigInteger BigblockNumber = BigInteger.valueOf(2);

        for (BigInteger blockNumber = BigInteger.ONE; blockNumber.compareTo(BigblockNumber) <= 0; blockNumber = blockNumber.add(BigInteger.ONE)) {
            while (true) {
                BigInteger latestBlock = client.getBlockNumber().getBlockNumber();
                if (latestBlock.compareTo(blockNumber) >= 0) {
                    break;
                }
                System.out.println("等待区块产生... 当前块号: " + latestBlock + " 目标块号: " + blockNumber);
                Thread.sleep(2000);
            }

            // 6. 区块查询
            BcosBlock blockResponse = client.getBlockByNumber(blockNumber, false, true);
            Block block = blockResponse.getBlock();

            // [新增] 获取 Sealer (区块信息补充)
            // block.getSealer() 返回打包节点在共识列表中的索引 (如 "0", "1")
            String sealer = String.valueOf(block.getSealer());

            String extractedHash = TransactionUtil.extractHashFromLogString(block.getTransactionHashes().toString());
            String parextractedHash = TransactionUtil.extractHashFromLogString(block.getParentInfo().toString());

            System.out.println("\n========= 区块信息 =========");
            System.out.println("区块号: " + block.getNumber());
            System.out.println("区块哈希: " + block.getHash());
            System.out.println("父区块哈希: " + parextractedHash);
            System.out.println("打包者(Sealer索引): " + sealer); // [输出 Sealer]
            System.out.println("Gas消耗: " + block.getGasUsed());

            long timestamp = block.getTimestamp();
            System.out.println("时间戳: " + timestamp );

            List<BcosBlock.TransactionResult> txs = block.getTransactions();
            String txHash = extractedHash;

            if (txs != null) {
                System.out.println("区块内交易数量: " + txs.size());
                // ... (保留您原有的遍历查找 index 逻辑) ...
            } else {
                System.out.println("区块内交易数量: 0");
            }

            // 获取交易详情
            BcosTransaction txResp = client.getTransaction(txHash, false);
            Optional<JsonTransactionResponse> txOptional = txResp.getTransaction();

            if (txOptional.isPresent()) {
                JsonTransactionResponse tx = txOptional.get();

                // [新增] 获取交易回执 (Transaction Receipt) 以提取 Status
                // 只有回执中才包含交易最终是成功还是失败
                BcosTransactionReceipt receiptResp = client.getTransactionReceipt(txHash,false);
                String status = "Unknown";
                if (receiptResp.getTransactionReceipt().isStatusOK()) {
                    // getStatus() 返回 int，0 表示成功，非 0 表示失败
                    status = String.valueOf(receiptResp.getTransactionReceipt().getStatus());
                }

                System.out.println("\n--- 交易详情 & 指令信息 ---");
                System.out.println("from: " + tx.getFrom());
                System.out.println("to: " + tx.getTo());
                System.out.println("nonce: " + tx.getNonce());
                System.out.println("gasLimit: " + tx.getGasLimit());
                System.out.println("gasPrice: " + tx.getGasPrice());

                // [输出 Status] (交易信息补充)
                System.out.println("status: " + status + " (0=成功, 其他=失败)");

                // [输出 Input] (指令信息核心)
                // 结合 ABI 解码此字段，即可知道调用了什么函数
                System.out.println("input: " + tx.getInput());

            } else {
                System.out.println("未能查到该交易详细信息。");
            }
        }
        // 建议在程序结束时停止 SDK
        // sdk.stopAll();
    }
}