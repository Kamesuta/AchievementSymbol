package com.kamesuta.achievementsymbol;

import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
     * @param connectionInfo 接続情報
     * @throws ReflectiveOperationException ドライバーのロードに失敗
     * @throws SQLException SQLエラー
     */
    public Database(ConnectionInfo connectionInfo) throws ReflectiveOperationException, SQLException {
        connection = connectionInfo.connect();

        // テーブルの作成
        connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_achievement (" +
                        "player_uuid BINARY(16) NOT NULL," +
                        "achievement VARCHAR(255) NOT NULL," +
                        "PRIMARY KEY (player_uuid, achievement)" +
                        ")"
        ).execute();
    }

    /**
     * データベースへの接続を閉じる
     * @throws SQLException SQLエラー
     */
    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    /**
     * プレイヤーの実績をデータベースに追加する
     * @param player プレイヤー
     * @param achievement 実績
     * @throws SQLException SQLエラー
     */
    public void insertAchievement(Player player, Advancement achievement) throws SQLException {
        // プレイヤーの実績をデータベースに追加
        PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO player_achievement (player_uuid, achievement) VALUES (?, ?)"
        );
        statement.setBytes(0, UUIDtoByte(player.getUniqueId()));
        statement.setString(1, achievement.getKey().getKey());
        statement.executeUpdate();
    }

    /**
     * UUIDをバイト配列に変換する
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
         * @return データベースへの接続
         * @throws ReflectiveOperationException ドライバーのロードに失敗
         * @throws SQLException SQLエラー
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
        public String user;
        public String password;

        public MySQLConnectionInfo(String host, int port, String database, String user, String password) {
            this.host = host;
            this.port = port;
            this.database = database;
            this.user = user;
            this.password = password;
        }

        public Connection connect() throws ReflectiveOperationException, SQLException {
            // クラスの読み込み
            Class.forName("com.mysql.jdbc.Driver").getConstructor().newInstance();

            // データベースへの接続
            return DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port + "/" + database + "?characterEncoding=utf8",
                user,
                password
            );
        }
    }
}
