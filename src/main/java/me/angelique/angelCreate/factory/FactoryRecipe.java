package me.angelique.angelCreate.factory;

import org.bukkit.Material;

import java.util.List;

public class FactoryRecipe {

    private final String id;
    private final String displayName;
    private final Material blockType;
    private final int tier;
    private final int durationSeconds;
    private final Material fuelMaterial;
    private final int fuelAmount;
    private final List<RecipeStack> inputs;
    private final List<RecipeStack> outputs;

    public FactoryRecipe(String id, String displayName, Material blockType, int tier,
                         int durationSeconds, Material fuelMaterial, int fuelAmount,
                         List<RecipeStack> inputs, List<RecipeStack> outputs) {
        this.id = id;
        this.displayName = displayName;
        this.blockType = blockType;
        this.tier = tier;
        this.durationSeconds = durationSeconds;
        this.fuelMaterial = fuelMaterial;
        this.fuelAmount = fuelAmount;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getBlockType() { return blockType; }
    public int getTier() { return tier; }
    public int getDurationSeconds() { return durationSeconds; }
    public Material getFuelMaterial() { return fuelMaterial; }
    public int getFuelAmount() { return fuelAmount; }
    public List<RecipeStack> getInputs() { return inputs; }
    public List<RecipeStack> getOutputs() { return outputs; }

    public record RecipeStack(Material material, int amount) {}
}
