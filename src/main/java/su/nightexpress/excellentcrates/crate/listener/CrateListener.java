package su.nightexpress.excellentcrates.crate.listener;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.bukkit.event.block.UseBlockEvent;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.CratesPlugin;
import su.nightexpress.excellentcrates.config.Perms;
import su.nightexpress.excellentcrates.crate.CrateManager;
import su.nightexpress.excellentcrates.crate.impl.Crate;
import su.nightexpress.excellentcrates.crate.impl.FallingCrate;
import su.nightexpress.excellentcrates.util.CrateUtils;
import su.nightexpress.excellentcrates.util.InteractType;
import su.nightexpress.nightcore.dialog.Dialog;
import su.nightexpress.nightcore.manager.AbstractListener;

import java.util.stream.Stream;

public class CrateListener extends AbstractListener<CratesPlugin> {

    private final CrateManager crateManager;

    public CrateListener(@NotNull CratesPlugin plugin, @NotNull CrateManager crateManager) {
        super(plugin);
        this.crateManager = crateManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCrateBlockAssign(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Dialog editor = Dialog.get(player);
        if (editor == null) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);

        if (plugin.getCrateManager().getCrateByBlock(block) != null) return;

        Crate crate = CrateUtils.getAssignBlockCrate(player);
        if (crate == null) return;

        crate.getBlockLocations().add(block.getLocation());
        crate.updateHologram();
        crate.saveSettings();
        Dialog.stop(player);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onWGUseBlock(UseBlockEvent event) {
        for (Block block : event.getBlocks()) {
            if (crateManager.getSpawnedCrate(block.getLocation()) != null) {
                event.setAllowed(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCrateOpen(PlayerInteractEvent event) {
        // Check if trying to open a spawned crate
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.hasBlock() && event.getClickedBlock().getType() == Material.BARREL) {
            FallingCrate fallingCrate = crateManager.getSpawnedCrate(event.getClickedBlock().getLocation());
            if (fallingCrate != null) {
                event.setCancelled(true);

                if (fallingCrate.getPlayer().getUniqueId() != event.getPlayer().getUniqueId()
                        && !event.getPlayer().hasPermission(Perms.BYPASS)) {
                    return;
                }

                Barrel barrel = (Barrel) event.getClickedBlock().getState();
                event.getPlayer().openInventory(barrel.getInventory());
            }
        }
    }

    /**
     * This will be used only if a WorldGuard hasn't covered the place event
     * (with PlayerInteractEvent below)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        Block block = event.getBlock();
        Crate crate = null;

        if (!item.getType().isAir())
            crate = this.crateManager.getCrateByItem(item);

        if (crate == null)
            return;

        event.setCancelled(true);

        this.crateManager.interactCrate(player, crate, InteractType.CRATE_OPEN, item, block);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlaceInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        Block block = null;
        Crate crate = null;

        // Check if trying to open a spawned crate
        if (event.hasBlock() && event.getClickedBlock().getType() == Material.BARREL) {
            FallingCrate fallingCrate = crateManager.getSpawnedCrate(event.getClickedBlock().getLocation());
            if (fallingCrate != null && fallingCrate.getPlayer().getUniqueId() != event.getPlayer().getUniqueId())
                return;
        }

        if (item != null && !item.getType().isAir()) {
            crate = this.crateManager.getCrateByItem(item);
        }

        if (crate != null && event.getClickedBlock() != null) {
            WorldGuardPlatform platform = WorldGuard.getInstance().getPlatform();
            RegionQuery query = platform.getRegionContainer().createQuery();

            // Check if can build - let PlaceBlockEvent handle if can WG build
            Location wgLoc = BukkitAdapter.adapt(event.getClickedBlock().getLocation());
            LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);

            if (query.testBuild(wgLoc, localPlayer))
                return;

            // Check if there is no region with an ID that starts with "ipasums-"
            // If there is, cancel the placement
            for (ProtectedRegion region : query.getApplicableRegions(wgLoc).getRegions()) {
                if (region.getId().startsWith("ipasums-"))
                    return;
            }
        }

        if (crate == null) {
            item = null;
            block = event.getClickedBlock();
            if (block == null)
                return;

            crate = this.crateManager.getCrateByBlock(block);
        }
        if (crate == null)
            return;

        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);

        if (event.getHand() != EquipmentSlot.HAND)
            return;

        event.setCancelled(true);

        // Block is set to the clicked block anyway (for placement location)
        if (event.getClickedBlock() != null)
            block = event.getClickedBlock().getRelative(event.getBlockFace());

        this.crateManager.interactCrate(player, crate, InteractType.CRATE_OPEN, item, block);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrateBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.BARREL)
            return;

        FallingCrate fallingCrate = crateManager.getSpawnedCrate(event.getBlock().getLocation());
        if (fallingCrate != null)
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCrateAnvilStop(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        ItemStack first = inventory.getItem(0);
        ItemStack second = inventory.getItem(1);

        if ((first != null && this.crateManager.isCrate(first)) || (second != null && this.crateManager.isCrate(second))) {
            event.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCrateCraftStop(CraftItemEvent event) {
        CraftingInventory inventory = event.getInventory();
        if (Stream.of(inventory.getMatrix()).anyMatch(item -> item != null && this.crateManager.isCrate(item))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getLocation() == null || event.getInventory().getType() != InventoryType.BARREL)
            return;

        FallingCrate fallingCrate = crateManager.getSpawnedCrate(event.getInventory().getLocation());
        if (fallingCrate == null)
            return;

        fallingCrate.removeIfEmpty();
    }
}
