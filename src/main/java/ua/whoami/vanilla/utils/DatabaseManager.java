package ua.whoami.vanilla.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public class DatabaseManager {
    private JavaPlugin plugin;
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void initializeDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/homes.db");

            String createTable = "CREATE TABLE IF NOT EXISTS player_homes (" +
                    "uuid TEXT NOT NULL, " +
                    "home_name TEXT NOT NULL, " +
                    "world TEXT NOT NULL, " +
                    "x REAL NOT NULL, " +
                    "y REAL NOT NULL, " +
                    "z REAL NOT NULL, " +
                    "yaw REAL NOT NULL, " +
                    "pitch REAL NOT NULL, " +
                    "PRIMARY KEY (uuid, home_name))";

            try (PreparedStatement stmt = connection.prepareStatement(createTable)) {
                stmt.execute();
            }

            plugin.getLogger().info("Database connection established successfully.");
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
        }
    }

    public boolean setHome(UUID playerUuid, String homeName, Location location) {
        String query = "INSERT OR REPLACE INTO player_homes (uuid, home_name, world, x, y, z, yaw, pitch) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, homeName);
            stmt.setString(3, location.getWorld().getName());
            stmt.setDouble(4, location.getX());
            stmt.setDouble(5, location.getY());
            stmt.setDouble(6, location.getZ());
            stmt.setFloat(7, location.getYaw());
            stmt.setFloat(8, location.getPitch());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to set home: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteHome(UUID playerUuid, String homeName) {
        String query = "DELETE FROM player_homes WHERE uuid = ? AND home_name = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, homeName);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to delete home: " + e.getMessage());
            return false;
        }
    }

    public Location getHome(UUID playerUuid, String homeName) {
        String query = "SELECT * FROM player_homes WHERE uuid = ? AND home_name = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, homeName);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String worldName = rs.getString("world");
                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                double z = rs.getDouble("z");
                float yaw = rs.getFloat("yaw");
                float pitch = rs.getFloat("pitch");

                if (Bukkit.getWorld(worldName) != null) {
                    return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get home: " + e.getMessage());
        }
        return null;
    }

    public Map<String, Location> getHomes(UUID playerUuid) {
        Map<String, Location> homes = new HashMap<>();
        String query = "SELECT * FROM player_homes WHERE uuid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String homeName = rs.getString("home_name");
                String worldName = rs.getString("world");
                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                double z = rs.getDouble("z");
                float yaw = rs.getFloat("yaw");
                float pitch = rs.getFloat("pitch");

                if (Bukkit.getWorld(worldName) != null) {
                    homes.put(homeName, new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get homes list: " + e.getMessage());
        }
        return homes;
    }

    public int getHomeCount(UUID playerUuid) {
        String query = "SELECT COUNT(*) FROM player_homes WHERE uuid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to count homes: " + e.getMessage());
        }
        return 0;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to close database connection: " + e.getMessage());
        }
    }
}