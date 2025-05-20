package dev.danielp.harvestseason;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    
    private PlayerManager playerManager;
    
    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();
        
        // Initialize player manager
        playerManager = new PlayerManager(this);
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new BlockBreakEventHandler(), this);
        getServer().getPluginManager().registerEvents(new ItemDropEventHandler(this), this);
        
        // Register commands
        CommandHandler commandHandler = new CommandHandler(this);
        getCommand("harvestseason").setExecutor(commandHandler);
        getCommand("harvestseason").setTabCompleter(commandHandler);
        
        getLogger().info("HarvestSeason has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Save player data
        if (playerManager != null) {
            playerManager.savePlayerData();
        }
        
        getLogger().info("HarvestSeason has been disabled!");
    }
    
    public PlayerManager getPlayerManager() {
        return playerManager;
    }
}