package com.oresomecraft.playercount;

import com.oresomecraft.playercount.database.MySQL;
import org.bukkit.*;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PlayerCount extends JavaPlugin implements Listener {

    public MySQL mysql;
    public String mysql_host;
    public String mysql_port;
    public String mysql_db;
    public String mysql_user;
    public String mysql_password;

    private static final Object LOCK = new Object();

    Scoreboard board;
    Objective objective;

    volatile Score smp;
    volatile Score battles;
    volatile Score arcade;
    volatile Score hub;

    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
        setupDatabase();

        board = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = board.registerNewObjective("playercount", "dummy");
        objective.setDisplayName(ChatColor.DARK_AQUA + "Online players");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Servers, hard coded because why not
        smp = objective.getScore(Bukkit.getOfflinePlayer(ChatColor.AQUA + "SMP"));
        battles = objective.getScore(Bukkit.getOfflinePlayer(ChatColor.AQUA + "Battles"));
        arcade = objective.getScore(Bukkit.getOfflinePlayer(ChatColor.AQUA + "Arcade"));
        hub = objective.getScore(Bukkit.getOfflinePlayer(ChatColor.AQUA + "Hub"));

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            public void run() {
                refreshBoard();
            }
        }, 100L, 100L);
    }

    public void onDisable() {

    }

    private synchronized void refreshBoard() {
        synchronized (LOCK) {
            try {
                mysql.open();

                // SMP
                ResultSet smpCount = mysql.query("SELECT COUNT(*) FROM players WHERE server='smp'");
                smpCount.first();
                smp.setScore(smpCount.getInt(1));

                // Battles
                ResultSet battlesCount = mysql.query("SELECT COUNT(*) FROM players WHERE server='battle'");
                battlesCount.first();
                battles.setScore(battlesCount.getInt(1));

                // Arcade
                ResultSet arcadeCount = mysql.query("SELECT COUNT(*) FROM players WHERE server='arcade'");
                arcadeCount.first();
                arcade.setScore(arcadeCount.getInt(1));

                // Hub
                ResultSet hubCount = mysql.query("SELECT COUNT(*) FROM players WHERE server='hub'");
                hubCount.first();
                hub.setScore(hubCount.getInt(1));

            } catch (SQLException ex) {
                smp.setScore(0);
                battles.setScore(0);
            } finally {
                mysql.close();
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().setScoreboard(board);
    }

    private void setupDatabase() {
        Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {
            public void run() {

                mysql_host = getConfig().getString("mysql.host");
                mysql_db = getConfig().getString("mysql.database");
                mysql_user = getConfig().getString("mysql.username");
                mysql_password = getConfig().getString("mysql.password");
                mysql_port = getConfig().getString("mysql.port");

                mysql = new MySQL(getLogger(), "[PlayerCount]", mysql_host, mysql_port,
                        mysql_db, mysql_user, mysql_password);

                getLogger().info("Connecting to MySQL database...");
                mysql.open();

                if (mysql.checkConnection()) {
                    getLogger().info("Successfully connected to database!");
                } else {
                    disable();
                }

                mysql.close();
            }
        });
    }

    public void disable() {
        Bukkit.getPluginManager().disablePlugin(this);
    }

}
