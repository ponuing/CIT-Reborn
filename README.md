# CIT Reborn

[![Modrinth Downloads](https://img.shields.io/modrinth/dt/3xbZC1Fh?logo=Modrinth&label=Modrinth)](https://modrinth.com/mod/cit-reborn-mod) [![CurseForge Downloads](https://img.shields.io/curseforge/dt/1485026?logo=CurseForge&label=CurseForge)](https://www.curseforge.com/minecraft/mc-mods/cit-reborn)

CIT Reborn is a client-side mod that adds support for OptiFine's Custom Item Textures (CIT) format to the Minecraft fabric version.

This mod allows you to dynamically change the appearance of items, armor, elytra, and enchantment visual effects. Textures and models can be customized based on specific conditions, such as renaming an item at an anvil, durability level, stack size, the presence of certain enchantments, hidden NBT tags, and new data components. CIT Reborn provides a fabric alternative to OptiFine for working with custom textures.

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

### I would also like to thank [SHsuperCM](https://github.com/SHsuperCM/) for his many years of development and support of the [CIT Resewn mod](https://modrinth.com/mod/cit-resewn). The mod was created using and incorporating the [CIT Resewn source code](https://github.com/SHsuperCM/CITResewn).
