package me.angelique.angelCreate.models;

import org.bukkit.Material;

public class Ingredient {
    private Material material;
    private int amount;

    public Ingredient(Material material, int amount) {
        this.material = material;
        this.amount = amount;
    }

    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
}
