package com.ponuing.pcustomtextures.mixin.client;

import com.ponuing.pcustomtextures.Pcustomtextures;
import com.ponuing.pcustomtextures.client.MergedTransformationBakedModel;
import com.ponuing.pcustomtextures.client.NbtRenderOverrideResolver;
import com.ponuing.pcustomtextures.client.TextureOverrideBakedModel;
import com.ponuing.pcustomtextures.client.TransformationOverrideBakedModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(ItemModelManager.class)
public class ItemModelManagerMixin {
    @Inject(
            method = "update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V",
            at = @At("TAIL")
    )
    private void pcustomtextures$applyOverride(ItemRenderState renderState, ItemStack stack, ModelTransformationMode mode, boolean leftHand, World world, LivingEntity entity, int seed, CallbackInfo ci) {
        applyOverride(renderState, stack, mode, leftHand);
    }

    @Inject(
            method = "update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V",
            at = @At("TAIL")
    )
    private void pcustomtextures$applyOverrideNoHand(ItemRenderState renderState, ItemStack stack, ModelTransformationMode mode, World world, LivingEntity entity, int seed, CallbackInfo ci) {
        applyOverride(renderState, stack, mode, null);
    }

    private static void applyOverride(ItemRenderState renderState, ItemStack stack, ModelTransformationMode mode, Boolean leftHand) {
        if (renderState == null || stack == null || stack.isEmpty()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        Identifier itemId = net.minecraft.registry.Registries.ITEM.getId(stack.getItem());
        NbtRenderOverrideResolver.HandMatch handMatch = NbtRenderOverrideResolver.HandMatch.ANY;
        if (mode == ModelTransformationMode.GUI) {
            handMatch = NbtRenderOverrideResolver.HandMatch.MAIN;
        } else if (leftHand != null) {
            handMatch = leftHand ? NbtRenderOverrideResolver.HandMatch.OFF : NbtRenderOverrideResolver.HandMatch.MAIN;
        }
        Identifier modelId = NbtRenderOverrideResolver.resolveItemModelOverride(stack, handMatch);
        Identifier textureId = NbtRenderOverrideResolver.resolveItemTextureOverride(stack, handMatch);
        ItemRenderStateAccessor accessor = (ItemRenderStateAccessor) renderState;
        int count = accessor.pcustomtextures$getLayerCount();
        ItemRenderState.LayerRenderState layer = count > 0 ? accessor.pcustomtextures$getLayers()[0] : null;
        ItemRenderLayerStateAccessor layerAccessor = layer != null ? (ItemRenderLayerStateAccessor) layer : null;
        boolean hadSpecialRenderer = layerAccessor != null && layerAccessor.pcustomtextures$getSpecialModelType() != null;

        if (layerAccessor != null && textureId != null && modelId == null && hadSpecialRenderer) {
            if (applySpecialTextureOverride(layerAccessor, textureId)) {
                return;
            }
        }

        if (modelId != null) {
            //Pcustomtextures.LOGGER.info("[pcustomtextures][model] override model for {} -> {}", itemId, modelId);
            BakedModel model = client.getBakedModelManager().getModel(new ModelIdentifier(modelId, "inventory"));
            BakedModel missing = client.getBakedModelManager().getMissingBlockModel();
            if (model == null || model == missing) {
                BakedModel baked = NbtRenderOverrideResolver.resolveModelBaked(modelId);
                if (baked != null) {
                    model = baked;
                }
            }
            if (model == null || model == missing) {
                Pcustomtextures.LOGGER.warn("[pcustomtextures][model] baked model missing for {}", modelId);
                return;
            }
            BakedModel baseModel = layerAccessor != null ? layerAccessor.pcustomtextures$getModel() : null;
            if (textureId != null) {
                Sprite sprite = NbtRenderOverrideResolver.resolveSprite(textureId);
                if (sprite != null) {
                    model = new TextureOverrideBakedModel(model, sprite);
                } else {
                    Pcustomtextures.LOGGER.warn("[pcustomtextures][model] sprite missing for texture {}", textureId);
                }
            } else if (isMissingSprite(model.getParticleSprite())) {
                Sprite resolved = NbtRenderOverrideResolver.resolveModelTextureSprite(modelId);
                if (resolved != null) {
                    model = new TextureOverrideBakedModel(model, resolved);
                }
            }
            if (baseModel != null) {
                if (hadSpecialRenderer) {
                    model = new MergedTransformationBakedModel(model, baseModel.getTransformation(), model.getTransformation());
                } else if (model.getTransformation() == ModelTransformation.NONE) {
                    model = new TransformationOverrideBakedModel(model, baseModel.getTransformation());
                }
            }
            if (!hasAnyQuads(model)) {
                Pcustomtextures.LOGGER.warn("[pcustomtextures][model] model has no quads, skipping override {}", modelId);
                return;
            }
            if (hadSpecialRenderer) {
                renderState.clear();
                accessor.pcustomtextures$setModelTransformationMode(mode != null ? mode : ModelTransformationMode.NONE);
                if (leftHand != null) {
                    accessor.pcustomtextures$setLeftHand(leftHand);
                }
                layer = renderState.newLayer();
                layerAccessor = (ItemRenderLayerStateAccessor) layer;
            } else if (layer == null) {
                layer = renderState.newLayer();
                layerAccessor = (ItemRenderLayerStateAccessor) layer;
            }
            if (layerAccessor.pcustomtextures$getSpecialModelType() != null) {
                layerAccessor.pcustomtextures$setSpecialModelType(null);
                layerAccessor.pcustomtextures$setSpecialModelData(null);
            }
            RenderLayer renderLayer = layerAccessor.pcustomtextures$getRenderLayer();
            if (renderLayer == null) {
                renderLayer = RenderLayers.getItemLayer(stack);
            }
            if (renderLayer == null) {
                renderLayer = TexturedRenderLayers.getItemEntityTranslucentCull();
            }
            //Pcustomtextures.LOGGER.info("[pcustomtextures][model] applying model {} to first layer for {}", modelId, itemId);
            layer.setModel(model, renderLayer);
            return;
        }

        if (textureId == null) {
            //Pcustomtextures.LOGGER.info("[pcustomtextures][model] no override model or texture for {}", itemId);
            return;
        }

        if (count <= 0) {
            // No baked layer to replace; avoid forcing special renderers into empty models.
            return;
        }

        layer = accessor.pcustomtextures$getLayers()[0];
        layerAccessor = (ItemRenderLayerStateAccessor) layer;

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
            renderLayer = RenderLayers.getItemLayer(stack);
        }
        if (renderLayer == null) {
            renderLayer = TexturedRenderLayers.getItemEntityTranslucentCull();
        }
        //Pcustomtextures.LOGGER.info("[pcustomtextures][model] applying texture {} to first layer for {}", textureId, itemId);
        layer.setModel(new TextureOverrideBakedModel(baseModel, sprite), renderLayer);
    }

    private static boolean hasAnyQuads(BakedModel model) {
        if (model == null) {
            return false;
        }
        Random random = Random.create();
        long seed = 42L;
        for (Direction dir : Direction.values()) {
            random.setSeed(seed);
            if (!model.getQuads(null, dir, random).isEmpty()) {
                return true;
            }
        }
        random.setSeed(seed);
        return !model.getQuads(null, null, random).isEmpty();
    }


    private static boolean isMissingSprite(Sprite sprite) {
        if (sprite == null) {
            return true;
        }
        try {
            return MissingSprite.getMissingSpriteId().equals(sprite.getContents().getId());
        } catch (Exception e) {
            return false;
        }
    }

    private static final Map<Class<?>, Field> SPRITE_FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Field> IDENTIFIER_FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Field NO_FIELD = null;

    private static boolean applySpecialTextureOverride(ItemRenderLayerStateAccessor layerAccessor, Identifier textureId) {
        Object renderer = layerAccessor.pcustomtextures$getSpecialModelType();
        if (renderer == null || textureId == null) {
            return false;
        }

        Identifier spriteId = NbtRenderOverrideResolver.toSpriteId(textureId);
        if (spriteId == null) {
            return false;
        }

        Field spriteField = findSpriteIdentifierField(renderer.getClass());
        if (spriteField != null) {
            try {
                SpriteIdentifier current = (SpriteIdentifier) spriteField.get(renderer);
                if (current == null) {
                    return false;
                }
                spriteField.set(renderer, new SpriteIdentifier(current.getAtlasId(), spriteId));
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }

        Field idField = findIdentifierField(renderer.getClass());
        if (idField != null) {
            try {
                idField.set(renderer, textureId);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }

        return false;
    }

    private static Field findSpriteIdentifierField(Class<?> type) {
        return SPRITE_FIELD_CACHE.computeIfAbsent(type, cls -> findFieldByType(cls, SpriteIdentifier.class));
    }

    private static Field findIdentifierField(Class<?> type) {
        return IDENTIFIER_FIELD_CACHE.computeIfAbsent(type, cls -> findFieldByType(cls, Identifier.class));
    }

    private static Field findFieldByType(Class<?> type, Class<?> fieldType) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getType() == fieldType) {
                    String name = field.getName().toLowerCase(java.util.Locale.ROOT);
                    if (fieldType == Identifier.class && !name.contains("texture")) {
                        continue;
                    }
                    field.setAccessible(true);
                    return field;
                }
            }
            current = current.getSuperclass();
        }
        return NO_FIELD;
    }
}
