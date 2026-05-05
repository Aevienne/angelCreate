package me.angelique.angelCreate.models;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Product {

    private UUID id;
    private UUID companyId;
    private String displayName;
    private List<String> lore;
    private Material baseItem;
    private List<Ingredient> ingredients;
    private List<EffectModule> effects;
    private long createdAt;

    public Product(UUID id, UUID companyId, String displayName, Material baseItem) {
        this.id = id;
        this.companyId = companyId;
        this.displayName = displayName;
        this.baseItem = baseItem;
        this.lore = new ArrayList<>();
        this.ingredients = new ArrayList<>();
        this.effects = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public List<String> getLore() { return lore; }
    public void setLore(List<String> lore) { this.lore = lore; }
    public Material getBaseItem() { return baseItem; }
    public void setBaseItem(Material baseItem) { this.baseItem = baseItem; }
    public List<Ingredient> getIngredients() { return ingredients; }
    public void setIngredients(List<Ingredient> ingredients) { this.ingredients = ingredients; }
    public List<EffectModule> getEffects() { return effects; }
    public void setEffects(List<EffectModule> effects) { this.effects = effects; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setId(UUID id) { this.id = id; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
}
