package com.swapmarket.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class DatabaseMigrationRunner implements ApplicationRunner {

    private final DataSource dataSource;

    private static final String NOTIFICATION_TABLE_DDL =
            "CREATE TABLE IF NOT EXISTS notification (" +
            "  id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "  user_id BIGINT NOT NULL," +
            "  type VARCHAR(32) NOT NULL," +
            "  title VARCHAR(100) NOT NULL," +
            "  content VARCHAR(500) NOT NULL," +
            "  offer_id BIGINT," +
            "  item_id BIGINT," +
            "  read_flag TINYINT DEFAULT 0," +
            "  create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            "  deleted TINYINT DEFAULT 0," +
            "  INDEX idx_notification_user_id(user_id)," +
            "  INDEX idx_notification_read_flag(read_flag)," +
            "  INDEX idx_notification_create_time(create_time)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    private static final String[][] NOTIFICATION_INDEXES = {
            {"idx_notification_user_id", "CREATE INDEX idx_notification_user_id ON notification(user_id)"},
            {"idx_notification_read_flag", "CREATE INDEX idx_notification_read_flag ON notification(read_flag)"},
            {"idx_notification_create_time", "CREATE INDEX idx_notification_create_time ON notification(create_time)"}
    };

    private static final String ITEM_LIKE_TABLE_DDL =
            "CREATE TABLE IF NOT EXISTS item_like (" +
            "  id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "  user_id BIGINT NOT NULL," +
            "  item_id BIGINT NOT NULL," +
            "  create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "  deleted TINYINT DEFAULT 0," +
            "  UNIQUE KEY uk_user_item(user_id, item_id)," +
            "  INDEX idx_user_id(user_id)," +
            "  INDEX idx_item_id(item_id)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    private static final String[][] ITEM_LIKE_INDEXES = {
            {"idx_user_id", "CREATE INDEX idx_user_id ON item_like(user_id)"},
            {"idx_item_id", "CREATE INDEX idx_item_id ON item_like(item_id)"}
    };

    private static final String REPORT_TABLE_DDL =
            "CREATE TABLE IF NOT EXISTS report (" +
            "  id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "  item_id BIGINT NOT NULL," +
            "  user_id BIGINT NOT NULL," +
            "  reason_type VARCHAR(50) NOT NULL," +
            "  description TEXT," +
            "  images TEXT," +
            "  status VARCHAR(20) DEFAULT 'pending'," +
            "  handler_id BIGINT," +
            "  handle_remark VARCHAR(500)," +
            "  create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            "  deleted TINYINT DEFAULT 0," +
            "  INDEX idx_report_item_id(item_id)," +
            "  INDEX idx_report_user_id(user_id)," +
            "  INDEX idx_report_status(status)," +
            "  INDEX idx_report_create_time(create_time)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    private static final String[][] REPORT_INDEXES = {
            {"idx_report_item_id", "CREATE INDEX idx_report_item_id ON report(item_id)"},
            {"idx_report_user_id", "CREATE INDEX idx_report_user_id ON report(user_id)"},
            {"idx_report_status", "CREATE INDEX idx_report_status ON report(status)"},
            {"idx_report_create_time", "CREATE INDEX idx_report_create_time ON report(create_time)"}
    };

    private static final String SWAP_OFFER_STATUS_LOG_TABLE_DDL =
            "CREATE TABLE IF NOT EXISTS swap_offer_status_log (" +
            "  id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "  offer_id BIGINT NOT NULL," +
            "  from_status VARCHAR(20)," +
            "  to_status VARCHAR(20) NOT NULL," +
            "  operator_id BIGINT NOT NULL," +
            "  operator_name VARCHAR(50)," +
            "  remark VARCHAR(500)," +
            "  create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "  INDEX idx_offer_id(offer_id)," +
            "  INDEX idx_create_time(create_time)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    private static final String[][] SWAP_OFFER_STATUS_LOG_INDEXES = {
            {"idx_offer_id", "CREATE INDEX idx_offer_id ON swap_offer_status_log(offer_id)"},
            {"idx_create_time", "CREATE INDEX idx_create_time ON swap_offer_status_log(create_time)"}
    };

    @Override
    public void run(ApplicationArguments args) {
        try {
            migrateNotificationTable();
            migrateItemLikeTable();
            migrateItemLikeCountColumn();
            migrateReportTable();
            migrateSwapOfferStatusLogTable();
        } catch (Exception e) {
            log.error("数据库迁移执行失败，如相关功能异常请检查数据库权限：{}", e.getMessage());
        }
    }

    private void migrateNotificationTable() {
        try (Connection conn = dataSource.getConnection()) {
            String databaseName = getCurrentDatabase(conn);
            if (databaseName == null) {
                log.warn("无法获取当前数据库名，跳过 notification 表迁移");
                return;
            }

            if (!tableExists(conn, databaseName, "notification")) {
                log.info("检测到 notification 表不存在，开始创建...");
                execute(conn, NOTIFICATION_TABLE_DDL);
                log.info("notification 表创建完成");
            } else {
                log.debug("notification 表已存在，跳过建表");
                ensureIndexes(conn, databaseName, "notification", NOTIFICATION_INDEXES);
                ensureColumnDefault(conn, databaseName, "notification", "read_flag", "0");
            }
        } catch (Exception e) {
            log.error("notification 表迁移失败: {}", e.getMessage());
        }
    }

    private String getCurrentDatabase(Connection conn) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DATABASE()")) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private boolean tableExists(Connection conn, String databaseName, String tableName) throws Exception {
        String sql = "SELECT COUNT(*) FROM information_schema.TABLES " +
                     "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, databaseName);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private void ensureIndexes(Connection conn, String databaseName, String tableName,
                               String[][] indexes) throws Exception {
        Set<String> existing = new HashSet<>();
        String sql = "SELECT INDEX_NAME FROM information_schema.STATISTICS " +
                     "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, databaseName);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    existing.add(rs.getString("INDEX_NAME").toLowerCase());
                }
            }
        }

        for (String[] idx : indexes) {
            String idxName = idx[0];
            String ddl = idx[1];
            if (!existing.contains(idxName.toLowerCase())) {
                try {
                    log.info("检测到索引 {} 不存在，开始创建...", idxName);
                    execute(conn, ddl);
                    log.info("索引 {} 创建完成", idxName);
                } catch (Exception e) {
                    log.warn("创建索引 {} 失败（可能已并发创建）：{}", idxName, e.getMessage());
                }
            }
        }
    }

    private void ensureColumnDefault(Connection conn, String databaseName, String tableName,
                                     String columnName, String expectedDefault) {
        String sql = "SELECT COLUMN_DEFAULT FROM information_schema.COLUMNS " +
                     "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, databaseName);
            ps.setString(2, tableName);
            ps.setString(3, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String currentDefault = rs.getString(1);
                    if (currentDefault == null || !expectedDefault.equals(currentDefault)) {
                        String alterSql = String.format(
                                "ALTER TABLE %s ALTER COLUMN %s SET DEFAULT %s",
                                tableName, columnName, expectedDefault);
                        execute(conn, alterSql);
                        log.info("已修正字段 {}.{} 的默认值为 {}", tableName, columnName, expectedDefault);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("检查/修正字段 {}.{} 默认值失败：{}", tableName, columnName, e.getMessage());
        }
    }

    private void execute(Connection conn, String sql) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void migrateItemLikeTable() {
        try (Connection conn = dataSource.getConnection()) {
            String databaseName = getCurrentDatabase(conn);
            if (databaseName == null) {
                log.warn("无法获取当前数据库名，跳过 item_like 表迁移");
                return;
            }

            if (!tableExists(conn, databaseName, "item_like")) {
                log.info("检测到 item_like 表不存在，开始创建...");
                execute(conn, ITEM_LIKE_TABLE_DDL);
                log.info("item_like 表创建完成");
            } else {
                log.debug("item_like 表已存在，跳过建表");
                ensureIndexes(conn, databaseName, "item_like", ITEM_LIKE_INDEXES);
            }
        } catch (Exception e) {
            log.error("item_like 表迁移失败: {}", e.getMessage());
        }
    }

    private void migrateItemLikeCountColumn() {
        try (Connection conn = dataSource.getConnection()) {
            String databaseName = getCurrentDatabase(conn);
            if (databaseName == null) {
                log.warn("无法获取当前数据库名，跳过 item.like_count 字段迁移");
                return;
            }

            if (!tableExists(conn, databaseName, "item")) {
                log.warn("item 表不存在，跳过 like_count 字段迁移");
                return;
            }

            if (!columnExists(conn, databaseName, "item", "like_count")) {
                log.info("检测到 item.like_count 字段不存在，开始添加...");
                String alterSql = "ALTER TABLE item ADD COLUMN like_count INT DEFAULT 0";
                execute(conn, alterSql);
                log.info("item.like_count 字段添加完成");
            } else {
                log.debug("item.like_count 字段已存在，跳过添加");
                ensureColumnDefault(conn, databaseName, "item", "like_count", "0");
            }
        } catch (Exception e) {
            log.error("item.like_count 字段迁移失败: {}", e.getMessage());
        }
    }

    private boolean columnExists(Connection conn, String databaseName, String tableName, String columnName) throws Exception {
        String sql = "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                     "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, databaseName);
            ps.setString(2, tableName);
            ps.setString(3, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private void migrateReportTable() {
        try (Connection conn = dataSource.getConnection()) {
            String databaseName = getCurrentDatabase(conn);
            if (databaseName == null) {
                log.warn("无法获取当前数据库名，跳过 report 表迁移");
                return;
            }

            if (!tableExists(conn, databaseName, "report")) {
                log.info("检测到 report 表不存在，开始创建...");
                execute(conn, REPORT_TABLE_DDL);
                log.info("report 表创建完成");
            } else {
                log.debug("report 表已存在，跳过建表");
                ensureIndexes(conn, databaseName, "report", REPORT_INDEXES);
            }
        } catch (Exception e) {
            log.error("report 表迁移失败: {}", e.getMessage());
        }
    }

    private void migrateSwapOfferStatusLogTable() {
        try (Connection conn = dataSource.getConnection()) {
            String databaseName = getCurrentDatabase(conn);
            if (databaseName == null) {
                log.warn("无法获取当前数据库名，跳过 swap_offer_status_log 表迁移");
                return;
            }

            if (!tableExists(conn, databaseName, "swap_offer_status_log")) {
                log.info("检测到 swap_offer_status_log 表不存在，开始创建...");
                execute(conn, SWAP_OFFER_STATUS_LOG_TABLE_DDL);
                log.info("swap_offer_status_log 表创建完成");
            } else {
                log.debug("swap_offer_status_log 表已存在，跳过建表");
                ensureIndexes(conn, databaseName, "swap_offer_status_log", SWAP_OFFER_STATUS_LOG_INDEXES);
            }
        } catch (Exception e) {
            log.error("swap_offer_status_log 表迁移失败: {}", e.getMessage());
        }
    }
}
