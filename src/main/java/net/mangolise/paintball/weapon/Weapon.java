package net.mangolise.paintball.weapon;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.particle.Particle;
import net.minestom.server.tag.Tag;

import java.util.Locale;

public enum Weapon {
    FROUP_DE_FROUP(
            Component.text("Froup de Froup"),
            Material.FLINT_AND_STEEL,
            context -> {
                switch (context) {
                    case WeaponAction.HitPlayerContext hit -> {
                        hit.shootParticles(Particle.FLAME);
                        hit.target().damage(Damage.fromPlayer(context.player(), 2));
                    }
                    case WeaponAction.MissContext miss -> {
                        miss.shootParticles(Particle.SMOKE);
                    }
                }
            }
    ),
    ;

    private final Component displayName;
    private final ItemStack displayItem;
    private final WeaponAction action;

    private static final Tag<String> WEAPON_ID_TAG = Tag.String("weaponId");

    Weapon(Component displayName, Material displayMaterial, WeaponAction action) {
        this.displayName = displayName;
        this.displayItem = ItemStack.of(displayMaterial)
                .withCustomName(displayName)
                .withTag(Tag.String("weaponId"), name().toLowerCase(Locale.ROOT));
        this.action = action;
    }

    public static Weapon weaponFromItemStack(ItemStack itemStack) {
        return valueOf(itemStack.getTag(Tag.String("weaponId")).toUpperCase(Locale.ROOT));
    }

    public Component displayName() {
        return displayName;
    }

    public ItemStack displayItem() {
        return displayItem;
    }

    public WeaponAction action() {
        return action;
    }
}
