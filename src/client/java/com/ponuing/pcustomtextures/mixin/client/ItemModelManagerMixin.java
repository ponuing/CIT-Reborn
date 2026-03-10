package com.ponuing.pcustomtextures.mixin.client;

import com.ponuing.pcustomtextures.Pcustomtextures;
import com.ponuing.pcustomtextures.client.NbtRenderOverrideResolver;
import com.ponuing.pcustomtextures.client.TextureOverrideBakedModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelManager.class)
public class ItemModelManagerMixin {
    @Inject(
            method = "update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V",
            at = @At("TAIL")
    )
    private void pcustomtextures$applyOverride(ItemRenderState renderState, ItemStack stack, ModelTransformationMode mode, boolean leftHand, World world, LivingEntity entity, int seed, CallbackInfo ci) {
        applyOverride(renderState, stack);
    }

    @Inject(
            method = "update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V",
            at = @At("TAIL")
    )
    private void pcustomtextures$applyOverrideNoHand(ItemRenderState renderState, ItemStack stack, ModelTransformationMode mode, World world, LivingEntity entity, int seed, CallbackInfo ci) {
        applyOverride(renderState, stack);
    }

    private static void applyOverride(ItemRenderState renderState, ItemStack stack) {
        if (renderState == null || stack == null || stack.isEmpty()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        ItemRenderStateAccessor accessor = (ItemRenderStateAccessor) renderState;
        int count = accessor.pcustomtextures$getLayerCount();
        if (count <= 0) {
            Identifier itemId = net.minecraft.registry.Registries.ITEM.getId(stack.getItem());
            //Pcustomtextures.LOGGER.warn("[pcustomtextures][model] no layers for {}", itemId);
            return;
        }

        ItemRenderState.LayerRenderState layer = accessor.pcustomtextures$getLayers()[0];
        ItemRenderLayerStateAccessor layerAccessor = (ItemRenderLayerStateAccessor) layer;

        Identifier itemId = net.minecraft.registry.Registries.ITEM.getId(stack.getItem());
        Identifier modelId = NbtRenderOverrideResolver.resolveItemModelOverride(stack);
        if (modelId != null) {
            //Pcustomtextures.LOGGER.info("[pcustomtextures][model] override model for {} -> {}", itemId, modelId);
            BakedModel model = client.getBakedModelManager().getModel(new ModelIdentifier(modelId, "inventory"));
            if (model == null) {
                Pcustomtextures.LOGGER.warn("[pcustomtextures][model] baked model missing for {}", modelId);
                return;
            }
            Sprite resolvedSprite = NbtRenderOverrideResolver.resolveModelTextureSprite(modelId);
            if (resolvedSprite != null) {
                model = new TextureOverrideBakedModel(model, resolvedSprite);
            }
            RenderLayer renderLayer = TexturedRenderLayers.getItemEntityTranslucentCull();
            //Pcustomtextures.LOGGER.info("[pcustomtextures][model] applying model {} to first layer for {}", modelId, itemId);
            layer.setModel(model, renderLayer);
            return;
        }

        Identifier textureId = NbtRenderOverrideResolver.resolveItemTextureOverride(stack);
        if (textureId == null) {
            //Pcustomtextures.LOGGER.info("[pcustomtextures][model] no override model or texture for {}", itemId);
            return;
        }

        Sprite sprite = NbtRenderOverrideResolver.resolveSprite(textureId);
        if (sprite == null) {
            Pcustomtextures.LOGGER.warn("[pcustomtextures][model] sprite missing for texture {}", textureId);
            return;
        }

        BakedModel baseModel = layerAccessor.pcustomtextures$getModel();
        if (baseModel == null) {
            //Pcustomtextures.LOGGER.warn("[pcustomtextures][model] base model missing for {}", itemId);
            return;
        }

        RenderLayer renderLayer = layerAccessor.pcustomtextures$getRenderLayer();
        if (renderLayer == null) {
            renderLayer = TexturedRenderLayers.getItemEntityTranslucentCull();
        }
        //Pcustomtextures.LOGGER.info("[pcustomtextures][model] applying texture {} to first layer for {}", textureId, itemId);
        layer.setModel(new TextureOverrideBakedModel(baseModel, sprite), renderLayer);
    }
}
