package su.nightexpress.excellentcrates.crate.impl;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import su.nightexpress.excellentcrates.CratesPlugin;
import su.nightexpress.excellentcrates.api.event.CrateObtainRewardEvent;
import su.nightexpress.nightcore.util.random.Rnd;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FallingCrate {

    private static final int FALL_HEIGHT = 20;
    private static final int FALL_SPEED = -2;
    private final CratesPlugin plugin;

    private final Player player;
    private final Crate crate;
    private final Location placeLocation;
    private Entity falling;
    private Entity vex;

    public FallingCrate(CratesPlugin plugin, Player player, Crate crate, Location placeLocation) {
        this.plugin = plugin;
        this.player = player;
        this.crate = crate;
        this.placeLocation = placeLocation;

        plugin.runTask(task -> {
            Location spawnAt = placeLocation.clone();
            spawnAt.add(0.5, FALL_HEIGHT, 0.5);
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
            });

            vex.setGravity(true);

            falling = spawnAt.getWorld().spawnFallingBlock(spawnAt, Material.BARREL.createBlockData());
            vex.addPassenger(falling);
            falling.setPersistent(false);
//            falling.getPersistentDataContainer().set(FALLING_BLOCK_KEY, PersistentDataType.BYTE, (byte) 0);
            vex.setVelocity(new Vector(0, FALL_SPEED, 0));
        });
    }

    public Entity getFalling() {
        return falling;
    }

    public Entity getVex() {
        return vex;
    }

    public Location getPlaceLocation() {
        return placeLocation;
    }

    public void land() {
        falling.remove();
        vex.remove();

        placeLocation.getBlock().setType(Material.BARREL);
        Barrel barrel = (Barrel) placeLocation.getBlock().getState();

        int rewardAmount = Rnd.get(crate.getMinRewardAmount(), crate.getMaxRewardAmount());
        List<Integer> possibleSlots = IntStream.range(0, barrel.getInventory().getContents().length)
                .boxed()
                .collect(Collectors.toList());

        for (int i = 0; i < rewardAmount; i++) {
            int next = Rnd.get(possibleSlots);
            possibleSlots.remove(next);

            Reward reward = crate.rollReward(player);
            ItemStack item = reward.getContent(player);
            barrel.getInventory().setItem(next, item);

            CrateObtainRewardEvent rewardEvent = new CrateObtainRewardEvent(reward, player);
            plugin.getPluginManager().callEvent(rewardEvent);
        }
    }
}
