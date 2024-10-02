package net.mangolise.paintball.weapon;

import dev.emortal.rayfast.area.area3d.Area3d;
import dev.emortal.rayfast.util.VectorMathUtil;
import dev.emortal.rayfast.vector.Vector3d;
import net.mangolise.paintball.PaintballGame;
import net.mangolise.paintball.util.VectorUtils;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Taggable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface HitRegisterer {

    List<HitResult> hit(Context context);

    static HitRegisterer clientSideHitScan() {
        return new ClientSideHitScan();
    }

    static HitRegisterer basicSingleShot(double scatter, double range) {
        return new BasicSingleShot(scatter, range);
    }

    static HitRegisterer basicSpreadShot(double scatter, double range, int shots) {
        return new BasicSpreadShot(scatter, range, shots);
    }

    sealed interface Context extends Taggable permits BasicHitRegistererContext {
        PaintballGame game();
        Player shooter();
        Instance instance();
        Pos eyePos();
    }
}

record ClientSideHitScan() implements HitRegisterer {
    @Override
    public List<HitResult> hit(Context context) {
        throw new IllegalCallerException("ClientSideHitScan#hit is hardcoded in UseWeaponFeature!");
    }
}

record BasicSingleShot(double scatter, double range) implements HitRegisterer {
    @Override
    public List<HitResult> hit(Context context) {
        return singleShotScattered(context, scatter, range);
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
    public List<HitResult> hit(Context context) {
        List<HitResult> hits = new ArrayList<>();
        for (int i = 0; i < shots; i++) {
            hits.addAll(BasicSingleShot.singleShotScattered(context, scatter, range));
        }
        return hits;
    }
}