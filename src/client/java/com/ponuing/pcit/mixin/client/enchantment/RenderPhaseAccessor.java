package com.ponuing.pcit.mixin.client.enchantment;

import net.minecraft.client.render.RenderPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderPhase.class)
public interface RenderPhaseAccessor {
    @Accessor("ARMOR_ENTITY_GLINT_PROGRAM")
    static RenderPhase.ShaderProgram pcit$armorEntityGlintProgram() {
        throw new RuntimeException();
    }

    @Accessor("TRANSLUCENT_GLINT_PROGRAM")
    static RenderPhase.ShaderProgram pcit$translucentGlintProgram() {
        throw new RuntimeException();
    }

    @Accessor("GLINT_PROGRAM")
    static RenderPhase.ShaderProgram pcit$glintProgram() {
        throw new RuntimeException();
    }

    @Accessor("ENTITY_GLINT_PROGRAM")
    static RenderPhase.ShaderProgram pcit$entityGlintProgram() {
        throw new RuntimeException();
    }

    @Accessor("GLINT_TEXTURING")
    static RenderPhase.Texturing pcit$glintTexturing() {
        throw new RuntimeException();
    }

    @Accessor("ENTITY_GLINT_TEXTURING")
    static RenderPhase.Texturing pcit$entityGlintTexturing() {
        throw new RuntimeException();
    }

    @Accessor("GLINT_TRANSPARENCY")
    static RenderPhase.Transparency pcit$glintTransparency() {
        throw new RuntimeException();
    }

    @Accessor("ADDITIVE_TRANSPARENCY")
    static RenderPhase.Transparency pcit$additiveTransparency() {
        throw new RuntimeException();
    }

    @Accessor("TRANSLUCENT_TRANSPARENCY")
    static RenderPhase.Transparency pcit$translucentTransparency() {
        throw new RuntimeException();
    }

    @Accessor("NO_TRANSPARENCY")
    static RenderPhase.Transparency pcit$noTransparency() {
        throw new RuntimeException();
    }

    @Accessor("DISABLE_CULLING")
    static RenderPhase.Cull pcit$disableCulling() {
        throw new RuntimeException();
    }

    @Accessor("EQUAL_DEPTH_TEST")
    static RenderPhase.DepthTest pcit$equalDepthTest() {
        throw new RuntimeException();
    }

    @Accessor("COLOR_MASK")
    static RenderPhase.WriteMaskState pcit$colorMask() {
        throw new RuntimeException();
    }

    @Accessor("VIEW_OFFSET_Z_LAYERING")
    static RenderPhase.Layering pcit$viewOffsetZLayering() {
        throw new RuntimeException();
    }

    @Accessor("ITEM_ENTITY_TARGET")
    static RenderPhase.Target pcit$itemEntityTarget() {
        throw new RuntimeException();
    }
}
