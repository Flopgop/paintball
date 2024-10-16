package net.mangolise.paintball.weapon;

import net.mangolise.gamesdk.Game;
import net.mangolise.paintball.PaintballGame;
import net.mangolise.paintball.util.PaintballUtils;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.client.play.ClientInteractEntityPacket;
import net.minestom.server.network.packet.client.play.ClientPlayerBlockPlacementPacket;
import net.minestom.server.network.packet.client.play.ClientUseItemPacket;

import java.util.List;

public record UseWeaponFeature() implements Game.Feature<PaintballGame> {
    @Override
    public void setup(Context<PaintballGame> context) {
        PaintballGame game = context.game();
        Instance instance = game.getTag(PaintballGame.INSTANCE_TAG);

        instance.eventNode().addListener(PlayerMoveEvent.class, event -> {
            // this is formatted the way it is such that hasTag gets called *only* if it is to be changed, that way we avoid calling it every single time a player moves and slowing down the server.
            if (event.isOnGround() && !event.getPlayer().isOnGround() && event.getPlayer().hasTag(Actions.Knockback.KNOCKED)) {
                event.getPlayer().setTag(Actions.Knockback.KNOCKED, false);
            }
        });

        instance.eventNode().addListener(PlayerPacketEvent.class, event -> {
            Player player = event.getPlayer();

            // stupid specific ik
            if (
                event.getPacket() instanceof ClientUseItemPacket packet &&
                packet.hand() == Player.Hand.MAIN &&
                !PaintballUtils.hasWeaponCooldown(player) &&
                Weapon.weaponFromItemStack(player.getItemInHand(packet.hand())) != null &&
                !(Weapon.weaponFromItemStack(player.getItemInHand(packet.hand())).hitreg() instanceof ClientSideHitScan)
            ) {
                ItemStack item = player.getItemInHand(packet.hand());
                Weapon weapon = Weapon.weaponFromItemStack(item);

                if (!weapon.hitreg().delayedHitBehavior()) {
                    List<HitResult> hits = weapon.hitreg().hits(new BasicHitRegistererContext(game, player));
                    hits.forEach(h -> {
                        // TODO: if ever necessary, regular entity hitting can be implemented via this and a slight modification to the HitRegisterers
                        if (!(h.hit() instanceof Player p))
                            weapon.action().execute(new PlayerMissWeaponContext(game, player, h.pos()));
                        else weapon.action().execute(new PlayerHitPlayerWeaponContext(game, player, p, h.pos()));
                    });
                } else {
                    weapon.hitreg().hits(new BasicHitRegistererContext(game, player)); // if implemented to spec this code is redundant, however to avoid bad implementations of HitRegisterer, we only call this here to prevent avoidable bugs.
                }
            } else {
                if (!(event.getPacket() instanceof ClientInteractEntityPacket packet)) return;
                if (!(packet.type() instanceof ClientInteractEntityPacket.InteractAt entityInteractAt)) return;
                // we only care about one of the two hands
                if (entityInteractAt.hand() != Player.Hand.MAIN) return;

                // if the player is on cooldown, don't do anything
                if (PaintballUtils.hasWeaponCooldown(player)) return;
                if (player.getGameMode() == GameMode.SPECTATOR) return;

                int targetId = packet.targetId();
                Entity entity = instance.getEntityById(targetId);
                if (!(entity instanceof Player target)) return;
                Vec hitOffset = new Vec(entityInteractAt.targetX(), entityInteractAt.targetY(), entityInteractAt.targetZ());
                Vec hitPosition = hitOffset.add(target.getPosition());

                // check if the player is holding a weapon
                if (player.getInventory().getItemInMainHand().isAir()) return;
                ItemStack item = player.getInventory().getItemInMainHand();
                Weapon weapon = Weapon.weaponFromItemStack(item);
                if (weapon == null) return;
                if (!(weapon.hitreg() instanceof ClientSideHitScan)) return;

                // if the player is holding a weapon, use it
                weapon.action().execute(new PlayerHitPlayerWeaponContext(game, player, target, hitPosition));
            }
        });

        instance.eventNode().addListener(PlayerPacketEvent.class, event -> {
            Player player = event.getPlayer();

            if (!(event.getPacket() instanceof ClientPlayerBlockPlacementPacket blockPlace)) return;

            // if the player is on cooldown, don't do anything
            if (PaintballUtils.hasWeaponCooldown(player)) return;
            if (player.getGameMode() == GameMode.SPECTATOR) return;

            // check if the player is holding a weapon
            if (player.getInventory().getItemInMainHand().isAir()) return;
            ItemStack item = player.getInventory().getItemInMainHand();
            Weapon weapon = Weapon.weaponFromItemStack(item);
            if (weapon == null) return;

            // if the player is holding a weapon, use it
            Point hitPosition = blockPlace.blockPosition().add(blockPlace.cursorPositionX(), blockPlace.cursorPositionY(), blockPlace.cursorPositionZ());
            weapon.action().execute(new PlayerMissWeaponContext(game, player, Vec.fromPoint(hitPosition)));
        });

    }
}
