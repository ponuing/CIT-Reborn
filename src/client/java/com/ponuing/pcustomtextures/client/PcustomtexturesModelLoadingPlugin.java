package com.ponuing.pcustomtextures.client;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.model.loading.v1.UnbakedModelDeserializer;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.util.Identifier;
import com.ponuing.pcustomtextures.Pcustomtextures;

import java.io.StringReader;
import java.util.Map;

public final class PcustomtexturesModelLoadingPlugin {
    private PcustomtexturesModelLoadingPlugin() {
    }

    public static void register() {
        ModelLoadingPlugin.register(PcustomtexturesModelLoadingPlugin::initialize);
    }

    private static void initialize(ModelLoadingPlugin.Context context) {
        Map<Identifier, NbtRenderOverrideResolver.GeneratedModelDef> models = NbtRenderOverrideResolver.getGeneratedItemModels();
        if (models.isEmpty()) {
            return;
        }

        context.addModels(models.keySet());
        context.modifyModelOnLoad().register(ModelModifier.OVERRIDE_PHASE, (model, ctx) -> {
            Identifier id = ctx.id();
            NbtRenderOverrideResolver.GeneratedModelDef def = models.get(id);
            if (def == null) {
                return model;
            }

            String parentString = def.parentModelId().toString();
            String textureString = toModelTextureString(def.virtualTextureId());
            String json = "{\"parent\":\"" + parentString + "\",\"textures\":{\"layer0\":\"" + textureString + "\"}}";
            //Pcustomtextures.LOGGER.info("[pcustomtextures][model] build model {} parent={} texture={}", id, parentString, textureString);
            UnbakedModel parsed = UnbakedModelDeserializer.deserialize(new StringReader(json));
            return parsed != null ? parsed : model;
        });
    }

    private static String toModelTextureString(Identifier textureId) {
        String path = textureId.getPath();
        if (path.startsWith("textures/")) {
            path = path.substring("textures/".length());
        }
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - 4);
        }
        return textureId.getNamespace() + ":" + path;
    }
}
