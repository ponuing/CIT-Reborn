package com.ponuing.pcustomtextures.client;

import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackInfo;
import net.minecraft.resource.ResourcePackPosition;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackProvider;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.ResourcePackCompatibility;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class CitVirtualResourcePackProvider implements ResourcePackProvider {
    private static final String PACK_ID = "pcustomtextures_cit_virtual";
    private final CitVirtualResourcePack pack = new CitVirtualResourcePack(PACK_ID);

    @Override
    public void register(Consumer<ResourcePackProfile> consumer) {
        ResourcePackInfo info = new ResourcePackInfo(
                PACK_ID,
                Text.literal("pcustomtextures CIT virtual"),
                ResourcePackSource.BUILTIN,
                Optional.empty()
        );
        ResourcePackPosition position = new ResourcePackPosition(true, ResourcePackProfile.InsertionPosition.TOP, false);
        ResourcePackProfile.Metadata metadata = new ResourcePackProfile.Metadata(
                Text.literal("pcustomtextures CIT virtual"),
                ResourcePackCompatibility.COMPATIBLE,
                FeatureSet.empty(),
                List.of()
        );
        ResourcePackProfile profile = new ResourcePackProfile(info, new ResourcePackProfile.PackFactory() {
            @Override
            public ResourcePack open(ResourcePackInfo var1) {
                return pack;
            }

            @Override
            public ResourcePack openWithOverlays(ResourcePackInfo var1, ResourcePackProfile.Metadata metadata) {
                return pack;
            }
        }, metadata, position);
        consumer.accept(profile);
    }
}
