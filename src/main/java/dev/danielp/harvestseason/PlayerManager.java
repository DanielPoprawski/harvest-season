package dev.danielp.harvestseason;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerManager {
    private final Main plugin;
    private final Map<UUID, HarvestMode> playerModes = new HashMap<>();
    private File playerDataFile;
    private FileConfiguration playerData;
    
    public PlayerManager(Main plugin) {
        this.plugin = plugin;
        setupPlayerData();
        loadPlayerData();
    }
    
    private void setupPlayerData() {
        playerDataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.getParentFile().mkdirs();
                playerDataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create playerdata.yml!");
                e.printStackTrace();
            }
        }
        
        playerData = YamlConfiguration.loadConfiguration(playerDataFile);
    }
    
    private void loadPlayerData() {
        if (playerData.contains("players")) {
            for (String uuidString : playerData.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    String modeName = playerData.getString("players." + uuidString + ".mode");
                    try {
                        HarvestMode mode = HarvestMode.valueOf(modeName);
                        playerModes.put(uuid, mode);
                    } catch (IllegalArgumentException e) {
                        // Invalid mode name, use default
                        playerModes.put(uuid, HarvestMode.HARVEST_ONLY);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in playerdata.yml: " + uuidString);
                }
            }
        }
    }
    
    public void savePlayerData() {
        for (Map.Entry<UUID, HarvestMode> entry : playerModes.entrySet()) {
            playerData.set("players." + entry.getKey() + ".mode", entry.getValue().name());
        }
        
        try {
            playerData.save(playerDataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save playerdata.yml!");
            e.printStackTrace();
        }
    }
    
    public HarvestMode getPlayerMode(UUID uuid) {
        return playerModes.getOrDefault(uuid, HarvestMode.HARVEST_ONLY);
    }
    
    public void setPlayerMode(UUID uuid, HarvestMode mode) {
        playerModes.put(uuid, mode);
    }
    
    public void cyclePlayerMode(UUID uuid) {
        HarvestMode currentMode = getPlayerMode(uuid);
        HarvestMode nextMode = currentMode.next();
        setPlayerMode(uuid, nextMode);
    }
}