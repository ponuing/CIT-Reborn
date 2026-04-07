package com.ponuing.defaults.mixin.types.item;

import net.minecraft.client.render.item.model.BasicItemModel;
import net.minecraft.client.render.item.tint.TintSource;
import net.minecraft.client.render.model.BakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(BasicItemModel.class)
public interface BasicItemModelInvoker {
    @Invoker("<init>")
    static BasicItemModel CITReborn$create(BakedModel model, List<TintSource> tints) {
        throw new AssertionError("Invoker body should be replaced by Mixin");
    }
}
