package net.mangolise.paintball.entity;

import net.minestom.server.entity.EntityType;

/**
 * @implSpec This class should be considered stateless, it should not hold any data that should not be synchronized between ALL behavioral entities with this behavior.
 */
public interface EntityBehavior {

    /**
     *
     * @return the type which this behavior belongs to
     */
    EntityType type();

    /**
     * Called each tick
     *
     * @param entity the entity which called this behavior
     * @param time the time of the update in milliseconds
     */
    void update(BehavioralEntity entity, long time);

    /**
     * Called when a new instance is set
     *
     * @param entity the entity which called this behavior
     */
    void spawn(BehavioralEntity entity);

    /**
     * Called right before an entity is removed
     *
     * @param entity the entity which called this behavior
     */
    void despawn(BehavioralEntity entity);
}
