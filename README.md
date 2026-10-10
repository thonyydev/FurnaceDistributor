# Furnace Distributor

**Furnace Distributor** is a Minecraft mod that makes managing multiple furnaces faster and easier.

Instead of opening every furnace one by one, you can select an area and let the mod distribute items or collect finished outputs automatically.

The mod supports **Fabric** and **NeoForge**.

## Features

- Distribute items evenly across multiple furnaces
- Collect finished items from several furnaces at once
- Select furnace areas directly in the world
- Reuse previously selected areas
- Dedicated collection selection mode
- Visual furnace outlines
- Contextual HUD
- Action bar feedback instead of unnecessary chat messages
- Vanilla experience and recipe handling when collecting
- Smart distribution based on available furnace capacity
- Recipe validation for different furnace types
- Configurable keybindings
- Gameplay and visual settings
- Multiplayer support
- Built-in translations for multiple languages

## Supported Furnace Types

Furnace Distributor supports:

- Furnace
- Blast Furnace
- Smoker

The mod automatically checks whether the item being distributed has a valid recipe for each furnace type.

## How It Works

### Distributing Items

Hold the item you want to distribute and look at the first furnace.

Press:

```text
R
```

Then look at another furnace and press `R` again.

The two selected furnaces define the corners of an area.

Furnace Distributor finds all supported furnaces inside that area and distributes the items between them.


### Smart Distribution

Distribution takes into account:

- available space;
- items already inside furnaces;
- stack limits;
- item compatibility;
- valid recipes;
- furnace type.

Furnaces with less available space are filled appropriately before the remaining items are redistributed.

Any items that cannot be inserted remain with the player.

## Reusing an Area

After selecting an area, you can quickly reuse it without selecting both corners again.

Press:

```text
Sneak + R
```

This is especially useful for permanent furnace setups.

## Collecting Items

Press:

```text
C
```

to collect finished items using the normal collection behavior.

### Collection Selection Mode

You can also select a separate area specifically for collection.

While sneaking:

1. Look at a furnace.
2. Press `C`.
3. Look at another furnace.
4. Press `C` again to confirm the area.

To collect from only one furnace, confirm while looking at the same furnace again.

Collection selection uses **purple outlines** so it can easily be distinguished from normal distribution selection.

## Experience

Collecting furnace outputs also preserves vanilla furnace behavior.

When applicable, the player receives:

- stored furnace experience;
- recipe-related progress;
- normal furnace collection rewards.

Experience is only awarded when items are actually collected.

## Canceling a Selection

Press:

```text
X
```

to cancel the current selection.

Selections are also automatically cleared when appropriate, such as when:

- the player dies;
- disconnects;
- changes dimension;
- the original selected furnace is no longer valid.

## Default Keybindings

| Action | Default |
| --- | --- |
| Distribute / Select Area | `R` |
| Reuse Saved Area | `Sneak + R` |
| Collect Items | `C` |
| Start Collection Selection | `Sneak + C` |
| Cancel Selection | `X` |

Keybindings can be changed in:

```text
Options → Controls → Key Binds → Furnace Distributor
```

## Visual Feedback

Furnace Distributor uses different types of feedback depending on the situation.

### HUD

The contextual HUD can display:

- current selection mode;
- selected furnace count;
- selection progress;
- temporary confirmation information.

### Outlines

Selected furnaces are highlighted directly in the world.

Collection selections use purple outlines to distinguish them from distribution selections.

### Action Bar

The action bar is used for temporary feedback such as:

- successful actions;
- item quantities;
- remaining items;
- cancellations;
- warnings;
- operational errors.

This keeps the Minecraft chat much cleaner.

## Configuration

Furnace Distributor includes configurable gameplay and visual options.

The configuration system supports:

- validated values;
- default settings;
- recovery from invalid configuration files;
- preservation of existing settings where possible;
- separate visual and gameplay options.

### Fabric

When **Mod Menu** is installed, the configuration screen can be opened directly from the mod list.

Mod Menu is optional.

### NeoForge

The configuration screen is available directly from the NeoForge mod list.

## Multiplayer

Furnace Distributor supports multiplayer.

Gameplay-changing actions are validated by the server, including checks for:

- player distance;
- selected area size;
- number of furnaces;
- loaded chunks;
- accessible furnaces;
- player state.

HUD elements, outlines, keybindings, and configuration screens remain client-side.

## Performance

The mod avoids repeatedly scanning furnace areas every rendered frame.

Visual previews reuse recent results for a short period, while actual actions are validated again when confirmed.

Searches are limited to prevent excessively large selections or unnecessary chunk processing.

## Languages

Furnace Distributor currently supports:

- 🇺🇸 English
- 🇧🇷 Português do Brasil
- 🇪🇸 Español
- 🇫🇷 Français
- 🇩🇪 Deutsch
- 🇮🇹 Italiano
- 🇵🇱 Polski
- 🇷🇺 Русский
- 🇨🇳 简体中文
- 🇹🇼 繁體中文
- 🇯🇵 日本語
- 🇰🇷 한국어

## Requirements

### Fabric

- Fabric Loader
- Fabric API
- Architectury API

Optional:

- Mod Menu

### NeoForge

- NeoForge
- Architectury API

## Installation

1. Install Fabric or NeoForge for the supported Minecraft version.
2. Install Architectury API.
3. If using Fabric, install Fabric API.
4. Download the correct Furnace Distributor version for your loader.
5. Place the `.jar` files inside the Minecraft `mods` folder.
6. Launch Minecraft.

## Building From Source

Clone the repository:

```bash
git clone https://github.com/thonyydev/FurnaceDistributor.git
cd FurnaceDistributor
```

### Windows

```powershell
.\gradlew build
```

### Linux / macOS

```bash
./gradlew build
```

The generated Fabric and NeoForge jars will be available inside their respective build directories.

## Reporting Issues

If you find a bug, open an issue and include:

- Minecraft version;
- Furnace Distributor version;
- mod loader;
- singleplayer or multiplayer;
- steps to reproduce;
- relevant logs or crash reports.

For distribution issues, also include information about:

- the item being distributed;
- the amount of items;
- furnace types;
- items already inside the furnaces;
- the selected area.

## Contributing

Contributions are welcome, including:

- bug fixes;
- translations;
- performance improvements;
- compatibility improvements;
- documentation;
- quality-of-life features.

For larger changes, consider opening an issue before starting implementation.

## License

Furnace Distributor is licensed under the **MIT License**.

See the [LICENSE](LICENSE) file for more information.

---

If Furnace Distributor makes your smelting setups easier to manage, consider giving the project a ⭐ on GitHub.
