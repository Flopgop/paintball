package net.mangolise.paintball;

import dev.emortal.rayfast.area.area3d.Area3d;
import dev.emortal.rayfast.area.area3d.Area3dRectangularPrism;
import dev.emortal.rayfast.vector.Vector3d;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.instance.block.Block;

import java.time.Duration;

public class InitRayfast {

    public static void init() {
        Vector3d.CONVERTER.register(Vec.class, vec -> Vector3d.of(vec.x(), vec.y(), vec.z()));
        Vector3d.CONVERTER.register(Point.class, point -> Vector3d.of(point.x(), point.y(), point.z()));
        Vector3d.CONVERTER.register(Pos.class, pos -> Vector3d.of(pos.x(), pos.y(), pos.z()));
        Area3d.CONVERTER.register(BoundingBox.class, box -> Area3dRectangularPrism.of(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()));
        Area3d.CONVERTER.register(Entity.class, entity -> {
            Point position = entity.getPosition();
            BoundingBox boundingBox = entity.getBoundingBox();
            // debug tool
            Entity entity1 = new Entity(EntityType.BLOCK_DISPLAY);
            entity1.editEntityMeta(BlockDisplayMeta.class, m -> {
                m.setBlockState(Block.GLOWSTONE);
                m.setTranslation(position.add(boundingBox.minX(), boundingBox.minY(), boundingBox.minZ()));
                m.setScale(new Vec(Math.abs(boundingBox.minX()) + Math.abs(boundingBox.maxX()), Math.abs(boundingBox.minY()) + Math.abs(boundingBox.maxY()), Math.abs(boundingBox.minZ()) + Math.abs(boundingBox.maxZ())));
            });
            entity1.setInstance(entity.getInstance());
            entity1.scheduleRemove(Duration.ofSeconds(5));
            return Area3d.CONVERTER.from(boundingBox.withOffset(position));
        });

    }
}
