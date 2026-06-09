package me.angelique.angelCreate.factory;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FactoryBlock {

    public enum Status { IDLE, RUNNING, DAMAGED }

    private final long id;
    private final Location location;
    private final UUID ownerUUID;
    private String companyId;
    private final String recipeId;

    private double health;               // 0.0 – 100.0
    private int fuelBuffer;              // units of fuel material stored
    private final Map<String, Integer> inputStorage = new HashMap<>();
    private final Map<String, Integer> outputStorage = new HashMap<>();
    private Status status;
    private long productionStartMillis;  // 0 = not running

    public FactoryBlock(long id, Location location, UUID ownerUUID, String companyId, String recipeId) {
        this.id = id;
        this.location = location;
        this.ownerUUID = ownerUUID;
        this.companyId = companyId;
        this.recipeId = recipeId;
        this.health = 100.0;
        this.fuelBuffer = 0;
        this.status = Status.IDLE;
        this.productionStartMillis = 0;
    }

    public long getId() { return id; }
    public Location getLocation() { return location; }
    public UUID getOwnerUUID() { return ownerUUID; }
    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getRecipeId() { return recipeId; }

    public double getHealth() { return health; }
    public void setHealth(double health) { this.health = Math.max(0, Math.min(100, health)); }
    public boolean isDamaged() { return health < 50.0; }

    public int getFuelBuffer() { return fuelBuffer; }
    public void setFuelBuffer(int fuelBuffer) { this.fuelBuffer = Math.max(0, fuelBuffer); }

    public Map<String, Integer> getInputStorage() { return inputStorage; }
    public Map<String, Integer> getOutputStorage() { return outputStorage; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public long getProductionStartMillis() { return productionStartMillis; }
    public void setProductionStartMillis(long productionStartMillis) { this.productionStartMillis = productionStartMillis; }

    /** Effective throughput: 1.0 = full, 0.5 = 50% (when health is low). */
    public double getThroughputMultiplier() {
        double healthMult;
        if (health >= 50.0) healthMult = 1.0;
        else healthMult = health / 100.0;
        return healthMult * seasonMultiplier;
    }

    private static double seasonMultiplier = 1.0;

    public static void setSeasonMultiplier(double multiplier) {
        seasonMultiplier = Math.max(0.5, Math.min(1.5, multiplier));
    }

    public static double getSeasonMultiplier() { return seasonMultiplier; }
}
