package dev.danielp.harvestseason;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

public class BlockBreakEventHandler implements Listener {
    
    private final Main plugin = Main.getPlugin(Main.class);;
    private final Random random = new Random();
    
    private final Set<Material> CROP_MATERIALS = Set.of(
        Material.WHEAT, 
        Material.CARROTS, 
        Material.POTATOES, 
        Material.BEETROOTS,
        Material.NETHER_WART
    );
    
    private final Set<Material> HOE_MATERIALS = Set.of(
        Material.WOODEN_HOE,
        Material.STONE_HOE,
        Material.IRON_HOE,
        Material.GOLDEN_HOE,
        Material.DIAMOND_HOE,
        Material.NETHERITE_HOE
    );
    

    
    @EventHandler
    public void blockBreakEvent(BlockBreakEvent e) {
        Player player = e.getPlayer();
        Block brokenBlock = e.getBlock();
        Material brokenMaterial = brokenBlock.getType();
        
        // Only apply in survival mode
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }
        
        // Check if the broken block is a crop
        if (!CROP_MATERIALS.contains(brokenMaterial)) {
            return;
        }
        
        // Get the item in the player's hand
        ItemStack handItem = player.getInventory().getItemInMainHand();
        
        // Check if the player is using a hoe
        if (!HOE_MATERIALS.contains(handItem.getType())) {
            return;
        }
        
        // Check if the crop is fully grown
        if (brokenBlock.getBlockData() instanceof Ageable) {
            Ageable age = (Ageable) brokenBlock.getBlockData();
            if (age.getAge() != age.getMaximumAge()) {
                return; // Crop is not fully grown, don't harvest the field
            }
        }
        
        // Ensure the player has permission to harvest
        if (!player.hasPermission("harvestseason.harvest")) {
            return;
        }
        
        // Get player's harvest mode
        HarvestMode mode = plugin.getPlayerManager().getPlayerMode(player.getUniqueId());
        
        // Cancel the original event - we'll handle the drops ourselves
        e.setCancelled(true);
        
        // Harvest connected crops
        harvestConnectedCrops(brokenBlock, brokenMaterial, player, handItem, mode);
    }
    
    private void harvestConnectedCrops(Block startBlock, Material cropType, Player player, ItemStack tool, HarvestMode mode) {
        Queue<Block> queue = new LinkedList<>();
        Set<Block> visited = new HashSet<>();
        
        // Add the start block
        queue.add(startBlock);
        visited.add(startBlock);
        
        int maxBlocks = plugin.getConfig().getInt("max-harvest-blocks", 100);
        int blocksHarvested = 0;
        int fortuneLevel = tool.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS);
        
        while (!queue.isEmpty() && blocksHarvested < maxBlocks) {
            Block current = queue.poll();
            
            // Skip blocks already processed or not matching crop type
            if (current.getType() != cropType) {
                continue;
            }
            
            // Check if the crop is fully grown
            if (current.getBlockData() instanceof Ageable) {
                Ageable age = (Ageable) current.getBlockData();
                if (age.getAge() != age.getMaximumAge()) {
                    continue; // Skip crops that are not fully grown
                }
            }
            
            // Harvest the crop with fortune
            Collection<ItemStack> drops = harvestWithFortune(current, fortuneLevel);
            
            // Handle auto-replanting if enabled
            if (mode == HarvestMode.AUTO_REPLANT) {
                replantCrop(current, cropType, drops);
            } else {
                // Just break the block if not replanting
                current.setType(Material.AIR);
            }
            
            // Drop items to the world
            for (ItemStack drop : drops) {
                if (drop != null && drop.getAmount() > 0) {
                    current.getWorld().dropItemNaturally(current.getLocation(), drop);
                }
            }
            
            blocksHarvested++;
            
            // Damage the tool
            if (!damageTool(tool, player)) {
                // Tool broke, stop harvesting
                break;
            }
            
            // Add orthogonal neighbors to queue
            addOrthogonalNeighbors(current, cropType, queue, visited);
        }
        
        if (blocksHarvested > 0) {
            player.sendMessage("§aHarvested " + blocksHarvested + " crops in " + mode.getDisplayName() + " mode.");
        }
    }
    
    private Collection<ItemStack> harvestWithFortune(Block block, int fortuneLevel) {
        Collection<ItemStack> drops = block.getDrops();
        
        // Apply fortune to crops that support it
        if (fortuneLevel > 0) {
            // For each drop, apply fortune chance
            for (ItemStack drop : drops) {
                Material dropType = drop.getType();
                
                // Seeds typically don't get fortune bonus, only the produce does
                if (dropType == Material.WHEAT || 
                    dropType == Material.CARROT || 
                    dropType == Material.POTATO || 
                    dropType == Material.BEETROOT || 
                    dropType == Material.NETHER_WART) {
                    
                    // Apply fortune: Each level has a 1/(level+2) chance for an extra drop
                    // This is simplified, but captures the concept
                    int extraDrops = 0;
                    for (int i = 0; i < fortuneLevel; i++) {
                        if (random.nextInt(fortuneLevel + 2) == 0) {
                            extraDrops++;
                        }
                    }
                    
                    drop.setAmount(drop.getAmount() + extraDrops);
                }
            }
        }
        
        return drops;
    }
    
    private void replantCrop(Block block, Material cropType, Collection<ItemStack> drops) {
        Material seedType = getSeedTypeForCrop(cropType);
        
        // Find and remove one seed from drops
        if (seedType != null) {
            for (ItemStack drop : drops) {
                if (drop.getType() == seedType && drop.getAmount() > 0) {
                    // Remove one seed
                    drop.setAmount(drop.getAmount() - 1);
                    
                    // Replant the crop (set to age 0)
                    block.setType(cropType);
                    if (block.getBlockData() instanceof Ageable) {
                        Ageable ageable = (Ageable) block.getBlockData();
                        ageable.setAge(0);
                        block.setBlockData(ageable);
                    }
                    
                    break;
                }
            }
        }
    }
    
    private Material getSeedTypeForCrop(Material cropType) {
        return switch (cropType) {
            case WHEAT -> Material.WHEAT_SEEDS;
            case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO;
            case BEETROOTS -> Material.BEETROOT_SEEDS;
            case NETHER_WART -> Material.NETHER_WART;
            default -> null;
        };
    }
    
    private boolean damageTool(ItemStack tool, Player player) {
        if (tool == null || tool.getType().isAir()) {
            return false;
        }
        
        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return true;  // Not damageable, continue harvesting
        }
        
        // Check for unbreaking enchantment
        int unbreakingLevel = tool.getEnchantmentLevel(Enchantment.DURABILITY);
        
        // Apply unbreaking chance
        // Formula: 100/(unbreakingLevel+1)% chance to damage the tool
        if (unbreakingLevel > 0 && random.nextInt(unbreakingLevel + 1) > 0) {
            return true;  // Unbreaking prevented damage
        }
        
        int damage = damageable.getDamage() + 1;
        
        // Check if tool will break
        if (damage >= tool.getType().getMaxDurability()) {
            // Break the tool
            player.getInventory().setItemInMainHand(null);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            return false;
        } else {
            // Apply damage
            damageable.setDamage(damage);
            tool.setItemMeta(meta);
            return true;
        }
    }
    
    private void addOrthogonalNeighbors(Block block, Material cropType, Queue<Block> queue, Set<Block> visited) {
        // Check the four adjacent blocks (North, East, South, West)
        Block north = block.getRelative(0, 0, -1);
        Block east = block.getRelative(1, 0, 0);
        Block south = block.getRelative(0, 0, 1);
        Block west = block.getRelative(-1, 0, 0);
        
        for (Block adjacent : new Block[]{north, east, south, west}) {
            if (!visited.contains(adjacent) && adjacent.getType() == cropType) {
                queue.add(adjacent);
                visited.add(adjacent);
            }
        }
    }
}