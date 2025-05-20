package dev.danielp.harvestseason;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class ItemDropEventHandler implements Listener {
    
    private final Main plugin;
    
    private final Set<Material> HOE_MATERIALS = Set.of(
        Material.WOODEN_HOE,
        Material.STONE_HOE,
        Material.IRON_HOE,
        Material.GOLDEN_HOE,
        Material.DIAMOND_HOE,
        Material.NETHERITE_HOE
    );
    
    public ItemDropEventHandler(Main plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        
        // Check if player has permission
        if (!player.hasPermission("harvestseason.modeswitch")) {
            return;
        }
        
        // Check if the player is sneaking
        if (!player.isSneaking()) {
            return;
        }
        
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        
        // Check if the dropped item is a hoe
        if (!HOE_MATERIALS.contains(droppedItem.getType())) {
            return;
        }
        
        // Cancel the drop event
        event.setCancelled(true);
        
        // Cycle to the next mode
        plugin.getPlayerManager().cyclePlayerMode(player.getUniqueId());
        HarvestMode newMode = plugin.getPlayerManager().getPlayerMode(player.getUniqueId());
        
        // Show player a title message
        player.sendTitle(
            "§6" + newMode.getDisplayName(),
            newMode.getDescription(),
            10, 40, 10
        );
        
        // Play a sound effect
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }
}