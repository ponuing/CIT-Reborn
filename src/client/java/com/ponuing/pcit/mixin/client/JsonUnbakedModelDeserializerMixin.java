package com.ponuing.pcit.mixin.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Type;
import java.util.Map;

@Mixin(targets = "net.minecraft.client.render.model.json.JsonUnbakedModel$Deserializer")
public class JsonUnbakedModelDeserializerMixin {
    @Inject(
            method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/client/render/model/json/JsonUnbakedModel;",
            at = @At("HEAD")
    )
    private void pcit$scaleTextureSize(JsonElement json, Type type, com.google.gson.JsonDeserializationContext ctx, CallbackInfoReturnable<JsonUnbakedModel> cir) {
        if (json == null || !json.isJsonObject()) {
            return;
        }
        JsonObject obj = json.getAsJsonObject();
        JsonElement sizeEl = obj.get("texture_size");
        if (sizeEl == null || !sizeEl.isJsonArray() || sizeEl.getAsJsonArray().size() < 2) {
            return;
        }
        float width = sizeEl.getAsJsonArray().get(0).getAsFloat();
        float height = sizeEl.getAsJsonArray().get(1).getAsFloat();
        if (width <= 0.0f || height <= 0.0f) {
            return;
        }
        float scaleU = 16.0f / width;
        float scaleV = 16.0f / height;
        if (Math.abs(scaleU - 1.0f) < 0.0001f && Math.abs(scaleV - 1.0f) < 0.0001f) {
            return;
        }
        JsonElement elementsEl = obj.get("elements");
        if (elementsEl == null || !elementsEl.isJsonArray()) {
            return;
        }
        float maxUv = 0.0f;
        for (JsonElement elementEl : elementsEl.getAsJsonArray()) {
            if (!elementEl.isJsonObject()) {
                continue;
            }
            JsonObject elementObj = elementEl.getAsJsonObject();
            JsonElement facesEl = elementObj.get("faces");
            if (facesEl == null || !facesEl.isJsonObject()) {
                continue;
            }
            JsonObject faces = facesEl.getAsJsonObject();
            for (Map.Entry<String, JsonElement> faceEntry : faces.entrySet()) {
                JsonElement faceEl = faceEntry.getValue();
                if (faceEl == null || !faceEl.isJsonObject()) {
                    continue;
                }
                JsonObject faceObj = faceEl.getAsJsonObject();
                JsonElement uvEl = faceObj.get("uv");
                if (uvEl == null || !uvEl.isJsonArray() || uvEl.getAsJsonArray().size() < 4) {
                    continue;
                }
                JsonArray uv = uvEl.getAsJsonArray();
                for (int i = 0; i < 4; i++) {
                    maxUv = Math.max(maxUv, uv.get(i).getAsFloat());
                }
            }
        }
        if (maxUv <= 16.001f) {
            return;
        }
        for (JsonElement elementEl : elementsEl.getAsJsonArray()) {
            if (!elementEl.isJsonObject()) {
                continue;
            }
            JsonObject elementObj = elementEl.getAsJsonObject();
            JsonElement facesEl = elementObj.get("faces");
            if (facesEl == null || !facesEl.isJsonObject()) {
                continue;
            }
            JsonObject faces = facesEl.getAsJsonObject();
            for (Map.Entry<String, JsonElement> faceEntry : faces.entrySet()) {
                JsonElement faceEl = faceEntry.getValue();
                if (faceEl == null || !faceEl.isJsonObject()) {
                    continue;
                }
                JsonObject faceObj = faceEl.getAsJsonObject();
                JsonElement uvEl = faceObj.get("uv");
                if (uvEl == null || !uvEl.isJsonArray() || uvEl.getAsJsonArray().size() < 4) {
                    continue;
                }
                JsonArray uv = uvEl.getAsJsonArray();
                float u0 = uv.get(0).getAsFloat() * scaleU;
                float v0 = uv.get(1).getAsFloat() * scaleV;
                float u1 = uv.get(2).getAsFloat() * scaleU;
                float v1 = uv.get(3).getAsFloat() * scaleV;
                JsonArray scaled = new JsonArray();
                scaled.add(u0);
                scaled.add(v0);
                scaled.add(u1);
                scaled.add(v1);
                faceObj.add("uv", scaled);
            }
        }
    }
}
