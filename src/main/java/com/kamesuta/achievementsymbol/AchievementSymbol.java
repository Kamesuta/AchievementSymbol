package com.kamesuta.achievementsymbol;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AchievementSymbol extends JavaPlugin implements Listener {
    /**
     * ロガー
     */
    public static Logger logger;

    /**
     * プラグインのインスタンス
     */
    public static AchievementSymbol instance;

    /**
     * データベース
     */
    public Database database;

    @Override
    public void onEnable() {
        // プラグインのインスタンスの取得
        instance = this;

        // ロガーの取得
        logger = getLogger();

        // コンフィグの読み込み
        saveDefaultConfig();
        Configuration config = getConfig();

        // データベースへの接続情報
        Database.MySQLConnectionInfo connectionInfo = new Database.MySQLConnectionInfo(
                config.getString("database.host"),
                config.getInt("database.port"),
                config.getString("database.database"),
                config.getString("database.username"),
                config.getString("database.password")
        );

        // データベースへの接続
        try {
            database = new Database(connectionInfo);
        } catch (ReflectiveOperationException | SQLException e) {
            logger.log(Level.SEVERE, "データベースへの接続に失敗しました。", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // イベントリスナーの登録
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        if (database != null) {
            try {
                database.close();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "データベースの切断に失敗しました。", e);
            }
        }
    }

    @EventHandler
    public void onAchievement(PlayerAdvancementDoneEvent e) {
        String player = e.getPlayer().getName();
        String advancementName = e.getAdvancement().getKey().getKey();

        Bukkit.broadcastMessage(ChatColor.GREEN + player + " さんが " + advancementName + " の実績を獲得しました。");

        // Update player's achievement data
        try {
            database.insertAchievement(e.getPlayer(), e.getAdvancement());
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "プレイヤーの実績の更新に失敗しました。", ex);
        }
    }
}
