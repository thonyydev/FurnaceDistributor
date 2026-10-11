# Furnace Distributor

**Minecraft 26.2 · Fabric & NeoForge · Java 25**

**Furnace Distributor** is a Minecraft mod that makes managing multiple furnaces faster, easier, and much less repetitive.

Instead of opening every furnace one by one, select an area and let the mod automatically distribute items or collect finished outputs from all supported furnaces inside it.

This branch targets **Minecraft 26.2**, supporting **Fabric** and **NeoForge**, with optional **Iron Furnaces integration on NeoForge**. For older Minecraft versions, use the corresponding version branch or release.

## Features

- Distribute items across multiple furnaces at once
- Collect finished outputs from several furnaces
- Select furnace areas directly in the world
- Quickly reuse previously selected areas
- Use a dedicated collection selection mode
- See contextual HUD information and visual furnace outlines
- Get action bar feedback without unnecessary chat messages
- Balance distribution based on actual available capacity
- Support vanilla Furnaces, Blast Furnaces, and Smokers
- Optionally support Iron Furnaces on NeoForge
- Work with Iron Furnaces in Normal, Factory, and Generator modes
- Handle multiple Factory input and output slots
- Distribute compatible fuel to furnaces using the Generator Augment
- Validate recipes, fuels, item compatibility, and furnace modes
- Collect stored experience while preserving vanilla furnace behavior
- Safely handle partially filled furnaces and items that cannot be inserted
- Customize keybindings, gameplay settings, and visual options
- Play in singleplayer or multiplayer
- Use built-in translations for **12 languages**

## Supported Furnace Types

### Vanilla

Furnace Distributor supports:

- Furnace
- Blast Furnace
- Smoker

The mod checks whether an item has a valid recipe for each furnace type before inserting it.

### Iron Furnaces (NeoForge)

On **NeoForge**, Furnace Distributor also supports **Iron Furnaces** as an optional integration.

Supported modes:

- **Normal Mode** — distribute processable items and collect finished outputs.
- **Factory Mode** — distribute items across multiple processing inputs and collect from multiple outputs.
- **Generator Mode** — distribute compatible fuels to furnaces equipped with the Generator Augment.

For Minecraft 26.2, the integration targets **Astryxion's Iron Furnaces 1.0.0 for NeoForge 26.2**
([CurseForge file 8573189](https://www.curseforge.com/minecraft/mc-mods/iron-furnaces-fabric-neoforge/files/8573189)).
Its actual Normal, Factory and Generator APIs were inspected and tested. Other forks sharing the
same mod ID are not assumed compatible. **Iron Furnaces is not required** to use Furnace Distributor.

#### Factory Mode

Factory furnaces may process items in multiple slots simultaneously. Furnace Distributor detects available input space, validates compatible items, and collects finished items from the appropriate output slots.

For balancing purposes, a Factory furnace still counts as **one furnace**, rather than gaining extra weight simply because it has multiple processing slots.

#### Generator Mode

Furnaces equipped with the **Generator Augment** can receive fuel through Furnace Distributor.

Fuel validation follows Iron Furnaces' own rules, including compatible fuels provided by other mods. The integration also accounts for fuels that leave a container behind after use.

Generator Mode does not produce processing experience.

## How It Works

### Distributing Items

1. Hold the item you want to distribute.
2. Look at the first furnace and press `R`.
3. Look at another furnace and press `R` again.

The two selected furnaces define the corners of an area. Furnace Distributor finds all supported furnaces inside that area and distributes the held items between them.

The distribution system considers:

- Available space and existing items
- Stack and inventory limits
- Compatible items and fuels
- Valid recipes and furnace types
- The current furnace mode and applicable upgrades

Any items that cannot be inserted **remain with the player**.

Before inserting items, the mod rechecks target furnaces to help prevent invalid transfers if their inventory, mode, or upgrades change during an operation.

### Smart Distribution

Furnace Distributor aims to distribute items evenly while respecting the **actual capacity** of each furnace.

For example, if one furnace has space for only 32 more items and another can accept 64, items that do not fit in the first furnace can be redirected to available space in the second.

A Factory furnace with multiple processing slots is still treated as a single furnace during balancing.

### Reusing an Area

After selecting an area, press **`Sneak + R`** to reuse the previous distribution area without selecting both corners again.

The shortcut uses your configured **Sneak** key, including when it has been remapped.

This is especially useful for permanent smelting setups.

### Collecting Items

Press **`C`** to collect finished items using the normal collection behavior for your selected area.

#### Collection Selection Mode

You can also choose a separate area specifically for collection:

1. Sneak and look at a furnace.
2. Press `C`.
3. Look at another furnace.
4. Press `C` again to confirm the area.

To target a single furnace, confirm while looking at that same furnace again.

Collection selections use **purple outlines** to distinguish them from distribution selections.

### Canceling a Selection

Press **`X`** to cancel the current selection.

Selections are also cleared when appropriate, such as when the player dies, disconnects, changes dimension, or the original selected furnace is no longer valid.

## Default Keybindings

| Action | Default |
| --- | --- |
| Distribute / Select Area | `R` |
| Reuse Previous Area | `Sneak + R` |
| Collect Items | `C` |
| Start Collection Selection | `Sneak + C` |
| Cancel Selection | `X` |

Keybindings can be changed in **Options → Controls → Key Binds → Furnace Distributor**.

## Visual Feedback

### HUD

The contextual HUD can show:

- Current selection mode
- Number of furnaces in the selection
- Selection progress
- Temporary confirmation information

### Outlines

Selected furnaces are highlighted directly in the world. Collection selections use **purple outlines** to distinguish them from distribution selections.

### Action Bar

The action bar provides temporary feedback for successful operations, item counts, remaining items, cancellations, warnings, and errors, keeping the chat cleaner.

## Safe Item Handling

Furnace Distributor includes protections designed to prevent:

- Inserting invalid items or incompatible fuels
- Overwriting incompatible stacks
- Exceeding stack sizes or furnace capacity
- Removing more items from the player than were actually inserted
- Item loss or duplication during normal distribution and collection
- Loading distant, unloaded chunks merely to perform an operation

Items that cannot be inserted stay with the player. The mod also revalidates furnaces when an operation is performed rather than relying only on a visual preview.

## Experience Collection

When collecting finished items, Furnace Distributor awards stored processing experience where appropriate.

- **Vanilla furnaces** preserve their normal Minecraft experience and recipe handling.
- **Supported Iron Furnaces** provide stored processing experience when collecting their outputs.
- **Generator Mode** does not grant processing experience.

Experience is awarded only when the corresponding collection actually occurs.

## Configuration

Furnace Distributor provides configurable gameplay and visual settings, including:

- Validated configuration values and sensible defaults
- Recovery from invalid configuration files
- Preservation of existing settings where possible
- Separate gameplay and visual options

### Fabric

When **Mod Menu** is installed, the configuration screen is available from the mod list. **Mod Menu is optional**.

### NeoForge

The configuration screen is accessible directly from the NeoForge mod list.

## Multiplayer

Furnace Distributor supports multiplayer. Gameplay-changing operations are validated by the server, including checks for:

- Player distance and state
- Selected area size and furnace count
- Loaded chunks
- Furnace accessibility
- Valid item transfers

The mod does **not** force-load unloaded chunks to perform furnace operations. HUD elements, outlines, keybindings, and configuration screens remain client-side.

## Performance

The mod avoids rescanning selected furnace areas every rendered frame. Visual previews reuse recent results briefly, while real operations are validated again when executed.

Search and selection limits help prevent excessively large scans and unnecessary chunk processing.

## Languages

Furnace Distributor includes translations for **12 languages**:

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

This version of Furnace Distributor targets **Minecraft Java Edition 26.2** and **Java 25**.

The following versions were used to build and validate the 26.2 port:

| Component | Fabric | NeoForge |
| --- | --- | --- |
| Minecraft | `26.2` | `26.2` |
| Java | `25` | `25` |
| Mod loader | Fabric Loader `0.19.5` | NeoForge `26.2.0.90` |
| Architectury API | `21.1.12` | `21.1.12` |
| Fabric API | `0.161.0+26.2` | Not applicable |

**Optional integrations:**

- **Fabric:** Mod Menu `20.0.3` provides access to the settings screen from the mod list.
- **NeoForge:** [Astryxion's Iron Furnaces 1.0.0 for Minecraft 26.2](https://www.curseforge.com/minecraft/mc-mods/iron-furnaces-fabric-neoforge/files/8573189).

Iron Furnaces is **not required**, and support for other forks is not guaranteed. Iron Furnaces integration is **NeoForge-only**.

## Installation

1. Use **Minecraft Java Edition 26.2** with **Java 25**.
2. Install the matching **Fabric** or **NeoForge** loader.
3. Install **Architectury API** for your loader.
4. If using Fabric, also install **Fabric API**.
5. Download the Furnace Distributor JAR for your loader and Minecraft 26.2.
6. Optionally add **Mod Menu** on Fabric or the supported **Iron Furnaces** version on NeoForge.
7. Place the mod and its required dependencies in the Minecraft instance's `mods` folder.
8. Launch Minecraft.

**Example release filenames (version 3.0.0):**

```text
furnacedistributor-fabric-3.0.0+26.2.jar
furnacedistributor-neoforge-3.0.0+26.2.jar
```

Do not install both loader variants in the same instance. Development (`-dev`) and source (`-sources`) JARs are not intended for normal installation.

## Building From Source

Install JDK 25. Set `JAVA_HOME` to your installation if necessary; machine-specific
Java paths belong in your environment or user Gradle configuration, not this repository.

Clone the repository:

```bash
git clone https://github.com/thonyydev/FurnaceDistributor.git
cd FurnaceDistributor
```

### Windows

```powershell
.\gradlew.bat clean build
```

### Linux / macOS

```bash
./gradlew build
```

Distributable JARs are generated in:

```text
fabric/build/libs/furnacedistributor-fabric-3.0.0+26.2.jar
neoforge/build/libs/furnacedistributor-neoforge-3.0.0+26.2.jar
```

The filenames are derived from the mod version and Minecraft version in the Gradle properties. If you change `mod_version` or `minecraft_version`, the output filenames change accordingly.

## Reporting Issues

If you find a bug, [open an issue](https://github.com/thonyydev/FurnaceDistributor/issues) and include:

- Minecraft version
- Furnace Distributor version
- Mod loader and version
- Singleplayer or multiplayer
- Steps to reproduce the problem
- Relevant logs or crash reports

For distribution or collection problems, also provide:

- The item or fuel and its quantity
- Furnace types and modes (Normal, Factory, or Generator)
- Any Iron Furnaces augments or upgrades in use
- Items already inside the furnaces
- The selected area
- Other relevant mods, especially those adding fuels or recipes

## Contributing

Contributions are welcome, including:

- Bug fixes
- Translations
- Performance improvements
- Mod compatibility improvements
- Documentation
- Quality-of-life features

For larger changes, consider opening an issue before starting implementation.

## License

Furnace Distributor is licensed under the **MIT License**. See [LICENSE](LICENSE) for more information.

---

Make large furnace setups easier to manage and spend less time opening furnaces one by one.

If Furnace Distributor helps your smelting setup, consider giving the project a ⭐ on GitHub!
