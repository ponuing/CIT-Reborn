package com.ponuing.defaults.mixin.types.elytra;

import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import com.ponuing.cit.CITCache;
import com.ponuing.defaults.cit.types.TypeElytra;

@Mixin(ItemStack.class)
public class ItemStackMixin implements TypeElytra.CITCacheElytra {
    private final CITCache.Single<TypeElytra> CITReborn$cacheTypeElytra = new CITCache.Single<>(TypeElytra.CONTAINER::getRealTimeCIT);

    @Override
    public CITCache.Single<TypeElytra> CITReborn$getCacheTypeElytra() {
        return this.CITReborn$cacheTypeElytra;
    }
}
