package com.spearsmp.managers.storage;

import com.spearsmp.spears.SpearType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * Optional SQLite persistence backend (players.db), selected via
 * {@code storage.type: sqlite} in config.yml.
 *
 * <p>Requires the {@code org.xerial:sqlite-jdbc} library, which is declared
 * under {@code libraries:} in plugin.yml and resolved automatically by
 * Paper's library loader at server startup - no manual jar shading needed.</p>
 *
 * <p>Table schema:</p>
 * <pre>
 * CREATE TABLE crafted (
 *     player_uuid TEXT NOT NULL,
 *     spear_key   TEXT NOT NULL,
 *     PRIMARY KEY (player_uuid, spear_key)
 * );
 * </pre>
 */
public final class SQLitePlayerDataStore implements PlayerDataStore {

    private final JavaPlugin plugin;
    private final File dbFile;
    private Connection connection;

    public SQLitePlayerDataStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), "players.db");
    }

    @Override
    public void load() {
        try {
            plugin.getDataFolder().mkdirs();
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS crafted (
                            player_uuid TEXT NOT NULL,
                            spear_key   TEXT NOT NULL,
                            PRIMARY KEY (player_uuid, spear_key)
                        )
                        """);
            }
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().severe("Failed to initialize SQLite storage: " + e.getMessage()
                    + " - falling back requires setting storage.type back to 'yaml' and restarting.");
        }
    }

    @Override
    public void save() {
        // Each mutation below is committed immediately (auto-commit), so there is
        // nothing to flush here; kept for interface symmetry with the YAML store.
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().warning("Error closing SQLite connection: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean hasCrafted(UUID playerId, SpearType type) {
        if (connection == null) return false;
        String sql = "SELECT 1 FROM crafted WHERE player_uuid = ? AND spear_key = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, type.configKey());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("SQLite read error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void markCrafted(UUID playerId, SpearType type) {
        if (connection == null) return;
        String sql = "INSERT OR IGNORE INTO crafted (player_uuid, spear_key) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, type.configKey());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("SQLite write error: " + e.getMessage());
        }
    }

    @Override
    public void resetCrafted(UUID playerId, SpearType type) {
        if (connection == null) return;
        String sql = "DELETE FROM crafted WHERE player_uuid = ? AND spear_key = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, type.configKey());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("SQLite delete error: " + e.getMessage());
        }
    }

    @Override
    public void resetAllCrafted(UUID playerId) {
        if (connection == null) return;
        String sql = "DELETE FROM crafted WHERE player_uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("SQLite delete-all error: " + e.getMessage());
        }
    }
}
