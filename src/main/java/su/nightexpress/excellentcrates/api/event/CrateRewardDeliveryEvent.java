package su.nightexpress.excellentcrates.api.event;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.crate.impl.Reward;

/**
 * Called when a reward has been rolled and is about to be given. A listener that shows the reward its own way
 * (a cutscene) can take the delivery over with {@link #defer()} and run it when its show reaches the reward.
 */
public class CrateRewardDeliveryEvent extends CrateEvent {

    private final Reward   reward;
    private final Runnable delivery;
    private boolean        deferred;

    public CrateRewardDeliveryEvent(@NotNull Reward reward, @NotNull Player player, @NotNull Runnable delivery) {
        super(reward.getCrate(), player);
        this.reward = reward;
        this.delivery = delivery;
    }

    @NotNull
    public Reward getReward() {
        return reward;
    }

    public boolean isDeferred() {
        return deferred;
    }

    /**
     * Takes over giving the reward. The caller must run the returned delivery exactly once, even if the player
     * leaves or its show is cut short: nothing else will give the reward.
     */
    @NotNull
    public Runnable defer() {
        this.deferred = true;
        return this.delivery;
    }
}
