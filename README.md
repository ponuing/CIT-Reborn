# pcustomtextures

OptiFine CIT compatibility layer for Fabric 1.21.4 (no generated mod resource pack).

## Supported CIT sources

- `assets/<namespace>/optifine/cit/**/*.properties` in folder and zip packs.

## Supported properties

- `type=item`, `type=armor`
- `items`, `matchItems`
- `model` (item)
- `tile`, `tiles`, `texture` (item)
- `texture.<name>` (armor)
- `weight`
- `damage`, `damaged`, `stackSize`
- `enchantments`, `enchantmentIDs`, `enchantmentLevels`
- `potion`, `unbreakable`
- `nbt.*`, `components.*`

## Match modes

- exact
- `pattern:` / `ipattern:`
- `regex:` / `iregex:`

## Notes

- Item render: inventory, hand, world item entity.
- Item `tile/tiles` uses a flat quad renderer (no 3D model geometry).
- Armor render: uses OptiFine-style `texture.<name>` mapping.
- `type=enchantment` and global `cit.properties` effects are not implemented.

## Reload

- `/pcustomtextures reload`