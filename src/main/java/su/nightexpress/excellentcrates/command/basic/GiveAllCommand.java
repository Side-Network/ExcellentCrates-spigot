package su.nightexpress.excellentcrates.command.basic;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.CratesPlugin;
import su.nightexpress.excellentcrates.Placeholders;
import su.nightexpress.excellentcrates.command.CommandFlags;
import su.nightexpress.excellentcrates.config.Lang;
import su.nightexpress.excellentcrates.config.Perms;
import su.nightexpress.excellentcrates.crate.impl.Crate;
import su.nightexpress.nightcore.command.CommandResult;
import su.nightexpress.nightcore.command.impl.AbstractCommand;

import java.util.Collection;
import java.util.List;

public class GiveAllCommand extends AbstractCommand<CratesPlugin> {

    public GiveAllCommand(@NotNull CratesPlugin plugin) {
        super(plugin, new String[]{"giveall"}, Perms.COMMAND_GIVE);
        this.setDescription(Lang.COMMAND_GIVE_ALL_DESC);
        this.setUsage(Lang.COMMAND_GIVE_ALL_USAGE);
        this.addFlag(CommandFlags.SILENT);
    }

    @Override
    @NotNull
    public List<String> getTab(@NotNull Player player, int arg, @NotNull String[] args) {
        if (arg == 1) {
            return plugin.getCrateManager().getCrateIds(false);
        }
        if (arg == 2) {
            return List.of("1", "5", "10");
        }
        return super.getTab(player, arg, args);
    }

    @Override
    protected void onExecute(@NotNull CommandSender sender, @NotNull CommandResult result) {
        if (result.length() < 2) {
            this.errorUsage(sender);
            return;
        }

        int amount = result.length() >= 3 ? result.getInt(2, 1) : 1;

        Crate crate = plugin.getCrateManager().getCrateById(result.getArg(1));
        if (crate == null) {
            Lang.ERROR_INVALID_CRATE.getMessage().send(sender);
            return;
        }

        Collection<? extends Player> players = this.plugin.getServer().getOnlinePlayers();
        boolean silent = result.hasFlag(CommandFlags.SILENT);

        players.forEach(player -> {
            plugin.getCrateManager().giveCrate(player, crate, amount);

            if (!silent) {
                Lang.COMMAND_GIVE_NOTIFY.getMessage()
                        .replace(Placeholders.GENERIC_AMOUNT, amount)
                        .replace(crate.replacePlaceholders())
                        .send(player);
            }
        });

        Lang.COMMAND_GIVE_ALL_DONE.getMessage()
                .replace(Placeholders.GENERIC_AMOUNT, amount)
                .replace(crate.replacePlaceholders())
                .send(sender);
    }
}
