package com.kamesuta.achievementsymbol;

import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.UUID;

/**
 * データベース
 */
public class Database implements AutoCloseable {
    /**
     * データベースへの接続
     */
    private final Connection connection;

    /**
     * データベースへの接続を行う
     *
     * @param connectionInfo 接続情報
     * @throws ReflectiveOperationException ドライバーのロードに失敗
     * @throws SQLException                 SQLエラー
     */
    public Database(ConnectionInfo connectionInfo) throws ReflectiveOperationException, SQLException {
        connection = connectionInfo.connect();

        try (Statement statement = connection.createStatement()) {
            // プレイヤーテーブルの作成
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS player (" +
                            "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                            "player_uuid BINARY(16) NOT NULL UNIQUE," +
                            "player_name VARCHAR(32) NOT NULL" +
                            ")"
            );

            // 実績テーブルの作成
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS achievement (" +
                            "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                            "achievement_key VARCHAR(255) NOT NULL UNIQUE," +
                            "achievement_name VARCHAR(255) NOT NULL" +
                            ")"
            );

            // プレイヤー実績獲得テーブルの作成
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS player_achievement (" +
                            "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                            "player_id INT NOT NULL," +
                            "achievement_id INT NOT NULL," +
                            "timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "FOREIGN KEY (player_id) REFERENCES player(id)," +
                            "FOREIGN KEY (achievement_id) REFERENCES achievement(id)" +
                            ")"
            );
        }
    }

    /**
     * データベースへの接続を閉じる
     *
     * @throws SQLException SQLエラー
     */
    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    /**
     * プレイヤーの実績をデータベースに追加する
     *
     * @param player      プレイヤー
     * @param achievement 実績
     * @throws SQLException SQLエラー
     */
    public void insertAchievement(Player player, Advancement achievement) throws SQLException {
        byte[] playerUuid = UUIDtoByte(player.getUniqueId());
        String achievementKey = achievement.getKey().getKey();

        // プレイヤーを登録
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT IGNORE INTO player (player_uuid, player_name) VALUES (?, ?)"
        )) {
            statement.setBytes(1, playerUuid);
            statement.setString(2, player.getName());
            statement.executeUpdate();
        }

        // 実績を登録
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT IGNORE INTO achievement (achievement_key, achievement_name) VALUES (?, ?)"
        )) {
            statement.setString(1, achievementKey);
            // TODO: 実績名の取得
            statement.setString(2, "実績(" + achievementKey + ")");
            statement.executeUpdate();
        }

        // プレイヤーの実績をデータベースに追加
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO player_achievement (player_id, achievement_id) VALUES (" +
                        "(SELECT id FROM player WHERE player_uuid = ?)," +
                        "(SELECT id FROM achievement WHERE achievement_key = ?)" +
                        ")"
        )) {
            statement.setBytes(1, playerUuid);
            statement.setString(2, achievementKey);
            statement.executeUpdate();
        }
    }

    /**
     * UUIDをバイト配列に変換する
     *
     * @param uuid UUID
     * @return バイト配列
     */
    public static byte[] UUIDtoByte(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    /**
     * バイト配列をUUIDに変換する
     *
     * @param bytes バイト配列
     * @return UUID
     */
    public static UUID ByteToUUID(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    /**
     * データベースへの接続情報
     */
    public interface ConnectionInfo {
        /**
         * データベースへの接続を行う
         *
         * @return データベースへの接続
         * @throws ReflectiveOperationException ドライバーのロードに失敗
         * @throws SQLException                 SQLエラー
         */
        Connection connect() throws ReflectiveOperationException, SQLException;
    }

    /**
     * MySQLへの接続情報
     */
    public static class MySQLConnectionInfo implements ConnectionInfo {
        public String host;
        public int port;
        public String database;
        public String username;
        public String password;

        public MySQLConnectionInfo(String host, int port, String database, String username, String password) {
            this.host = host;
            this.port = port;
            this.database = database;
            this.username = username;
            this.password = password;
        }

        public Connection connect() throws ReflectiveOperationException, SQLException {
            // クラスの読み込み
            Class.forName("com.mysql.jdbc.Driver").getConstructor().newInstance();

            // データベースへの接続
            return DriverManager.getConnection(
                    "jdbc:mysql://" + host + ":" + port + "/" + database + "?characterEncoding=utf8",
                    username,
                    password
            );
        }
    }
}
