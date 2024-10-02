package net.mangolise.paintball.weapon;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A RayCast hit result.
 *
 * @param hit The entity that was hit, null no entity was hit.
 * @param pos The position that the entity was hit at, or the position that the ray ended at
 */
public record HitResult(@Nullable Entity hit, @NotNull Vec pos) {
}
