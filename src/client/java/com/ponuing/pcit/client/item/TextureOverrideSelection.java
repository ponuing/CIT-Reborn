package com.ponuing.pcit.client.item;

import net.minecraft.client.texture.Sprite;

import java.util.Map;

public record TextureOverrideSelection(Map<String, Sprite> namedSprites, Sprite defaultSprite) {
    public TextureOverrideSelection {
        namedSprites = namedSprites == null ? Map.of() : namedSprites;
    }
}
