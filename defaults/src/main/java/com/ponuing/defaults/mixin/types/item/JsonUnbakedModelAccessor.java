package com.ponuing.defaults.mixin.types.item;

import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelElement;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(JsonUnbakedModel.class)
public interface JsonUnbakedModelAccessor {
    @Accessor("elements")
    List<ModelElement> CITReborn$getElements();

    @Accessor("guiLight")
    UnbakedModel.GuiLight CITReborn$getGuiLight();

    @Accessor("ambientOcclusion")
    Boolean CITReborn$getAmbientOcclusion();

    @Accessor("transformations")
    ModelTransformation CITReborn$getTransformations();

    @Accessor("parentId")
    Identifier CITReborn$getParentId();

    @Accessor("parentId")
    void CITReborn$setParentId(Identifier parentId);

    @Accessor("parent")
    void CITReborn$setParent(net.minecraft.client.render.model.UnbakedModel parent);
}
