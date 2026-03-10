package com.ponuing.pcustomtextures.client;

import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackInfo;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.metadata.PackResourceMetadata;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.SharedConstants;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CitVirtualResourcePack implements ResourcePack {
    private static volatile Map<Identifier, byte[]> VIRTUAL_TO_BYTES = Map.of();
    private static final java.util.concurrent.atomic.AtomicInteger OPEN_LOG_COUNT = new java.util.concurrent.atomic.AtomicInteger(0);
    private final ResourcePackInfo info;

    public CitVirtualResourcePack(String id) {
        this.info = new ResourcePackInfo(id, Text.literal("pcustomtextures CIT virtual"), ResourcePackSource.BUILTIN, java.util.Optional.empty());
    }

    public static void updateMappings(Map<Identifier, byte[]> mappings) {
        VIRTUAL_TO_BYTES = mappings != null ? mappings : Map.of();
        //com.ponuing.pcustomtextures.Pcustomtextures.LOGGER.info("[pcustomtextures][model] virtual texture count={}", VIRTUAL_TO_BYTES.size());
    }

    @Override
    public InputSupplier<InputStream> openRoot(String... segments) {
        return null;
    }

    @Override
    public InputSupplier<InputStream> open(ResourceType type, Identifier id) {
        if (type != ResourceType.CLIENT_RESOURCES) {
            return null;
        }
        byte[] bytes = VIRTUAL_TO_BYTES.get(id);
        if (bytes == null) {
            return null;
        }
        int count = OPEN_LOG_COUNT.getAndIncrement();
        if (count < 20) {
            //com.ponuing.pcustomtextures.Pcustomtextures.LOGGER.info("[pcustomtextures][model] virtual open {} bytes={}", id, bytes.length);
        }
        return () -> new ByteArrayInputStream(bytes);
    }

    @Override
    public void findResources(ResourceType type, String namespace, String prefix, ResourcePack.ResultConsumer consumer) {
        if (type != ResourceType.CLIENT_RESOURCES) {
            return;
        }
        String normalizedPrefix = prefix == null ? "" : prefix;
        boolean prefixHasTextures = normalizedPrefix.startsWith("textures/");
        boolean prefixIsItems = normalizedPrefix.startsWith("items");
        for (Identifier id : VIRTUAL_TO_BYTES.keySet()) {
            if (!id.getNamespace().equals(namespace)) {
                continue;
            }
            String path = id.getPath();
            boolean matches = path.startsWith(normalizedPrefix);
            if (!matches && !prefixHasTextures) {
                matches = path.startsWith("textures/" + normalizedPrefix);
            }
            if (!matches && prefixIsItems) {
                String suffix = normalizedPrefix.length() > "items".length()
                        ? normalizedPrefix.substring("items".length())
                        : "";
                matches = path.startsWith("textures/item" + suffix);
            }
            if (!matches) {
                continue;
            }
            consumer.accept(id, open(type, id));
        }
        //com.ponuing.pcustomtextures.Pcustomtextures.LOGGER.info("[pcustomtextures][model] findResources ns={} prefix={} count={}", namespace, prefix, VIRTUAL_TO_BYTES.size());
    }

    @Override
    public Set<String> getNamespaces(ResourceType type) {
        if (type != ResourceType.CLIENT_RESOURCES) {
            return Collections.emptySet();
        }
        Set<String> namespaces = new HashSet<>();
        for (Identifier id : VIRTUAL_TO_BYTES.keySet()) {
            namespaces.add(id.getNamespace());
        }
        return namespaces;
    }

    @Override
    public <T> T parseMetadata(ResourceMetadataSerializer<T> serializer) {
        if (serializer == PackResourceMetadata.SERIALIZER) {
            PackResourceMetadata meta = new PackResourceMetadata(
                    Text.literal("pcustomtextures CIT virtual"),
                    SharedConstants.RESOURCE_PACK_VERSION,
                    Optional.empty()
            );
            @SuppressWarnings("unchecked")
            T cast = (T) meta;
            return cast;
        }
        return null;
    }

    @Override
    public ResourcePackInfo getInfo() {
        return info;
    }

    @Override
    public void close() {
    }
}
