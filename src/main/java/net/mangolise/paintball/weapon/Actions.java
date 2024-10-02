package net.mangolise.paintball.weapon;

import net.kyori.adventure.sound.Sound;
import net.mangolise.paintball.util.PaintballUtils;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

public interface Actions {
    record ApplyCombo(DoubleBinaryOperator damageCombo2damage) implements WeaponAction {
        @Override
        public void execute(Context context) {
            double damage = context.getTag(WeaponTags.DAMAGE_TAG);
            double combo = context.player().getTag(WeaponTags.COMBO_TAG);
            double newDamage = damageCombo2damage.applyAsDouble(damage, combo);
            context.setTag(WeaponTags.DAMAGE_TAG, newDamage);
        }
    }

    record ApplyDamage() implements WeaponAction {
        @Override
        public void execute(Context context) {
            switch (context) {
                case HitPlayerContext hit -> {
                    double damage = context.getTag(WeaponTags.DAMAGE_TAG);
                    hit.target().damage(Damage.fromPlayer(context.player(), (float) damage));
                }
                case MissContext miss -> {
                    // TODO: block cracking on miss
                }
            }
        }
    }

    record SetCooldown(double seconds) implements WeaponAction {
        @Override
        public void execute(Context context) {
            PaintballUtils.setWeaponCooldown(context.player(), seconds);
        }
    }

    record Knockback(Target target) implements WeaponAction {

        @ApiStatus.Internal
        public static final Tag<Boolean> KNOCKED = Tag.Boolean("knocked");

        @Override
        public void execute(Context context) {
            final double gravity = 9.81; // Player#getAerodynamics#gravity returns zero because player movement is calculated on the client side. Minestom bug or intended behavior?
            Vec knockbackDirection = context.eyePosition().sub(context.hitPosition()).asVec().normalize(); // away from the hit direction

            Player player = context.player();
            if (target.affectsSelf() && (!player.hasTag(KNOCKED) || !player.getTag(KNOCKED))) {
                Vec initialVelocity = player.getVelocity();
                player.setVelocity(initialVelocity.sub(0, initialVelocity.y(), 0).add(knockbackDirection.mul(gravity / ((context instanceof HitPlayerContext) ? 2 : 1))));
                player.setTag(KNOCKED, true);
            }

            if (target.affectsTarget() && context instanceof HitPlayerContext hitContext) {
                Player target = hitContext.target();
                Vec targetInitialVelocity = target.getVelocity();
                target.setVelocity(targetInitialVelocity.add(knockbackDirection.neg().mul(gravity / 2)));
            }
        }

        enum Target {
            SELF(true, false),
            TARGET(false, true),
            SELF_AND_TARGET(true, true),
            ;

            private boolean affectsSelf;
            private boolean affectsTarget;

            Target(boolean affectsSelf, boolean affectsTarget) {
                this.affectsSelf = affectsSelf;
                this.affectsTarget = affectsTarget;
            }

            public boolean affectsSelf() {
                return affectsSelf;
            }

            public boolean affectsTarget() {
                return affectsTarget;
            }
        }
    }

    record ExplosionOnKill(double maxSpeed, int particleCount) implements WeaponAction {
        @Override
        public void execute(Context context) {
            switch (context) {
                case HitPlayerContext hit -> {
                    double damage = hit.getTag(WeaponTags.DAMAGE_TAG);
                    boolean willDie = hit.target().getHealth() - damage <= 0;

                    // send explosion if the player will die
                    if (willDie) {
                        ParticlePacket packet = new ParticlePacket(Particle.WHITE_SMOKE, hit.hitPosition(), Vec.ZERO, (float) maxSpeed, particleCount);
                        hit.instance().sendGroupedPacket(packet);
                    }
                }
                case MissContext miss -> {
                    // TODO: explosion on miss handling
                }
            }
        }
    }

    record HitscanParticle(Particle particle, double stepSize) implements WeaponAction {
        @Override
        public void execute(Context context) {
            Vec start = Vec.fromPoint(context.eyePosition());
            Vec end = Vec.fromPoint(context.hitPosition());
            double length = start.distance(end);

            {
                // we offset the start position a bit so the particles don't spawn in front of the player eyes
                Vec dir = end.sub(start).normalize();
                start = start.add(dir.rotateAroundY(-Math.PI / 4.0).mul(0.3));
                start = start.sub(0, 0.1, 0);
            }

            Vec step = end.sub(start).normalize().mul(stepSize);
            double distanceTraveled = 0;
            do {
                ParticlePacket packet = new ParticlePacket(particle, true, start.x(), start.y(), start.z(), 0, 0, 0, 0, 1);
                context.instance().sendGroupedPacket(packet);
                start = start.add(step);
                distanceTraveled += stepSize;
            } while (distanceTraveled < length);
        }
    }

    record ModifyCombo(DoubleUnaryOperator apply) implements WeaponAction {
        @Override
        public void execute(Context context) {
            context.player().updateTag(WeaponTags.COMBO_TAG, apply::applyAsDouble);
        }
    }

    record PlayerSound(Sound sound) implements WeaponAction {
        @Override
        public void execute(Context context) {
            context.player().playSound(sound);
        }
    }

    /**
     * Set the charge bar of the weapon
     * @param charge the charge to set (0-1)
     */
    record SetChargeBar(double charge) implements WeaponAction {
        @Override
        public void execute(Context context) {
            context.player().sendMessage("Charge: " + charge);
            context.player().setExp((float) charge);
        }
    }

    record SetDamage(double damage) implements WeaponAction {
        @Override
        public void execute(Context context) {
            context.setTag(WeaponTags.DAMAGE_TAG, damage);
        }
    }
}
