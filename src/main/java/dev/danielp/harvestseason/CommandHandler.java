package dev.danielp.harvestseason;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandHandler implements CommandExecutor, TabCompleter {
    
    private final Main plugin;
    
    public CommandHandler(Main plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "help" -> sendHelpMessage(sender);
            case "reload" -> handleReloadCommand(sender);
            case "mode" -> handleModeCommand(sender, args);
            case "info" -> handleInfoCommand(sender);
            default -> {
                sender.sendMessage("§cUnknown command. Use §6/harvestseason help §cfor help.");
                return false;
            }
        }
        
        return true;
    }
    
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§6=== HarvestSeason Help ===");
        sender.sendMessage("§e/harvestseason mode <mode> [player] §7- Set harvest mode");
        sender.sendMessage("§e/harvestseason info §7- Show current mode");
        sender.sendMessage("§e/harvestseason reload §7- Reload configuration");
        sender.sendMessage("§e/harvestseason help §7- Show this help message");
        sender.sendMessage("§7Tip: Sneak and throw your hoe to switch modes!");
    }
    
    private void handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("harvestseason.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return;
        }
        
        plugin.reloadConfig();
        plugin.getPlayerManager().savePlayerData();
        sender.sendMessage("§aHarvestSeason configuration reloaded.");
    }
    
    private void handleModeCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /harvestseason mode <mode> [player]");
            return;
        }
        
        // Parse mode
        String modeName = args[1].toUpperCase();
        HarvestMode mode;
        try {
            mode = HarvestMode.valueOf(modeName);
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cInvalid mode. Available modes:");
            for (HarvestMode availableMode : HarvestMode.values()) {
                sender.sendMessage("§e- " + availableMode.name());
            }
            return;
        }
        
        // Determine target player
        Player targetPlayer;
        if (args.length >= 3) {
            // Setting mode for another player
            if (!sender.hasPermission("harvestseason.admin")) {
                sender.sendMessage("§cYou don't have permission to set mode for other players.");
                return;
            }
            
            targetPlayer = Bukkit.getPlayer(args[2]);
            if (targetPlayer == null) {
                sender.sendMessage("§cPlayer not found: " + args[2]);
                return;
            }
        } else {
            // Setting mode for self
            if (!sender.hasPermission("harvestseason.mode")) {
                sender.sendMessage("§cYou don't have permission to use this command.");
                return;
            }
            
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cThis command can only be used by players.");
                return;
            }
            
            targetPlayer = (Player) sender;
        }
        
        // Set the mode
        plugin.getPlayerManager().setPlayerMode(targetPlayer.getUniqueId(), mode);
        
        // Notify players
        if (sender == targetPlayer) {
            sender.sendMessage("§aYour harvest mode has been set to §6" + mode.getDisplayName() + "§a.");
        } else {
            sender.sendMessage("§aSet harvest mode for §6" + targetPlayer.getName() + 
                    " §ato §6" + mode.getDisplayName() + "§a.");
            targetPlayer.sendMessage("§aYour harvest mode has been set to §6" + mode.getDisplayName() + 
                    " §aby §6" + sender.getName() + "§a.");
        }
    }
    
    private void handleInfoCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return;
        }
        
        if (!player.hasPermission("harvestseason.info")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return;
        }
        
        HarvestMode mode = plugin.getPlayerManager().getPlayerMode(player.getUniqueId());
        player.sendMessage("§aYour current harvest mode: §6" + mode.getDisplayName());
        player.sendMessage("§7" + mode.getDescription());
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterTabCompletions(List.of("help", "mode", "info", "reload"), args[0]);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("mode")) {
            return filterTabCompletions(
                    Arrays.stream(HarvestMode.values())
                            .map(Enum::name)
                            .map(String::toLowerCase)
                            .collect(Collectors.toList()),
                    args[1]
            );
        } else if (args.length == 3 && args[0].equalsIgnoreCase("mode") && sender.hasPermission("harvestseason.admin")) {
            return null;  // Return null for player names (default behavior)
        }
        
        return List.of();
    }
    
    private List<String> filterTabCompletions(List<String> completions, String partial) {
        String partialLower = partial.toLowerCase();
        List<String> filtered = new ArrayList<>();
        
        for (String completion : completions) {
            if (completion.toLowerCase().startsWith(partialLower)) {
                filtered.add(completion);
            }
        }
        
        return filtered;
    }
}