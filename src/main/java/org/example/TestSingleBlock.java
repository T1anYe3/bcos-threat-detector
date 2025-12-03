package org.example;

import org.fisco.bcos.sdk.v3.BcosSDK;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosBlock;
import java.math.BigInteger;
import java.util.List;

public class TestSingleBlock {

    public static void main(String[] args) {
        // --- 我们只关心这一块代码的输出 ---

        // 1. 指定我们要查询的、已知的包含交易的区块号
        final BigInteger TARGET_BLOCK_NUMBER = new BigInteger("105");

        BcosSDK sdk = BcosSDK.build("config.toml");
        Client client = sdk.getClient("group0");

        try {
            System.out.println("正在精确查询区块: " + TARGET_BLOCK_NUMBER + "...");

            // 2. 获取区块信息
            BcosBlock blockResponse = client.getBlockByNumber(TARGET_BLOCK_NUMBER, false, false);
            BcosBlock.Block block = blockResponse.getBlock();

            if (block != null) {
                // 3. 获取该区块的交易列表
                List<BcosBlock.TransactionResult> txs = block.getTransactions();

                // 4. 打印交易列表的大小 (即交易数量)
                if (txs != null) {
                    System.out.println("区块内交易数量: " + txs.size());
                } else {
                    System.out.println("交易列表为null，交易数量: 0");
                }
            } else {
                System.out.println("未能获取到区块 " + TARGET_BLOCK_NUMBER + " 的信息。");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (sdk != null) {
                sdk.stopAll();
            }
        }
    }
}