package su.nightexpress.excellentcrates.crate.editor;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.CratesPlugin;
import su.nightexpress.excellentcrates.api.currency.Currency;
import su.nightexpress.excellentcrates.config.EditorLang;
import su.nightexpress.excellentcrates.config.Lang;
import su.nightexpress.excellentcrates.crate.impl.Crate;
import su.nightexpress.excellentcrates.key.CrateKey;
import su.nightexpress.nightcore.menu.MenuOptions;
import su.nightexpress.nightcore.menu.MenuViewer;
import su.nightexpress.nightcore.menu.click.ClickResult;
import su.nightexpress.nightcore.menu.impl.EditorMenu;
import su.nightexpress.nightcore.util.ItemReplacer;
import su.nightexpress.nightcore.util.ItemUtil;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.Players;

import java.util.HashSet;

public class CrateMainEditor extends EditorMenu<CratesPlugin, Crate> implements CrateEditor {

    private static final String TEXTURE_KEYS       = "311790e8005c7f972c469b7b875eab218e0713afe5f2edfd468659910ed622e3";
    private static final String TEXTURE_REWARDS    = "663029cc8167897e6535a3c5734bbabaff188d0905f9d9353afac62a06dadf86";
    private static final String TEXTURE_PLACEMENT  = "181e124a2765c4b320d754f04e1807ad7b3c26ff95376d0b4263c4e1ae84e758";
    private static final String TEXTURE_MILESTONES = "d194a22345d9cdde75168299ad61873bc105e3ae73cd6c9ac02a285291ad0f1b";

    public CrateMainEditor(@NotNull CratesPlugin plugin) {
        super(plugin, Lang.EDITOR_TITLE_CRATES.getString(), 36);

        this.addReturn(31, (viewer, event, crate) -> {
            this.runNextTick(() -> this.plugin.getCrateManager().openCratesEditor(viewer.getPlayer()));
        });

        this.addItem(Material.NAME_TAG, EditorLang.CRATE_DISPLAY_NAME, 2, (viewer, event, crate) -> {
            this.handleInput(viewer, Lang.EDITOR_ENTER_DISPLAY_NAME, (dialog, wrapper) -> {
                crate.setName(wrapper.getText());
                this.saveSettings(viewer, crate, false);
                return true;
            });
        });

        this.addItem(Material.ITEM_FRAME, EditorLang.CRATE_ITEM, 4, (viewer, event, crate) -> {
            ItemStack cursor = event.getCursor();
            if (cursor == null || cursor.getType().isAir()) {
                if (event.isLeftClick()) {
                    Players.addItem(viewer.getPlayer(), crate.getItem());
                }
                if (event.isRightClick()) {
                    Players.addItem(viewer.getPlayer(), crate.getRawItem());
                }
                return;
            }

            crate.setItem(cursor);
            event.getView().setCursor(null);
            this.saveSettings(viewer, crate, true);
        }).getOptions().setDisplayModifier(((viewer, item) -> {
            Crate crate = this.getObject(viewer);
            item.setType(crate.getItem().getType());
            item.setItemMeta(crate.getItem().getItemMeta());
            ItemUtil.editMeta(item, meta -> {
                meta.setDisplayName(EditorLang.CRATE_ITEM.getLocalizedName());
                meta.setLore(EditorLang.CRATE_ITEM.getLocalizedLore());
            });
        }));

        this.addItem(Material.GLOW_ITEM_FRAME, EditorLang.CRATE_PREVIEW_AND_OPENING, 6, (viewer, event, crate) -> {
            if (event.isShiftClick()) {
                if (event.isLeftClick()) {
                    crate.setPreviewConfig(null);
                }
                else if (event.isRightClick()) {
                    crate.setOpeningConfig(null);
                }

                this.saveSettings(viewer, crate, true);
            }
            else {
                if (event.isLeftClick()) {
                    this.handleInput(viewer, Lang.EDITOR_ENTER_PREVIEW_CONFIG, (dialog, wrapper) -> {
                        crate.setPreviewConfig(wrapper.getTextRaw());
                        this.saveSettings(viewer, crate, false);
                        return true;
                    }).setSuggestions(plugin.getCrateManager().getPreviewNames(), true);
                }
                else if (event.isRightClick()) {
                    this.handleInput(viewer, Lang.EDITOR_ENTER_ANIMATION_CONFIG, (dialog, wrapper) -> {
                        crate.setOpeningConfig(wrapper.getTextRaw());
                        this.saveSettings(viewer, crate, false);
                        return true;
                    }).setSuggestions(plugin.getOpeningManager().getMenuMap().keySet(), true);
                }
            }
        });

        this.addItem(ItemUtil.getSkinHead(TEXTURE_PLACEMENT), EditorLang.CRATE_PLACEMENT_INFO, 12, (viewer, event, crate) -> {
            this.runNextTick(() -> this.plugin.getCrateManager().openPlacementEditor(viewer.getPlayer(), crate));
        });

        this.addItem(ItemUtil.getSkinHead(TEXTURE_REWARDS), EditorLang.CRATE_REWARDS, 14, (viewer, event, crate) -> {
            this.runNextTick(() -> this.plugin.getCrateManager().openRewardsEditor(viewer.getPlayer(), crate));
        });

        this.getItems().forEach(menuItem -> menuItem.getOptions().addDisplayModifier((viewer, item) -> {
            ItemReplacer.replace(item, this.getObject(viewer).getAllPlaceholders().replacer());
        }));
    }

    @Override
    protected void onPrepare(@NotNull MenuViewer viewer, @NotNull MenuOptions options) {

    }

    @Override
    protected void onReady(@NotNull MenuViewer viewer, @NotNull Inventory inventory) {

    }

    @Override
    public void onClick(@NotNull MenuViewer viewer, @NotNull ClickResult result, @NotNull InventoryClickEvent event) {
        super.onClick(viewer, result, event);
        if (result.isInventory()) {
            event.setCancelled(false);
        }
    }
}
