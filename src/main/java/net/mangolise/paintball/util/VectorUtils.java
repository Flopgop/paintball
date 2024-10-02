package net.mangolise.paintball.util;

import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

public class VectorUtils {
    public static @NotNull Vec perpendicular(@NotNull Vec v) {
        Vec y = new Vec(0, 1,  0);
        if (Math.abs(v.dot(y)) > 0.999) {
            y = new Vec(1, 0, 0);
        }
        return v.cross(y).normalize();
    }

    public static @NotNull Vec rotate(Vec v, Vec axis, double angle) {
        axis = axis.normalize();

        return v.mul(Math.cos(angle)).add(axis.cross(v).mul(Math.sin(angle))).add(axis.mul(axis.dot(v)).mul(1 - Math.cos(angle)));
    }

    public static @NotNull Vec scatter(@NotNull Vec v, double angle) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        double randomAngle = angle > 0 ? random.nextDouble(0, angle) : 0;
        double randomAzimuth = random.nextDouble(0, 2 * Math.PI);

        Vec perpendicular = VectorUtils.perpendicular(v);
        return VectorUtils.rotate(VectorUtils.rotate(v, perpendicular, randomAngle), v, randomAzimuth);
    }
}
