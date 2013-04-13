package me.libraryaddict.Hungergames.Listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import me.libraryaddict.Hungergames.Types.Damage;
import me.libraryaddict.Hungergames.Types.Extender;
import me.libraryaddict.Hungergames.Types.Gamer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.ContainerBlock;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class PlayerListener extends Extender implements Listener {

    public HashMap<Location, EntityType> entitys = new HashMap<Location, EntityType>();
    private List<String> deathMessages = new ArrayList<String>();

    public PlayerListener() {
        deathMessages.add("%Killer% dual wielded a %Weapon% and laid waste upon %Killed%");
        deathMessages.add("%Killer% slid a %Weapon% into %Killed% when he wasn't looking");
        deathMessages.add("%Killed% was murdered in cold blood by %Killer% with a %Weapon%");
        deathMessages.add("%Killed% gasped his last breath as %Killer% savagely stabbed him with a %Weapon%");
        deathMessages.add("%Killed% screamed in agnoy as he was bludgeoned over the head with a %Weapon% by %Killer%");
        deathMessages.add("%Killed% was killed by %Killer% with a %Weapon%");
        deathMessages.add("%Killer% gave %Killed% a helping hand into death's sweet embrace with his trusty %Weapon%");
        deathMessages.add("%Killer%'s %Weapon% could not resist killing %Killed%");
        deathMessages.add("%Killer% and his trusty %Weapon% slew %Killed%");
        deathMessages.add("%Killed%'s weapon could not stand up against %Killer%'s %Weapon% of doom!");
        // deathMessages
        // .add("%Killed% and %Killer% did paper, sissors, rock. %Killed% used paper. %Killer% used %Weapon%. %Killed% was slaughtered");
    }

    @EventHandler
    public void onSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() == EntityType.SLIME)
            event.setCancelled(true);
        if (hg.currentTime < 0) {
            event.setCancelled(true);
            if (event.getEntity() instanceof Animals && event.getSpawnReason() == SpawnReason.CHUNK_GEN
                    && new Random().nextInt(4) == 1)
                entitys.put(event.getLocation().clone(), event.getEntityType());
        } else if (event.getEntity() instanceof Animals && event.getSpawnReason() == SpawnReason.CHUNK_GEN
                && new Random().nextInt(4) != 1)
            event.setCancelled(true);
    }

    @EventHandler
    public void onExpChange(PlayerExpChangeEvent event) {
        if (!pm.getGamer(event.getPlayer()).isAlive())
            event.setAmount(0);
    }

    @EventHandler
    public void onChat(PlayerChatEvent event) {
        Gamer gamer = pm.getGamer(event.getPlayer());
        if (!hg.spectatorChat && !gamer.isAlive() && hg.doSeconds) {
            Iterator<Player> players = event.getRecipients().iterator();
            while (players.hasNext()) {
                Gamer g = pm.getGamer(players.next());
                if (!g.getPlayer().isOp() && g.isAlive())
                    players.remove();
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (kits.getKitByPlayer(event.getPlayer().getName()) == null)
            kits.setKit(event.getPlayer(), kits.defaultKit);
        event.setJoinMessage(null);
        final Gamer gamer = pm.registerGamer(event.getPlayer());
        Player p = gamer.getPlayer();
        p.setAllowFlight(true);
        if (gamer.isVip() && gamer.getPlayer().equals(gamer.getName()))
            gamer.getPlayer().setDisplayName(ChatColor.GREEN + gamer.getName());
        if (hg.currentTime >= 0) {
            pm.setSpectator(gamer);
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(hg, new Runnable() {
                public void run() {
                    gamer.hide(gamer.getPlayer());
                }
            }, 0L);
        }
        pm.sendToSpawn(gamer.getPlayer());
        gamer.updateOthersToSelf();
        gamer.updateSelfToOthers();
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().clear();
        Player p = event.getEntity();
        EntityDamageEvent cause = event.getEntity().getLastDamageCause();
        String deathMessage = event.getDeathMessage();
        if (cause instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent entityEvent = (EntityDamageByEntityEvent) cause;
            if (entityEvent.getDamager() instanceof Player) {
                String weapon = "fist";
                Player dmg = (Player) entityEvent.getDamager();
                ItemStack item = dmg.getInventory().getItemInHand();
                if (item != null && item.getType() != Material.AIR)
                    weapon = kits.toReadable(item.getType().name());
                deathMessage = deathMessages.get(new Random().nextInt(deathMessages.size())).replace("%Killed%", p.getName())
                        .replace("%Killer%", dmg.getName()).replace("%Weapon%", weapon);
            }
        } else if (cause.getCause() == DamageCause.FALL)
            deathMessage = p.getName() + " fell to his death";
        Gamer gamer = pm.getGamer(p);
        pm.killPlayer(gamer, null, p.getLocation(), gamer.getInventory(), deathMessage);
        event.setDeathMessage(null);
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        if (event.getResult() == Result.KICK_FULL && !pm.vips.containsKey(event.getPlayer().getName()))
            event.disallow(Result.KICK_OTHER, "The game is full!");
        else if (hg.currentTime >= 0 && !hg.spectators && !event.getPlayer().hasPermission("Hungergames.Spectate"))
            event.disallow(Result.KICK_OTHER, "Spectators have been disabled!");
        // else if (!(event.getPlayer().isOp() ||
        // pm.vips.containsKey(event.getPlayer().getName())))
        // event.disallow(Result.KICK_OTHER,
        // "Only VIP's may spectate!\nBuy idk at www.blabla.com");
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        event.setLeaveMessage(null);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        Gamer gamer = pm.getGamer(event.getPlayer());
        if (gamer.isAlive() && hg.currentTime >= 0 && pm.getAliveGamers().size() > 1) {
            pm.killPlayer(gamer, null, gamer.getPlayer().getLocation(), gamer.getInventory(), gamer.getName()
                    + " was slaughtered for leaving the game");
        }
        pm.unregisterGamer(gamer);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent event) {
        Gamer gamer = pm.getGamer(event.getPlayer());
        if (!gamer.canInteract())
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent event) {
        if (!pm.getGamer(event.getPlayer()).canInteract()
                || event.getBlock().getLocation().getBlockY() > event.getBlock().getWorld().getMaxHeight())
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(BlockDamageEvent event) {
        if (!pm.getGamer(event.getPlayer()).canInteract())
            event.setCancelled(true);
    }

    @EventHandler
    public void onHungry(FoodLevelChangeEvent event) {
        if (!pm.getGamer(event.getEntity()).isAlive())
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player
                || (event.getEntity() instanceof Tameable && ((Tameable) event.getEntity()).isTamed())) {
            if ((event.getEntity() instanceof Player && (!hg.doSeconds || !pm.getGamer(event.getEntity()).isAlive()))
                    || hg.currentTime <= hg.invincibility)
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && !pm.getGamer(event.getDamager()).canInteract()) {
            event.setCancelled(true);
            return;
        }
        if (event.getEntity() instanceof Player) {
            Gamer damager = null;
            if (event.getDamager() instanceof Player)
                damager = pm.getGamer(event.getDamager());
            else if (event.getDamager() instanceof Projectile)
                damager = pm.getGamer(((Projectile) event.getDamager()).getShooter());
            else if (event.getDamager() instanceof Tameable) {
                if (((Tameable) event.getDamager()).getOwner() != null)
                    damager = pm.getGamer(((Tameable) event.getDamager()).getOwner().getName());
            }
            if (damager != null) {
                if (damager.canInteract())
                    pm.lastDamager.put(pm.getGamer(event.getEntity()), new Damage((System.currentTimeMillis() / 1000) + 60,
                            damager));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        Gamer gamer = pm.getGamer(p);
        if (!gamer.canInteract()) {
            event.setCancelled(true);
            /*
             * if (event.getAction() == Action.RIGHT_CLICK_BLOCK &&
             * event.getClickedBlock().getState() != null &&
             * event.getClickedBlock().getState() instanceof InventoryHolder) {
             * p.openInventory(((InventoryHolder)
             * event.getClickedBlock().getState()).getInventory()); }
             */
        }
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if (gamer.isAlive() && gamer.canRide())
                if (p.isInsideVehicle() == true)
                    p.leaveVehicle();
            if (hg.mushroomStew) {
                ItemStack item = event.getItem();
                if ((p.getHealth() < 20 || p.getFoodLevel() < 19) && item != null && item.getType() == Material.MUSHROOM_SOUP) {
                    event.setCancelled(true);
                    if (p.getHealth() < 20)
                        if (p.getHealth() + hg.mushroomStewRestores <= 20)
                            p.setHealth(p.getHealth() + hg.mushroomStewRestores);
                        else
                            p.setHealth(20);
                    else if (p.getFoodLevel() < 20)
                        if (p.getFoodLevel() + hg.mushroomStewRestores <= 20)
                            p.setFoodLevel(p.getFoodLevel() + hg.mushroomStewRestores);
                        else
                            p.setFoodLevel(20);
                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                        kits.addItem(p, new ItemStack(Material.BOWL));
                    } else
                        item = new ItemStack(Material.BOWL);
                    p.setItemInHand(item);
                }
            }
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Gamer gamer = pm.getGamer(event.getPlayer());
        if (!gamer.canInteract()) {
            event.setCancelled(true);
        }
        if (!gamer.isAlive()) {
            if (event.getRightClicked() instanceof Player) {
                Player victim = (Player) event.getRightClicked();
                Player p = event.getPlayer();
                p.sendMessage(ChatColor.RED + victim.getName() + ChatColor.RED + " has " + victim.getHealth() + "/20 health");
                p.sendMessage(ChatColor.RED + victim.getName() + ChatColor.RED + " has " + victim.getFoodLevel() + "/20 hunger");
            }
            if (gamer.canRide()) {
                if (event.getPlayer().isInsideVehicle() == false && event.getRightClicked().getVehicle() != event.getPlayer())
                    event.getRightClicked().setPassenger(event.getPlayer());
                else
                    event.getPlayer().leaveVehicle();
            }
        }
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        if (!pm.getGamer(event.getPlayer()).isAlive())
            event.setCancelled(true);
    }

    @EventHandler
    public void onPickup(PlayerDropItemEvent event) {
        if (!pm.getGamer(event.getPlayer()).canInteract())
            event.setCancelled(true);
    }

    @EventHandler
    public void onVechileEnter(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player) {
            if (!pm.getGamer(event.getEntered()).canInteract()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onVechileMove(VehicleEntityCollisionEvent event) {
        if (event.getEntity() instanceof Player)
            if (!pm.getGamer(event.getEntity()).canInteract())
                event.setCancelled(true);
    }

    @EventHandler
    public void onVechileEvent(VehicleDestroyEvent event) {
        if (event.getAttacker() instanceof Player) {
            if (!pm.getGamer(event.getAttacker()).canInteract()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!pm.getGamer(event.getWhoClicked()).canInteract()) {
            event.setCancelled(true);
        }
    }
}