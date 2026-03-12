package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class DBUtil {
    // 数据库文件路径
    private static final String DB_URL = "jdbc:sqlite:blockchain_data.db";

    /**
     * 初始化数据库：建表包含所有 12 个字段 + AI 分析字段
     */
    public static void initDB() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println(" 找不到 SQLite 驱动，请检查 pom.xml");
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            // ⚠️ 注意：为了防止表结构冲突，如果之前的表字段不够，建议先删除旧表
            // 如果您不介意数据丢失，可以取消下面这行的注释：
            // stmt.execute("DROP TABLE IF EXISTS transactions");

            String sql = "CREATE TABLE IF NOT EXISTS transactions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "tradehash TEXT," +       // 1. 交易哈希
                    "blocknumber INTEGER," +  // 2. 区块高度
                    "sealer TEXT," +          // 3. 打包节点 (新加)
                    "from_addr TEXT," +       // 4. 发送者
                    "to_addr TEXT," +         // 5. 接收者
                    "timestamp INTEGER," +    // 6. 时间戳
                    "status TEXT," +          // 7. 状态
                    "input_data TEXT," +      // 8. 输入数据
                    "block_trade_count INTEGER," + // 9. 区块交易量 (新加)
                    "gas_limit TEXT," +    // 10. Gas Limit (新加)
                    "gas_used INTEGER," +     // 11. Gas Used
                    "nonce TEXT," +        // 12. Nonce (新加)
                    "is_anomaly INTEGER DEFAULT 0," +
                    "risk_score REAL DEFAULT 0" +
                    ");";

            stmt.execute(sql);
            String semanticSql = "CREATE TABLE IF NOT EXISTS semantic_calls (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "tradehash TEXT," +
                    "blocknumber INTEGER," +
                    "contract_addr TEXT," +
                    "from_addr TEXT," +
                    "call_type TEXT," +
                    "method_id TEXT," +
                    "method_name TEXT," +
                    "is_sensitive INTEGER DEFAULT 0," +
                    "decode_success INTEGER DEFAULT 0," +
                    "arg_summary TEXT," +
                    "created_at INTEGER" +
                    ");";
            stmt.execute(semanticSql);

            String networkSql = "CREATE TABLE IF NOT EXISTS network_metrics (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "blocknumber INTEGER," +
                    "rpc_latency_ms INTEGER," +
                    "block_interval_ms INTEGER," +
                    "tx_count INTEGER," +
                    "fail_rate REAL," +
                    "created_at INTEGER" +
                    ");";
            stmt.execute(networkSql);
            System.out.println("数据库初始化成功 (12 个核心字段)");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存交易：包含所有 12 个参数
     */
    public static void saveTransaction(String tradehash, long blockNumber, String sealer,
                                       String from, String to, long timestamp, String status,
                                       String input, int blockTradeCount, String gasLimit,
                                       long gasUsed, String  nonce) {

        String sql = "INSERT INTO transactions(" +
                "tradehash, blocknumber, sealer, from_addr, to_addr, timestamp, status, input_data, " +
                "block_trade_count, gas_limit, gas_used, nonce) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tradehash);
            pstmt.setLong(2, blockNumber);
            pstmt.setString(3, sealer);
            pstmt.setString(4, from);
            pstmt.setString(5, to);
            pstmt.setLong(6, timestamp);
            pstmt.setString(7, status);
            pstmt.setString(8, input);
            pstmt.setInt(9, blockTradeCount);
            pstmt.setString(10, gasLimit);
            pstmt.setLong(11, gasUsed);
            pstmt.setString(12, nonce);

            pstmt.executeUpdate();

        } catch (Exception e) {
            System.err.println("入库失败: " + e.getMessage());
        }
    }

    public static void saveSemanticCall(String tradehash, long blockNumber, String contractAddr,
                                        String fromAddr, InstructionAnalyzer.DecodeResult result) {
        if (result == null) return;
        String sql = "INSERT INTO semantic_calls(" +
                "tradehash, blocknumber, contract_addr, from_addr, call_type, method_id, method_name, " +
                "is_sensitive, decode_success, arg_summary, created_at) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tradehash);
            pstmt.setLong(2, blockNumber);
            pstmt.setString(3, contractAddr);
            pstmt.setString(4, fromAddr);
            pstmt.setString(5, result.callType);
            pstmt.setString(6, result.methodId);
            pstmt.setString(7, result.methodName);
            pstmt.setInt(8, result.isSensitive);
            pstmt.setInt(9, result.decodeSuccess);
            pstmt.setString(10, result.argSummary);
            pstmt.setLong(11, System.currentTimeMillis());
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("语义入库失败: " + e.getMessage());
        }
    }

    public static void saveNetworkMetric(long blockNumber, long rpcLatencyMs,
                                         long blockIntervalMs, int txCount, double failRate) {
        String sql = "INSERT INTO network_metrics(" +
                "blocknumber, rpc_latency_ms, block_interval_ms, tx_count, fail_rate, created_at) " +
                "VALUES(?,?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, blockNumber);
            pstmt.setLong(2, rpcLatencyMs);
            pstmt.setLong(3, blockIntervalMs);
            pstmt.setInt(4, txCount);
            pstmt.setDouble(5, failRate);
            pstmt.setLong(6, System.currentTimeMillis());
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("网络指标入库失败: " + e.getMessage());
        }
    }
}