package com.ponuing.defaults.mixin.types.item;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.entity.LoadedBlockEntityModels;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.Baker;
import net.minecraft.client.render.model.GroupableModel;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.render.model.ModelRotation;
import net.minecraft.client.render.model.ResolvableModel;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.SpriteAtlasManager;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.ponuing.CITReborn;
import com.ponuing.cit.CIT;
import com.ponuing.defaults.cit.types.TypeItem;
import com.ponuing.defaults.common.RebornItemModelIdentifier;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.ponuing.CITReborn.info;
import static com.ponuing.defaults.cit.types.TypeItem.CONTAINER;

@Mixin(BakedModelManager.class)
public class ModelLoaderMixin {
    @Shadow private Map<ModelIdentifier, BakedModel> bakedBlockModels;

    @Inject(method = "bake", at = @At("HEAD"))
    private static void CITReborn$addTypeItemModelIds(Profiler profiler,
                                                      Map<Identifier, SpriteAtlasManager.AtlasPreparation> atlasPreparations,
                                                      ModelBaker modelBaker,
                                                      Object2IntMap<BlockState> modelGroups,
                                                      LoadedEntityModels entityModels,
                                                      LoadedBlockEntityModels blockEntityModels,
                                                      CallbackInfoReturnable<Object> cir) {
        if (!CONTAINER.active() || CONTAINER.loaded.isEmpty())
            return;

        Map<ModelIdentifier, GroupableModel> original = ((ModelBakerAccessor) modelBaker).CITReborn$getBlockModels();
        Map<ModelIdentifier, GroupableModel> mutable = new HashMap<>(original);
        boolean added = false;

        for (CIT<TypeItem> cit : CONTAINER.loaded) {
            if (cit.type.unbakedAssets == null)
                continue;

            for (Map.Entry<Identifier, UnbakedModel> entry : cit.type.unbakedAssets.entrySet()) {
                Identifier packedId = RebornItemModelIdentifier.pack(entry.getKey());
                ModelIdentifier modelIdentifier = new ModelIdentifier(packedId, "inventory");
                if (!mutable.containsKey(modelIdentifier)) {
                    mutable.put(modelIdentifier, new CitItemGroupableModel(packedId, entry.getValue()));
                    added = true;
                }
            }
        }

        if (added)
            ((ModelBakerAccessor) modelBaker).CITReborn$setBlockModels(mutable);
    }

    @Inject(method = "reloadModels", at = @At("RETURN"), cancellable = true)
    private static void CITReborn$addTypeItemModels(ResourceManager resourceManager, Executor executor,
                                                    CallbackInfoReturnable<CompletableFuture<Map<Identifier, UnbakedModel>>> cir) {
        CompletableFuture<Map<Identifier, UnbakedModel>> original = cir.getReturnValue();
        cir.setReturnValue(original.thenApply(models -> {
            if (!CONTAINER.active())
                return models;

            Map<Identifier, UnbakedModel> mutableModels = new HashMap<>(models);

            info("Loading item CIT models...");
            for (CIT<TypeItem> cit : CONTAINER.loaded) {
                try {
                    cit.type.loadUnbakedAssets(resourceManager);

                    for (Map.Entry<Identifier, UnbakedModel> entry : cit.type.unbakedAssets.entrySet()) {
                        Identifier id = RebornItemModelIdentifier.pack(entry.getKey());
                        mutableModels.put(id, entry.getValue());
                    }
                } catch (Exception e) {
                    CITReborn.logErrorLoading("Errored loading model in " + cit.propertiesIdentifier + " from " + cit.packName);
                    e.printStackTrace();
                }
            }

            TypeItem.GENERATED_SUB_CITS_SEEN.clear();
            return mutableModels;
        }));
    }

    @Inject(method = "upload", at = @At("TAIL"))
    private void CITReborn$linkTypeItemModels(@Coerce Object bakingResult, Profiler profiler, CallbackInfo ci) {
        if (!CONTAINER.active())
            return;

        info("Linking baked models to item CITs...");

        for (CIT<TypeItem> cit : CONTAINER.loaded) {
            for (Identifier modelId : cit.type.unbakedAssets.keySet()) {
                ModelIdentifier modelIdentifier = new ModelIdentifier(RebornItemModelIdentifier.pack(modelId), "inventory");
                BakedModel bakedModel = this.bakedBlockModels.get(modelIdentifier);
                if (bakedModel != null && cit.type.bakedModel == null) {
                    BakedModel resolvedModel = bakedModel;
                    if (cit.type.isTexture()) {
                        Item referenceItem = cit.type.getReferenceItem();
                        if (referenceItem != null) {
                            Identifier itemId = Registries.ITEM.getId(referenceItem);
                            ModelIdentifier referenceId = new ModelIdentifier(itemId, "inventory");
                            BakedModel referenceModel = this.bakedBlockModels.get(referenceId);
                            if (referenceModel != null) {
                                ModelTransformation referenceTransform = referenceModel.getTransformation();
                                resolvedModel = TypeItem.overrideTransformation(resolvedModel, referenceTransform);
                            }
                        }
                    }
                    cit.type.bakedModel = resolvedModel;
                } else if (bakedModel == null) {
                    CITReborn.logWarnLoading("Skipping model for \"" + modelId + "\" in " + cit.propertiesIdentifier + " from " + cit.packName);
                }
            }
            cit.type.unbakedAssets = null;
        }
    }

    private static final class CitItemGroupableModel implements GroupableModel {
        private final Identifier id;
        private final UnbakedModel model;

        private CitItemGroupableModel(Identifier id, UnbakedModel model) {
            this.id = id;
            this.model = model;
        }

        @Override
        public void resolve(ResolvableModel.Resolver resolver) {
            model.resolve(resolver);
        }

        @Override
        public BakedModel bake(Baker baker) {
            return UnbakedModel.bake(model, baker, ModelRotation.X0_Y0);
        }

        @Override
        public Object getEqualityGroup(BlockState state) {
            return id;
        }
    }
}
