package su.nightexpress.excellentcrates.crate.impl;

import org.bukkit.*;
import org.bukkit.block.Barrel;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import su.nightexpress.excellentcrates.CratesPlugin;
import su.nightexpress.excellentcrates.api.event.CrateObtainRewardEvent;
import su.nightexpress.excellentcrates.util.TextDisplayBuilder;
import su.nightexpress.nightcore.util.random.Rnd;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FallingCrate {

    private static final int FALL_HEIGHT = 20;
    private static final float FALL_SPEED = -0.5f;
    private static final int REMOVE_IN = 5; // Seconds

    private final CratesPlugin plugin;

    private final Player player;
    private final Crate crate;
    private final Location placeLocation;
    private Entity vex;
    private TextDisplay hologram = null;

    private long spawnedAt = Long.MAX_VALUE;

    public FallingCrate(CratesPlugin plugin, Player player, Crate crate, Location placeLocation) {
        this.plugin = plugin;
        this.player = player;
        this.crate = crate;
        this.placeLocation = placeLocation;

        plugin.runTask(task -> {
            Location spawnAt = placeLocation.clone();
            spawnAt.add(0.5, FALL_HEIGHT, 0.5);

            // Vex
            vex = spawnAt.getWorld().spawn(spawnAt, Vex.class, ent -> {
                ent.setInvisible(true);
                ent.setSilent(true);
                ent.setInvulnerable(true);
                ent.setGravity(true);
                ent.setAware(false);
                ent.setPersistent(false);
                if (ent.getEquipment() != null) {
                    ent.getEquipment().clear();
                }
                ent.setGravity(true);
            });

            // Falling block
            Entity falling = spawnAt.getWorld().spawnFallingBlock(spawnAt, Material.BARREL.createBlockData());
            falling.setPersistent(false);
            vex.addPassenger(falling);

            // "Parachute" (Item Display)
            ItemDisplay parachute = spawnAt.getWorld().spawn(spawnAt, ItemDisplay.class, ent -> {
                ItemStack item = new ItemStack(Material.PAPER);
                ItemMeta meta = item.getItemMeta();
                meta.setCustomModelData(90001);
                item.setItemMeta(meta);
                ent.setItemStack(item);

                Transformation transformation = ent.getTransformation();
                transformation.getScale().set(2);
                transformation.getTranslation().set(0, 1.7, 0);
                ent.setTransformation(transformation);

                ent.setPersistent(false);
            });
            vex.addPassenger(parachute);

            vex.setVelocity(new Vector(0, FALL_SPEED, 0));
        });
    }

    public Entity getVex() {
        return vex;
    }

    public Location getPlaceLocation() {
        return placeLocation;
    }

    public void land() {
        removePassengers(vex);
        vex.remove();

        placeLocation.getBlock().setType(Material.BARREL);
        Barrel barrel = (Barrel) placeLocation.getBlock().getState();

        int rewardAmount = Rnd.get(crate.getMinRewardAmount(), crate.getMaxRewardAmount());
        List<Integer> possibleSlots = IntStream.range(0, barrel.getInventory().getContents().length)
                .boxed()
                .collect(Collectors.toList());

        for (int i = 0; i < rewardAmount; i++) {
            int next = Rnd.get(possibleSlots);
            possibleSlots.remove((Object) next);

            Reward reward = crate.rollReward(player);
            ItemStack item = reward.getContent(player);
            barrel.getInventory().setItem(next, item);

            CrateObtainRewardEvent rewardEvent = new CrateObtainRewardEvent(reward, player);
            plugin.getPluginManager().callEvent(rewardEvent);
        }

        spawnedAt = System.currentTimeMillis() + (1000 * REMOVE_IN);

        hologram = new TextDisplayBuilder(placeLocation.clone().add(0.5, 1.2, 0.5))
                .setBillboard(Display.Billboard.CENTER)
                .setText("Removing in " + REMOVE_IN)
                .build();
        hologram.setPersistent(false);

        playSoundNearby(Sound.BLOCK_RESPAWN_ANCHOR_CHARGE);
    }

    private void removePassengers(Entity entity) {
        List<Entity> passengers = new ArrayList<>(entity.getPassengers());
        for (Entity passenger : passengers) {
            removePassengers(passenger);
            passenger.remove();
        }
    }

    public boolean updateTimer() {
        long now = System.currentTimeMillis();
        int remainingSeconds = (int) ((spawnedAt - now) / 1000);

        if (remainingSeconds == 0) {
            remove();
            return false;
        }

        hologram.setText("Removing in " + remainingSeconds);

        if (remainingSeconds <= 3)
            playSoundNearby(Sound.BLOCK_NOTE_BLOCK_BELL);

        return true;
    }

    public void remove() {
        new BukkitRunnable() {
            @Override
            public void run() {
                placeLocation.getBlock().setType(Material.AIR);
                hologram.remove();
                playSoundNearby(Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR);
            }
        }.runTask(plugin);
    }

    public void removeIfEmpty() {
        Inventory inventory = ((Barrel) placeLocation.getBlock().getState()).getInventory();
        if (!inventory.isEmpty())
            return;

        remove();
        plugin.getCrateManager().removeSpawnedCrate(this);
    }

    public void playSoundNearby(Sound sound) {
        for (Player oPlayer : Bukkit.getOnlinePlayers()) {
            if (oPlayer.getWorld() != placeLocation.getWorld())
                continue;

            if (placeLocation.distanceSquared(oPlayer.getLocation()) > 250)
                continue;

            oPlayer.playSound(placeLocation, sound, SoundCategory.BLOCKS, 0.5f, 1f);
        }
    }
}
