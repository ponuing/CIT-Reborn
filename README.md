# P-CIT

OptiFine CIT compatibility layer for Fabric 1.21.4.

## Supported CIT sources

- `assets/<namespace>/optifine/cit/**/*.properties` in folder and zip packs.

## Supported properties

- `type=item`, `type=armor`, `type=elytra`, `type=enchantment`
- `items`, `matchItems`
- `model`, `model.<name>` (item)
- `tile`, `tiles`, `texture`, `texture.<name>` (item)
- `texture.<name>` (armor)
- `texture`, `blend`, `speed`, `rotation`, `layer`, `duration` (enchantment)
- `weight`
- `damage`, `damaged`, `stackSize`
- `enchantments`, `enchantmentIDs`, `enchantmentLevels`
- `potion`, `unbreakable`
- `nbt.*`, `components.*` (alias: `component.*`)

## Match modes

- exact
- `pattern:` / `ipattern:`
- `regex:` / `iregex:`

## Notes

- Item render: inventory, hand, world item entity.
- Item `tile/tiles` uses a flat quad renderer (no 3D model geometry).
- Armor render: uses OptiFine-style `texture.<name>` mapping.
- `type=enchantment` supports glint texture overrides; animation settings currently use vanilla timing.
- Global `cit.properties` effects are not implemented.
