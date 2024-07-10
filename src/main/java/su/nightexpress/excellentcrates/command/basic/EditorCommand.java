package su.nightexpress.excellentcrates.command.basic;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.CratesPlugin;
import su.nightexpress.excellentcrates.config.Perms;
import su.nightexpress.excellentcrates.config.Lang;
import su.nightexpress.nightcore.command.CommandResult;
import su.nightexpress.nightcore.command.impl.AbstractCommand;

public class EditorCommand extends AbstractCommand<CratesPlugin> {

    public EditorCommand(@NotNull CratesPlugin plugin) {
        super(plugin, new String[]{"editor"}, Perms.COMMAND_EDITOR);
        this.setDescription(Lang.COMMAND_EDITOR_DESC);
        this.setPlayerOnly(true);
    }

    @Override
    protected void onExecute(@NotNull CommandSender sender, @NotNull CommandResult result) {
        Player player = (Player) sender;
        if (!player.getWorld().getName().equalsIgnoreCase("spawn")) {
            sender.sendMessage("You can edit crates only at spawn!");
            return;
        }

        this.plugin.getCrateManager().openEditor((Player) sender);
    }
}
