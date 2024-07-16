package su.nightexpress.excellentcrates.api.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.crate.impl.Crate;

public class CratePlacedEvent extends CrateEvent {

    private final Location location;

    public CratePlacedEvent(@NotNull Crate crate, @NotNull Location location, @NotNull Player player) {
        super(crate, player);
        this.location = location;
    }

    @NotNull
    public Location getLocation() {
        return location;
    }
}
