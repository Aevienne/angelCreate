package me.angelique.angelCreate.models;

import me.angelique.angelCreate.models.enums.EffectType;
import me.angelique.angelCreate.models.enums.Trigger;

import java.util.HashMap;
import java.util.Map;

public class EffectModule {

    private Trigger trigger;
    private EffectType type;
    private Map<String, Object> parameters;

    public EffectModule(Trigger trigger, EffectType type, Map<String, Object> parameters) {
        this.trigger = trigger;
        this.type = type;
        this.parameters = parameters != null ? parameters : new HashMap<>();
    }

    public EffectModule() {
        this.parameters = new HashMap<>();
    }

    public Trigger getTrigger() { return trigger; }
    public void setTrigger(Trigger trigger) { this.trigger = trigger; }
    public EffectType getType() { return type; }
    public void setType(EffectType type) { this.type = type; }
    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }

    public Object getParam(String key) { return parameters.get(key); }
    public String getParamString(String key, String def) {
        Object v = parameters.get(key);
        return v != null ? v.toString() : def;
    }
    public double getParamDouble(String key, double def) {
        Object v = parameters.get(key);
        if (v == null) return def;
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return def; }
    }
    public int getParamInt(String key, int def) {
        Object v = parameters.get(key);
        if (v == null) return def;
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return def; }
    }
}
