package com.ponuing.defaults.mixin.types.armor;

import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import com.ponuing.cit.CITCache;
import com.ponuing.defaults.cit.types.TypeArmor;

@Mixin(ItemStack.class)
public class ItemStackMixin implements TypeArmor.CITCacheArmor {
    private final CITCache.Single<TypeArmor> CITReborn$cacheTypeArmor = new CITCache.Single<>(TypeArmor.CONTAINER::getRealTimeCIT);

    @Override
    public CITCache.Single<TypeArmor> CITReborn$getCacheTypeArmor() {
        return this.CITReborn$cacheTypeArmor;
    }
}
