package net.mangolise.paintball.entity;

import net.mangolise.paintball.weapon.HitRegisterer;
import net.mangolise.paintball.weapon.WeaponAction;
import net.minestom.server.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class BehavioralEntity extends Entity {

    private final EntityBehavior behavior;
    private final HitRegisterer.Context context;

    public BehavioralEntity(@NotNull EntityBehavior behavior, @NotNull HitRegisterer.Context context) {
        super(behavior.type());
        this.behavior = behavior;
        this.context = context;
    }

    /**
     * Called each tick.
     *
     * @param time time of the update in milliseconds
     */
    public void update(long time) {
        behavior.update(this, time);
    }

    /**
     * Called when a new instance is set.
     */
    public void spawn() {
        behavior.spawn(this);
    }

    /**
     * Called right before an entity is removed
     */
    protected void despawn() {
        behavior.despawn(this);
    }

    public void triggerAction(@NotNull WeaponAction.Context damageContext) {
        context.weapon().action().execute(damageContext);
    }

    public boolean isMyShooter(@NotNull Entity entity) {
        return this.context.shooter() == entity;
    }

    public final HitRegisterer.Context context() {
        return context;
    }
}
