package me.angelique.angelCreate.hooks;

import me.angelique.angelCreate.AngelCreate;
import me.angelique.angelNCore.events.TradeCompletedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CaravanHook implements Listener {

    private final AngelCreate plugin;
    private boolean enabled = true;

    public CaravanHook(AngelCreate plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("CaravanHook listening for trade events via angelNCore event bus.");
    }

    @EventHandler
    public void onTradeCompleted(TradeCompletedEvent event) {
        plugin.getLogger().info(
            "CaravanHook: trade from " + event.getSeller() +
            " to " + event.getBuyer() +
            " — " + event.getQuantity() + "x " + event.getItemType() +
            " @ " + String.format("%.2f", event.getPricePerUnit())
        );
    }

    public boolean isEnabled() { return enabled; }
}
