package net.mangolise.paintball.weapon;

import dev.emortal.rayfast.area.area3d.Area3d;
import dev.emortal.rayfast.util.VectorMathUtil;
import dev.emortal.rayfast.vector.Vector3d;
import net.mangolise.paintball.PaintballGame;
import net.mangolise.paintball.entity.BehavioralEntity;
import net.mangolise.paintball.entity.EntityBehavior;
import net.mangolise.paintball.util.VectorUtils;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Taggable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface HitRegisterer {

    /**
     * Used to calculate all hits given the context.
     *
     * @param context the Hit context
     * @return A list of hit results, the result of every single scan that was fired
     * @implNote when {@link #delayedHitBehavior()} returns true, this will only be called once,
     * and is expected to handle hits manually, and the output
     */
    @Contract("_ -> new")
    @NotNull List<HitResult> hits(@NotNull Context context);
    @Contract(pure = true)
    boolean delayedHitBehavior();

    static HitRegisterer clientSideHitScan() {
        return new ClientSideHitScan();
    }

    static HitRegisterer basicSingleShot(double scatter, double range) {
        return new BasicSingleShot(scatter, range);
    }

    static HitRegisterer basicSpreadShot(double scatter, double range, int shots) {
        return new BasicSpreadShot(scatter, range, shots);
    }

    static HitRegisterer projectile(EntityBehavior projectile, double projectileSpawnDistanceInFrontOfShooter, double projectileSpawningVelocity) {
        return new ProjectileHitReg(projectile, projectileSpawnDistanceInFrontOfShooter, projectileSpawningVelocity);
    }

    sealed interface Context extends Taggable permits BasicHitRegistererContext {
        PaintballGame game();
        Player shooter();
        Instance instance();
        Pos eyePos();
        Weapon weapon();
    }
}

record ProjectileHitReg(EntityBehavior projectileBehavior, double projectileSpawnDistanceInFrontOfShooter, double projectileSpawningVelocity) implements HitRegisterer {

    @Override
    public @NotNull List<HitResult> hits(@NotNull Context context) {
        Instance instance = context.instance();
        BehavioralEntity ent = new BehavioralEntity(projectileBehavior, context);
        ent.setInstance(instance, context.eyePos().asVec().add(context.shooter().getPosition().direction().normalize().mul(projectileSpawnDistanceInFrontOfShooter)));
        ent.setVelocity(context.shooter().getPosition().direction().normalize().mul(projectileSpawningVelocity));
        // whatever happens from here is not our problem. The projectile has full control.
        return List.of();
    }

    @Override
    public boolean delayedHitBehavior() {
        return true;
    }
}

record ClientSideHitScan() implements HitRegisterer {
    @Override
    public @NotNull List<HitResult> hits(@NotNull Context context) {
        throw new IllegalCallerException("ClientSideHitScan#hit is hardcoded in UseWeaponFeature!");
    }

    @Override
    public boolean delayedHitBehavior() {
        return false;
    }
}

record BasicSingleShot(double scatter, double range) implements HitRegisterer {
    @Override
    public @NotNull List<HitResult> hits(@NotNull Context context) {
        return singleShotScattered(context, scatter, range);
    }

    @Override
    public boolean delayedHitBehavior() {
        return false;
    }

    static List<HitResult> singleShotScattered(Context context, double scatter, double range) {
        Vec rayOrigin = Vec.fromPoint(context.eyePos());
        Vec rayDirection = context.shooter().getPosition().direction();
        Vec scattered = VectorUtils.scatter(rayDirection, scatter);

        return singleShot(context, rayOrigin, scattered, range);
    }

    static List<HitResult> singleShot(Context context, Vec rayOrigin, Vec rayDirection, double maxDistance) {
        Map<Entity, Area3d> possibleHits = new HashMap<>();
        for (Player player : context.instance().getPlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR) continue;
            if (player == context.shooter()) continue;
            possibleHits.put(player, Area3d.CONVERTER.from(player));
        }

        Vector3d pos = Vector3d.CONVERTER.from(rayOrigin);
        Vector3d dir = Vector3d.CONVERTER.from(rayDirection);
        Map<Entity, Vector3d> intersections = new HashMap<>();
        for (Map.Entry<Entity, Area3d> key : possibleHits.entrySet()) {
            Vector3d intersection = key.getValue().lineIntersection(pos, dir);
            if (intersection != null && VectorMathUtil.distanceSquared(intersection, pos) < maxDistance*maxDistance) {
                intersections.put(key.getKey(), intersection);
                break;
            }
        }
        List<HitResult> finalHits = intersections.entrySet().stream().map(v -> new HitResult(
                v.getKey(),
                new Vec(v.getValue().x(), v.getValue().y(), v.getValue().z())
        )).toList();

        return finalHits.isEmpty() ? List.of(new HitResult(null, rayOrigin.add(rayDirection.mul(maxDistance)))) : finalHits;
    }
}

record BasicSpreadShot(double scatter, double range, int shots) implements HitRegisterer {

    @Override
    public @NotNull List<HitResult> hits(@NotNull Context context) {
        List<HitResult> hits = new ArrayList<>();
        for (int i = 0; i < shots; i++) {
            hits.addAll(BasicSingleShot.singleShotScattered(context, scatter, range));
        }
        return hits;
    }

    @Override
    public boolean delayedHitBehavior() {
        return false;
    }
}