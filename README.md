# pcustomtextures

OptiFine CIT compatibility layer for Fabric 1.21.4 (no generated mod resource pack).

## Supported CIT sources

- `assets/<namespace>/optifine/cit/**/*.properties` in folder and zip packs.

## Supported properties

- `type=item`, `type=armor`
- `items`, `matchItems`
- `model` (for item CIT)
- `texture.<name>` (for armor CIT, including `*_layer_1`, `*_layer_2`, overlays)
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
- Armor render: works through OptiFine-style `texture.<name>` mapping.
- `type=enchantment` and global `cit.properties` visual effect controls are not implemented.

## Reload

- `/pcustomtextures reload`