package com.ponuing.defaults.mixin.types.enchantment;

import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.ponuing.cit.CITCache;
import com.ponuing.defaults.cit.types.TypeEnchantment;

@Mixin(ItemStack.class)
public class ItemStackMixin implements TypeEnchantment.CITCacheEnchantment {
    private final CITCache.MultiList<TypeEnchantment> CITReborn$cacheTypeEnchantment = new CITCache.MultiList<>(TypeEnchantment.CONTAINER::getRealTimeCIT);

    @Override
    public CITCache.MultiList<TypeEnchantment> CITReborn$getCacheTypeEnchantment() {
        return this.CITReborn$cacheTypeEnchantment;
    }

    @Inject(method = "hasGlint", cancellable = true, at = @At("HEAD"))
    private void CITReborn$enchantment$disableDefaultGlint(CallbackInfoReturnable<Boolean> cir) {
        if (TypeEnchantment.CONTAINER.shouldNotApplyDefaultGlint())
            cir.setReturnValue(false);
    }
}
