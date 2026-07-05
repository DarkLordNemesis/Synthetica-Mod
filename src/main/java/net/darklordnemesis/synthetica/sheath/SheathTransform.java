package net.darklordnemesis.synthetica.sheath;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.crafting.Ingredient;
import org.joml.Vector3f;

public record SheathTransform(Ingredient validItems, Vector3f translation, Vector3f rotation, Vector3f scale) {

    // Codec for Vector3f
    public static final Codec<Vector3f> VECTOR3F_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("x").forGetter(Vector3f::x),
            Codec.FLOAT.fieldOf("y").forGetter(Vector3f::y),
            Codec.FLOAT.fieldOf("z").forGetter(Vector3f::z)
    ).apply(instance, Vector3f::new));


    // The Codec tells the game how to convert the JSON into this Record
    public static final Codec<SheathTransform> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC.fieldOf("valid_items").forGetter(SheathTransform::validItems),
            VECTOR3F_CODEC.optionalFieldOf("translation", new Vector3f(0, 0, 0)).forGetter(SheathTransform::translation),
            VECTOR3F_CODEC.optionalFieldOf("rotation", new Vector3f(0, 0, 0)).forGetter(SheathTransform::rotation),
            VECTOR3F_CODEC.optionalFieldOf("scale", new Vector3f(1, 1, 1)).forGetter(SheathTransform::scale)
    ).apply(instance, SheathTransform::new));

    // A helper method to check if an item is allowed to use this position
    public boolean isValidFor(net.minecraft.world.item.ItemStack stack) {
        return this.validItems.test(stack);
    }
}