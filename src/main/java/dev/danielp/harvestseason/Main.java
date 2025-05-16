package dev.danielp.harvestseason;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

	@Override
	public void onEnable() {
		getLogger().info("Plugin has been enabled!");

	}

	@Override
	public void onDisable() {
		getLogger().info("Plugin has been disabled!");
	}
}
