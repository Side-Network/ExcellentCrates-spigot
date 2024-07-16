package su.nightexpress.excellentcrates.api.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.crate.impl.Crate;

public class CrateFallenEvent extends CrateEvent {

    private final Location location;

    public CrateFallenEvent(@NotNull Crate crate, @NotNull Location location, @NotNull Player player) {
        super(crate, player);
        this.location = location;
    }

    @NotNull
    public Location getLocation() {
        return location;
    }
}
