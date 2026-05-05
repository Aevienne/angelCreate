package me.angelique.angelCreate.models;

import me.angelique.angelCreate.models.enums.Role;
import org.bukkit.Location;

import java.util.*;

public class Company {

    private UUID id;
    private String name;
    private UUID owner;
    private Map<UUID, Role> members;
    private double treasury;
    private int level;
    private Location workbenchLocation;
    private List<UUID> productIds;
    private List<UUID> patentIds;
    private boolean ipoListed;

    public Company(UUID id, String name, UUID owner) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.members = new LinkedHashMap<>();
        this.members.put(owner, Role.OWNER);
        this.treasury = 0.0;
        this.level = 1;
        this.productIds = new ArrayList<>();
        this.patentIds = new ArrayList<>();
        this.ipoListed = false;
    }

    public boolean hasRole(UUID uuid, Role minimum) {
        Role role = members.get(uuid);
        if (role == null) return false;
        return role.ordinal() <= minimum.ordinal();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getOwner() { return owner; }
    public Map<UUID, Role> getMembers() { return members; }
    public double getTreasury() { return treasury; }
    public void setTreasury(double treasury) { this.treasury = treasury; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public Location getWorkbenchLocation() { return workbenchLocation; }
    public void setWorkbenchLocation(Location workbenchLocation) { this.workbenchLocation = workbenchLocation; }
    public List<UUID> getProductIds() { return productIds; }
    public List<UUID> getPatentIds() { return patentIds; }
    public boolean isIpoListed() { return ipoListed; }
    public void setIpoListed(boolean ipoListed) { this.ipoListed = ipoListed; }
    public void setId(UUID id) { this.id = id; }
}
