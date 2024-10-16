package net.mangolise.paintball.weapon;

import net.flamgop.BDStudio;
import net.flamgop.BDStudioUtil;
import net.mangolise.paintball.entity.BehavioralEntity;
import net.mangolise.paintball.entity.EntityBehavior;
import net.mangolise.paintball.entity.bdstudio.BDStudioEntityUtil;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeInstance;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.instance.block.Block;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.time.TimeUnit;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class SlimeStickyProjectileBehavior implements EntityBehavior {

    private static final BDStudio SLIME_BLOCK_DISPLAY = BDStudioUtil.bdStudio2POJO("H4sIAAAAAAAACsWUyW4TURBF/6XXnVYNtyYvgQ9gH0UoCUYYHBvZZoFQ/h29FyOQ3UIMBro3rVK9Gs69/a4/D6v98+16vbw/rLabYXHYfVyOw+b2YTkshpe77bvl/WEYh83dYVgMwzgcdreb/Zvt7mE/LK55pOM7+3UzDvdvV+vXu+VmWFy3Ts/W2/v3L1b7D+vbTye99uvVw/LVXUv4yX5XNImd9XwcL9CIJqI0ToNCIlQ5pPXjUhM1rhS1FG2xSiZLNzWol/BIk41tNPr+4RhpKiM1kiKIIPppTpMy0Qxpx8taEBqtRHkKpbqysHBW3xjq6cKRiKzQlkfmCEcSh4NTcqTJj2guiCSDjCWIs0S4ZKSJ1SytoGpEVtFiCGUiI1RESFVLkz6mmLmll0vBna3NWQgDMuAFU+rrK6dVOkwgEiVHpRuYYiTD3ELEA2igs6ClpSrUSI40pbCmdibKiRbyjAsDuToFH4WjepKVLIQQYuoyZ5UHl2myGhwjHyc6FdRbLD0ySdnraYmZqtBfMwry3Cgc3YGXdkqbK6WpzMqAKdzbUqQk4gZPLQ50pZugmp4EIW5qUjfuZO1XQSLYzcD9pypFpYsBFVqtpIFLAfYSY9JulHpC4hCQEFcxFUf17bW+mcfTvOWHAw7J8EKUdVf/Da+cIIk4R6JzSHiaA4JzIPanQKIwA8T/xmXS2pSFKYchlHoXk3ITZrKQBJrmUuphmcbEIWjMlOd4xDkP/JgHswQIoZaUatoqOFmVgtNCq7KnRwa0MtLIGSpHQf41jRkY8d0N8Fs48AMcdY6DZ3C0ov/FHbM8vg5zeRwz7pjFcXV6e9w83nwBwmXLyGQJAAA=");

    private final BoundingBox effectBox = new BoundingBox(3, 1, 3, new Vec(-1, 0, -1));
    private final long maxLifetimeTicks = 300L;
    private final long damageCooldownTicks = 5L;

    @Override
    public EntityType type() {
        return EntityType.BLOCK_DISPLAY;
    }

    @Override
    public void update(BehavioralEntity entity, long time) {
        entity.getInstance().getEntities().stream().filter(e -> (e instanceof Player) && ((Player)e).getGameMode() != GameMode.SPECTATOR && !entity.isMyShooter(e) && effectBox.intersectEntity(entity.getPosition(), e)).forEach(e -> {
            Player p = (Player)e;
            AttributeInstance attr = p.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (attr.getBaseValue() != 0.025) {
                attr.setBaseValue(0.025);
                CompletableFuture<Void> leftEffectBox = new CompletableFuture<>();
                p.scheduler().scheduleTask(() -> {
                    if (!effectBox.intersectEntity(entity.getPosition(), p)) {
                        return TaskSchedule.stop();
                    }
                    entity.triggerAction(new PlayerHitPlayerWeaponContext(entity.context().game(), entity.context().shooter(), p, entity.getPosition().add(0.5, 0.5, 0.5)));
                    return TaskSchedule.duration(damageCooldownTicks, TimeUnit.SERVER_TICK);
                }, TaskSchedule.duration(damageCooldownTicks, TimeUnit.SERVER_TICK));
                p.scheduler().scheduleTask(() -> {
                    if (!effectBox.intersectEntity(entity.getPosition(), p)) {
                        leftEffectBox.complete(null);
                        return TaskSchedule.stop();
                    }
                    return TaskSchedule.nextTick();
                }, TaskSchedule.nextTick());
                leftEffectBox.whenComplete(($$1, $$2) -> attr.setBaseValue(0.1));
            }
        });
    }

    @Override
    public void spawn(BehavioralEntity entity) {
        entity.editEntityMeta(BlockDisplayMeta.class, meta -> {
            meta.setBlockState(Block.AIR);
            meta.setGlowColorOverride(0xff0000);
            meta.setHasGlowingEffect(true);
        });
        try {
            BDStudioEntityUtil.applyBDStudioToEntity(entity, SLIME_BLOCK_DISPLAY);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        entity.scheduleRemove(Duration.of(maxLifetimeTicks, TimeUnit.SERVER_TICK));
    }

    @Override
    public void despawn(BehavioralEntity entity) {
        entity.getPassengers().forEach(Entity::remove);
    }
}
