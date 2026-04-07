package com.ponuing.defaults.mixin.types.item;

import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import com.ponuing.cit.CITCache;
import com.ponuing.defaults.cit.types.TypeItem;

@Mixin(ItemStack.class)
public class ItemStackMixin implements TypeItem.CITCacheItem {
    private final CITCache.Single<TypeItem> CITReborn$cacheTypeItem = new CITCache.Single<>(TypeItem.CONTAINER::getRealTimeCIT);

    @Override
    public CITCache.Single<TypeItem> CITReborn$getCacheTypeItem() {
        return this.CITReborn$cacheTypeItem;
    }
}
