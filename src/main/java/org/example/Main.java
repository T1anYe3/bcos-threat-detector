package org.example;

import org.fisco.bcos.sdk.v3.BcosSDK;
import org.fisco.bcos.sdk.v3.client.Client;

public class Main {
    public static void main(String[] args) {
        // 1) 读取项目根目录下的 config.toml
        BcosSDK sdk = BcosSDK.build("config.toml");
        try {
            // 2) 获取 group 客户端（air 部署默认是 group0）
            Client client = sdk.getClient("group0");

            // 3) 打印最新块号
            long latest = client.getBlockNumber().getBlockNumber().longValue();
            System.out.println("✅ 链已连接成功，最新区块号 = " + latest);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 4) 关闭 SDK
            sdk.stopAll();
        }
    }
}
