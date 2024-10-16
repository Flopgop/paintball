package net.mangolise.paintball.weapon;

import net.mangolise.paintball.PaintballGame;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.TagHandler;

public record BasicHitRegistererContext(PaintballGame game, Player shooter, Instance instance, Pos eyePos, TagHandler tagHandler, Weapon weapon) implements HitRegisterer.Context {
    public BasicHitRegistererContext(PaintballGame game, Player shooter) {
        this(game, shooter, shooter.getInstance(), shooter.getPosition().add(0, shooter.getEyeHeight(), 0), TagHandler.newHandler(), Weapon.weaponFromItemStack(shooter.getItemInMainHand()));
    }
}
