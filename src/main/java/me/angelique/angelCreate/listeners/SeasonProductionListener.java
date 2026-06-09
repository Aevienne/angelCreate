package me.angelique.angelCreate.listeners;

import me.angelique.angelCreate.factory.FactoryBlock;
import me.angelique.angelNCore.events.SeasonChangedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SeasonProductionListener implements Listener {

    @EventHandler
    public void onSeasonChanged(SeasonChangedEvent event) {
        double multiplier = switch (event.getNewSeason()) {
            case SPRING -> 1.15;
            case SUMMER -> 1.00;
            case AUTUMN -> 0.90;
            case WINTER -> 0.75;
        };
        FactoryBlock.setSeasonMultiplier(multiplier);
    }
}
