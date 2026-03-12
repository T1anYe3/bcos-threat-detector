package org.example;

import org.fisco.bcos.sdk.v3.BcosSDK;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.client.protocol.response.SealerList;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.lang.reflect.Method;
import java.util.List;

public class NodeInfoCollector {

    // 确保这个路径指向您 Python 项目用的同一个数据库文件
    private static final String DB_URL = "jdbc:sqlite:blockchain_data.db";

    public static void main(String[] args) {
        try {
            System.out.println("🚀 [Java] 正在连接区块链获取共识节点白名单...");

            // 1. 初始化 SDK
            String configFile = NodeInfoCollector.class.getClassLoader().getResource("config.toml").getPath();
            BcosSDK sdk = BcosSDK.build(configFile);
            Client client = sdk.getClient("group0");

            // 2. 调用核心接口并写入数据库
            syncSealersFromClient(client);

            System.exit(0);

        } catch (Exception e) {
            System.err.println("❌ [Java] 获取失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 供其他类（如 RealTimeMonitor）复用：使用现有 Client 同步一次白名单。
     */
    public static void syncSealersFromClient(Client client) {
        try {
            SealerList sealerListResponse = client.getSealerList();
            List<SealerList.Sealer> sealers = sealerListResponse.getSealerList();
            System.out.println("✅ [Java] 链上获取到 " + sealers.size() + " 个共识节点。");
            saveWhitelistToDB(sealers);
        } catch (Exception e) {
            System.err.println("❌ [Java] 同步白名单失败: " + e.getMessage());
        }
    }

    private static void saveWhitelistToDB(List<SealerList.Sealer> sealers) {
        // 建表 (如果不存在)
        String createTableSQL = "CREATE TABLE IF NOT EXISTS nodes (" +
                "node_id TEXT PRIMARY KEY, " +
                "role TEXT, " +
                "update_time LONG" +
                ");";

        // 先清空旧的 Sealer 记录，确保名单是最新的
        String clearSealerSQL = "DELETE FROM nodes WHERE role = 'Sealer'";

        String insertSQL = "INSERT OR REPLACE INTO nodes (node_id, role, update_time) VALUES (?, 'Sealer', ?)";
        long now = System.currentTimeMillis();

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            // 1. 建表
            stmt.execute(createTableSQL);

            // 2. 清理旧白名单
            stmt.execute(clearSealerSQL);

            // 3. 插入新白名单
            try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                for (SealerList.Sealer node : sealers) {
                    String nodeId = extractNodeId(node);
                    if (nodeId == null || nodeId.trim().isEmpty()) {
                        continue;
                    }
                    // 保持 node_id 原始值存库，Python 侧做标准化匹配。
                    pstmt.setString(1, nodeId);
                    pstmt.setLong(2, now);
                    pstmt.executeUpdate();
                    System.out.println("   -> 已录入白名单: " + nodeId + "...");
                }
            }
            System.out.println("💾 [Java] 白名单已同步至数据库。");

        } catch (Exception e) {
            System.err.println("数据库操作失败: " + e.getMessage());
        }
    }

    private static String extractNodeId(SealerList.Sealer node) {
        // 兼容不同 SDK 版本：优先调用 getNodeID()/getNodeId()，再回退 toString。
        try {
            Method m = node.getClass().getMethod("getNodeID");
            Object v = m.invoke(node);
            if (v != null) return String.valueOf(v);
        } catch (Exception ignored) { }
        try {
            Method m = node.getClass().getMethod("getNodeId");
            Object v = m.invoke(node);
            if (v != null) return String.valueOf(v);
        } catch (Exception ignored) { }
        return String.valueOf(node);
    }
}