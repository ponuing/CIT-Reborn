package com.ponuing.defaults.mixin.types.item;

import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.ponuing.cit.CIT;
import com.ponuing.cit.CITContext;
import com.ponuing.defaults.cit.types.TypeItem;

import java.util.Collections;
import static com.ponuing.defaults.cit.types.TypeItem.CONTAINER;

@Mixin(ItemModelManager.class)
public class ItemRendererMixin {
    @Inject(method = "update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V", at = @At("HEAD"), cancellable = true)
    private void CITReborn$updateItemModel(ItemRenderState state, ItemStack stack, ModelTransformationMode mode,
                                          World world, LivingEntity entity, int seed, CallbackInfo ci) {
        if (!CONTAINER.active())
            return;

        CITContext context = new CITContext(stack, world, entity);
        CIT<TypeItem> cit = CONTAINER.getCIT(context, seed);
        if (cit == null)
            return;

        var citModel = cit.type.getItemModel(context, seed);
        if (citModel == null)
            return;

        ItemModel itemModel = BasicItemModelInvoker.CITReborn$create(citModel, Collections.emptyList());
        ClientWorld clientWorld = world instanceof ClientWorld cw ? cw : null;
        itemModel.update(state, stack, (ItemModelManager) (Object) this, mode, clientWorld, entity, seed);
        ci.cancel();
    }
}
