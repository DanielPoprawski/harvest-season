package dev.danielp.harvestseason;

public enum HarvestMode {
    HARVEST_ONLY("Harvest Only", "§eHarvest crops without replanting"),
    AUTO_REPLANT("Auto Replant", "§aAutomatically replant harvested crops");
    
    private final String displayName;
    private final String description;
    
    HarvestMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public HarvestMode next() {
        HarvestMode[] modes = values();
        return modes[(this.ordinal() + 1) % modes.length];
    }
}