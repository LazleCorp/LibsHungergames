package me.libraryaddict.Hungergames.Kits;

import me.libraryaddict.Hungergames.Types.Extender;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class Fireman extends Extender implements Listener {

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player
                && kits.hasAbility((Player) event.getEntity(), "Fireman")
                && (event.getCause() == DamageCause.FIRE || event.getCause() == DamageCause.FIRE_TICK
                        || event.getCause() == DamageCause.LIGHTNING || event.getCause() == DamageCause.LAVA)) {
            event.setCancelled(true);
            event.getEntity().setFireTicks(0);
        }
    }

}