package net.mangolise.paintball.entity.bdstudio;

import net.flamgop.BDStudio;
import net.flamgop.BDStudioGenericData;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentBlockState;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentItemStack;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class BDStudioEntityUtil {
    @SuppressWarnings("deprecation")
    public static void applyBDStudioToEntity(@NotNull Entity entity, @NotNull BDStudio bdStudio) throws IOException {
        assignGenericDataFromBdStudioToEntity(entity, bdStudio);
        for (BDStudio.Child child : bdStudio.children) {
            EntityType type = child.isBlockDisplay ? EntityType.BLOCK_DISPLAY : child.isTextDisplay ? EntityType.TEXT_DISPLAY : child.isItemDisplay ? EntityType.ITEM_DISPLAY : null;
            if (type == null) throw new IllegalStateException("Child does not have a display type! BDStudio object possibly corrupt.");
            Entity childEnt = new Entity(type);
            assignGenericDataFromBdStudioToEntity(childEnt, child);
            if (child.isTextDisplay) {
                childEnt.editEntityMeta(TextDisplayMeta.class, m -> {
                    BDStudio.TextOptions options = child.options;
                    m.setBackgroundColor(((byte) (0xff * options.backgroundAlpha) << 24) | (Integer.parseInt(options.backgroundColor) & 0x00FFFFFF));
                    m.setTextOpacity((byte) (0xff * options.alpha));
                    m.setLineWidth(options.lineLength);
                    switch (options.align) {
                        case LEFT -> m.setAlignLeft(true);
                        case RIGHT -> m.setAlignRight(true);
                        case CENTER -> {
                        }
                    }

                    Style.Builder builder = Style.style();
                    if (options.bold) builder.decorate(TextDecoration.BOLD);
                    if (options.italic) builder.decorate(TextDecoration.ITALIC);
                    if (options.underline) builder.decorate(TextDecoration.UNDERLINED);
                    if (options.strikeThrough) builder.decorate(TextDecoration.STRIKETHROUGH);
                    builder.color(TextColor.color(Integer.parseInt(options.color)));
                    m.setText(Component.text(child.name).style(builder));
                });
            } else {
                if (child.isBlockDisplay) {
                    childEnt.editEntityMeta(BlockDisplayMeta.class, m -> {
                        Block block = ArgumentBlockState.staticParse(child.name);
                        m.setBlockState(block);
                    });
                }
                if (child.isItemDisplay) {
                    childEnt.editEntityMeta(ItemDisplayMeta.class, m -> {
                        String newInput = child.name.replaceAll("display=[^,\\s]+(,?)", "");
                        ItemStack stack = ArgumentItemStack.staticParse(newInput);
                        m.setItemStack(stack);
                        m.setDisplayContext(switch (child.name.substring(child.name.indexOf("display=")+1)) {
                            case "ground" -> ItemDisplayMeta.DisplayContext.GROUND;
                            case "head" -> ItemDisplayMeta.DisplayContext.HEAD;
                            case "thirdperson_righthand" -> ItemDisplayMeta.DisplayContext.THIRD_PERSON_RIGHT_HAND;
                            case "firstperson_righthand" -> ItemDisplayMeta.DisplayContext.FIRST_PERSON_RIGHT_HAND;
                            case "fixed" -> ItemDisplayMeta.DisplayContext.FIXED;
                            default -> ItemDisplayMeta.DisplayContext.NONE;
                        });
                    });
                }
            }

            entity.addPassenger(childEnt);
        }
    }

    private static void assignGenericDataFromBdStudioToEntity(Entity entity, BDStudioGenericData bdStudio) throws IOException {
        MiniMessage miniMessage = MiniMessage.miniMessage();
        entity.setCustomName(miniMessage.deserialize(bdStudio.name));
        if (!bdStudio.nbt.isEmpty()) {
            CompoundBinaryTag tag = BinaryTagIO.reader().read(new ByteArrayInputStream(bdStudio.nbt.getBytes(StandardCharsets.UTF_8)));
            entity.tagHandler().updateContent(tag);
        }
        entity.editEntityMeta(AbstractDisplayMeta.class, m -> {
            float[] transformFloats = new float[4*4];
            for (int i = 0; i < 16; i++) transformFloats[i] = bdStudio.transforms.get(i).floatValue();
            Matrix4f transformMatrix = new Matrix4f();
            transformMatrix.set(transformFloats).transpose();

            Vector3f translation = transformMatrix.getTranslation(new Vector3f());
            m.setTranslation(new Vec(translation.x, translation.y, translation.z));

            Vector3f scale = transformMatrix.getScale(new Vector3f());
            m.setScale(new Vec(scale.x, scale.y, scale.z));

            Quaternionf left = transformMatrix.getNormalizedRotation(new Quaternionf()).normalize();
            m.setLeftRotation(new float[]{left.x, left.y, left.z, left.w});
        });
    }
}
