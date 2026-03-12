package org.example;

import org.fisco.bcos.sdk.v3.BcosSDK;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.client.protocol.model.JsonTransactionResponse;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosBlock;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosBlock.Block;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosTransactionReceipt;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

public class RealTimeMonitor {

    private static Client client;
    private static long lastBlockTimestamp = -1L;

    public static void main(String[] args) {
        try {
            DBUtil.initDB();

            // ⚠️ 确认您的配置文件路径
            String configFile = "src/main/resources/config.toml";
            BcosSDK sdk = BcosSDK.build(configFile);
            client = sdk.getClient("group0");

            System.out.println("监控启动成功！连接节点: " + client.getGroup());
            // 启动时同步链上共识节点白名单，供 Python 检测端动态使用。
            NodeInfoCollector.syncSealersFromClient(client);

            // 获取最新块高，回退 100 个块开始追赶
            BigInteger latestChainBlock = client.getBlockNumber().getBlockNumber();
            BigInteger currentProcessBlock = latestChainBlock.subtract(BigInteger.valueOf(100));
            if (currentProcessBlock.compareTo(BigInteger.ZERO) < 0) currentProcessBlock = BigInteger.ZERO;

            System.out.println("起始同步区块: " + currentProcessBlock);

            while (true) {
                latestChainBlock = client.getBlockNumber().getBlockNumber();

                // 追赶模式：如果当前处理块 < 最新块，则持续处理
                if (currentProcessBlock.compareTo(latestChainBlock) <= 0) {
                    processSingleBlock(currentProcessBlock);
                    currentProcessBlock = currentProcessBlock.add(BigInteger.ONE);
                } else {
                    // 监听模式：如果追上了，就休息等待
                    System.out.print(".");
                    Thread.sleep(2000);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processSingleBlock(BigInteger blockNumber) {
        try {
            long fetchStart = System.currentTimeMillis();
            // 参数3: true 尝试获取完整交易，但我们会在下面做兼容处理
            BcosBlock bcosBlock = client.getBlockByNumber(blockNumber, false, true);
            long rpcLatencyMs = System.currentTimeMillis() - fetchStart;

            Block block = bcosBlock.getBlock();
            if (block == null) {
                System.out.println("⚠️ 区块为空: " + blockNumber);
                return;
            }

            List<BcosBlock.TransactionResult> transactions = block.getTransactions();
            int txCount = (transactions == null) ? 0 : transactions.size();

            if (txCount > 0) {
                System.out.println("\n处理区块 [" + blockNumber + "] 含交易数: " + txCount);
            }

            if (transactions == null || transactions.isEmpty()) {
                return;
            }

            // 获取打包者
            String sealer = "0x00";
            if (block.getSealerList() != null && !block.getSealerList().isEmpty()) {
                sealer = block.getSealerList().get(0);
            }
            long timestamp = block.getTimestamp();
            long blockIntervalMs = (lastBlockTimestamp <= 0) ? 0 : Math.max(0, timestamp - lastBlockTimestamp);
            lastBlockTimestamp = timestamp;
            int failedTxCount = 0;

            // === 遍历交易===
            for (BcosBlock.TransactionResult result : transactions) {
                try {
                    JsonTransactionResponse tx = null;

                    //判断 result 到底是 "完整对象" 还是 "哈希壳子"
                    if (result instanceof BcosBlock.TransactionObject) {
                        // 情况A: 拿到了完整对象 (最理想)
                        tx = ((BcosBlock.TransactionObject) result).get();
                    } else if (result instanceof BcosBlock.TransactionHash) {
                        // 情况B: 只拿到了 Hash (报错就是因为进了这里) -> 手动再去查一遍详情
                        String hash = ((BcosBlock.TransactionHash) result).get();
                        // 手动查询交易详情
                        //tx = client.getTransaction(hash, false).getTransaction().orElse(null);
                        Optional<JsonTransactionResponse> txop = client.getTransaction(hash, false).getTransaction();
                        tx = txop.get();
                        System.out.println(tx);
                    }

                    // 如果还没拿到 tx，跳过
                    if (tx == null) continue;

                    // 获取回执 (获取 Status 和 GasUsed)
                    BcosTransactionReceipt receiptResp = client.getTransactionReceipt(tx.getHash(), false);
                    if (receiptResp == null || receiptResp.getTransactionReceipt() == null) continue;

                    TransactionReceipt receipt = receiptResp.getTransactionReceipt();

                    // --- 下面是正常的解析逻辑 ---

                    // 1. Gas Used (消耗量通常不大，保留解析逻辑以便做图表分析)
                    long gasUsedVal = 0;
                    String gasRaw = receipt.getGasUsed();
                    if (gasRaw != null) {
                        String cleanGas = gasRaw.startsWith("0x") ? gasRaw.substring(2) : gasRaw;
                        try { gasUsedVal = Long.parseLong(cleanGas, 16); } catch (Exception e) {}
                    }

                    // 2. Gas Limit (直接取原始字符串)
                    // 如果是 null 就存 "0"
                    System.out.println(tx.getGasLimit());
                    String gasLimitStr = String.valueOf(tx.getGasLimit());

//                    if (gasLimitStr == null) gasLimitStr = "0";

                    // 3. Nonce (直接取原始字符串)
                    // 防止超大数值溢出，直接存文本
                    String nonceStr = tx.getNonce();
                    if (nonceStr == null) nonceStr = "0";

                    // 4. 写入数据库 (调用修改后的 saveTransaction)
                    DBUtil.saveTransaction(
                            tx.getHash(),
                            blockNumber.longValue(),
                            sealer,
                            tx.getFrom(),
                            tx.getTo(),
                            timestamp,
                            String.valueOf(receipt.getStatus()),
                            tx.getInput(),
                            txCount,
                            gasLimitStr,  // 传入 String
                            gasUsedVal,
                            nonceStr      // 传入 String
                    );

                    InstructionAnalyzer.DecodeResult semanticResult =
                            InstructionAnalyzer.analyze(tx.getInput(), tx.getTo());
                    DBUtil.saveSemanticCall(
                            tx.getHash(),
                            blockNumber.longValue(),
                            tx.getTo(),
                            tx.getFrom(),
                            semanticResult
                    );

                    if (!isSuccessStatus(String.valueOf(receipt.getStatus()))) {
                        failedTxCount++;
                    }


                    System.out.println(" [入库成功] Tx: " + tx.getHash().substring(0, 10) + "... | Status: " + receipt.getStatus());

                } catch (Exception e) {
                    System.err.println(" 单笔交易处理失败: " + e.getMessage());
                    // e.printStackTrace(); // 调试时可打开
                }
            }

            double failRate = txCount == 0 ? 0.0 : (double) failedTxCount / (double) txCount;
            DBUtil.saveNetworkMetric(
                    blockNumber.longValue(),
                    rpcLatencyMs,
                    blockIntervalMs,
                    txCount,
                    failRate
            );

        } catch (Exception e) {
            System.err.println("区块处理失败: " + e.getMessage());
        }
    }

    private static boolean isSuccessStatus(String status) {
        if (status == null) return false;
        String s = status.trim().toLowerCase();
        return "0x0".equals(s) || "0".equals(s);
    }
}