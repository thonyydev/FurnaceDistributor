# Furnace Distributor

**Spend less time managing furnaces and more time playing Minecraft.**

[![Version](https://img.shields.io/badge/Version-4.0.0-2374AB?style=flat-square)](https://github.com/thonyydev/FurnaceDistributor/releases)
![Minecraft](https://img.shields.io/badge/Minecraft-26.2-62B47A?style=flat-square)
![Loaders](https://img.shields.io/badge/Loaders-Fabric%20%7C%20NeoForge-7E64A5?style=flat-square)
![Java](https://img.shields.io/badge/Java-25-DB7931?style=flat-square)
![Languages](https://img.shields.io/badge/Languages-12-39988B?style=flat-square)
[![License](https://img.shields.io/badge/License-MIT-555555?style=flat-square)](LICENSE)

[![CurseForge](https://img.shields.io/badge/Download-CurseForge-F16436?style=for-the-badge&logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/furnace-distributor)
[![GitHub Releases](https://img.shields.io/badge/Download-GitHub%20Releases-24292F?style=for-the-badge&logo=github&logoColor=white)](https://github.com/thonyydev/FurnaceDistributor/releases)

**Furnace Distributor** is a quality-of-life Minecraft mod that makes managing multiple furnaces faster, easier, and much less repetitive. Instead of opening every furnace one by one, select an area and let the mod distribute items or collect finished outputs from all supported furnaces inside it.

It supports **Fabric** and **NeoForge**, with optional **Iron Furnaces integration on NeoForge**.

> **This README documents Furnace Distributor 4.0.0 for Minecraft 26.2.** For older Minecraft versions, use the matching release or version branch.

## See It in Action

Four quick demonstrations of the main features. All animations are hosted on CurseForge, so no video files need to be stored in the Git repository.

| **Distribute Items** | **Bulk Inventory Distribution · NEW in 4.0.0** |
|:---:|:---:|
| ![Selecting a furnace area and distributing items](https://media.forgecdn.net/attachments/2041/372/distributeitems-webp.webp) | ![Selecting multiple inventory stacks for bulk distribution](https://media.forgecdn.net/attachments/2041/374/bulkdistribution-webp.webp) |
| **Reuse a Saved Area** | **Collect Finished Items** |
| ![Reusing a previously selected furnace area](https://media.forgecdn.net/attachments/2041/373/distributesavedarea-webp.webp) | ![Collecting finished items directly into the player inventory](https://media.forgecdn.net/attachments/2041/375/collectitems-webp.webp) |

> **About collection:** Press `C` to collect finished outputs from the saved area. Items transfer directly into your inventory rather than flying through the world, so the change happens almost instantly in the demonstration.

## What's New in 4.0.0

- **Bulk Inventory Distribution** — Select multiple inventory stacks, including different materials and fuels, and distribute them in one operation using `B`.
- **Select Matching** — Quickly include equivalent stacks in the Bulk Distribution screen.
- **Redesigned HUD** — A more compact layout with smoother animations, clearer counts, and better readability.
- **Better Feedback** — See how many items were transferred, how many furnaces received them, and why others were skipped.
- **Contextual Tips** — Helpful prompts introduce useful shortcuts, including area collection with `C`.
- **More Control** — Configure UI animations and contextual tips; server owners can manage bulk distribution limits.
- **Quality-of-Life Fixes** — Improvements to selection handling, item transfers, settings, and optional Iron Furnaces compatibility.

See the full [changelog](CHANGELOG.md) for details.

## Features

- Distribute items and fuel across multiple furnaces at once.
- Select **multiple inventory stacks** and distribute them in a single bulk operation.
- Collect finished outputs from several furnaces, including stored experience when applicable.
- Select areas directly in the world, **reuse saved areas**, or set up a separate collection area.
- Use a contextual HUD, colored furnace outlines, and clear action bar messages without excessive chat spam.
- Balance items according to **actual available capacity**, recipes, compatible fuels, and existing inventory contents.
- Support vanilla **Furnaces, Blast Furnaces, and Smokers**.
- Optionally support **Iron Furnaces on NeoForge**, including Normal, Factory, and Generator modes, multiple Factory inputs/outputs, and Generator fuel distribution.
- Keep incompatible or excess items with the player, with safeguards against invalid transfers.
- Customize keybindings, gameplay settings, and visual options.
- Play in singleplayer or multiplayer, with server-side validation of gameplay actions.
- Use built-in translations for **12 languages**.

## How It Works

### Distributing Items

1. Hold the item you want to distribute.
2. Look at the first furnace and press `R`.
3. Look at another furnace and press `R` again.

The two selected furnaces define the corners of an area. Furnace Distributor finds the supported furnaces inside it and distributes your held items where they can be accepted.

Distribution considers available space, existing items, stack limits, compatible items and fuels, valid recipes, furnace types, and any relevant furnace modes or upgrades. Items that cannot be inserted **remain with the player**.

Before an operation, the mod rechecks target furnaces in case their contents, modes, or upgrades have changed since the preview.

### Bulk Inventory Distribution · New in 4.0.0

1. Select the furnace area you want to use.
2. Press `B` to open the **Bulk Distribution** screen.
3. Choose individual inventory stacks or use **Select Matching** for equivalent stacks.
4. Review your selection and confirm to distribute the items.

You can distribute different materials and fuels together. Each item is inserted only where it is compatible and space is available. Items that cannot be transferred stay in your inventory. Opening or cancelling the screen does not move items.

### Smart Distribution

Furnace Distributor aims to distribute items evenly while respecting each furnace's **actual capacity**. If one furnace has room for only 32 more items while another can accept 64, remaining items can be redirected to available space instead of being lost.

A Factory furnace with multiple processing slots still counts as **one furnace** during balancing.

### Reusing a Saved Area

After selecting an area, press **`Sneak + R`** to reuse it without selecting both corners again. This respects your configured Sneak key, even when it has been remapped.

### Collecting Finished Items

Press **`C`** to collect finished outputs from the saved area. Collected items go **directly into your inventory**; they do not appear as flying item entities.

To create a dedicated collection area:

1. Sneak while looking at a furnace and press `C`.
2. Look at another furnace and press `C` again to confirm.
3. To select just one furnace, confirm while looking at the same furnace again.

Collection selections use **purple outlines** to distinguish them from distribution selections.

### Canceling a Selection

Press `X` to cancel your current selection. Selections are also cleared when appropriate, such as when you die, disconnect, change dimensions, or the original selected furnace becomes invalid.

## Default Keybindings

| Action | Default |
| --- | --- |
| Distribute / Select Area | `R` |
| Reuse Previous Area | `Sneak + R` |
| **Bulk Inventory Distribution** | **`B`** |
| Collect Items | `C` |
| Start Collection Selection | `Sneak + C` |
| Cancel Selection | `X` |

All keybindings can be changed in **Options → Controls → Key Binds → Furnace Distributor**.

## Supported Furnace Types

### Vanilla

- Furnace
- Blast Furnace
- Smoker

The mod checks whether an item has a valid recipe for the corresponding furnace type before inserting it.

### Iron Furnaces · NeoForge Only

On **NeoForge**, Furnace Distributor optionally integrates with compatible versions of **Iron Furnaces**:

| Mode | Supported behavior |
| --- | --- |
| **Normal** | Distribute processable items and collect finished outputs. |
| **Factory** | Distribute to multiple input slots and collect from multiple output slots. |
| **Generator** | Distribute compatible fuel to furnaces equipped with the Generator Augment. |

**Factory Mode:** The mod detects available input space, validates compatible items, and collects finished items from the appropriate outputs. Each Factory furnace counts as one furnace for balancing purposes, regardless of its number of processing slots.

**Generator Mode:** Fuel validation follows Iron Furnaces' own rules, including compatible fuels added by other mods. The integration also accounts for fuels that leave containers behind after use. Generator Mode does not produce processing experience.

**Iron Furnaces is optional** and is not required for vanilla furnace support. Compatibility with unrelated forks is not guaranteed.

## Visual Feedback

### Contextual HUD

The compact HUD provides selection mode, furnace counts, selection progress, temporary confirmations, and helpful control tips. Version 4.0.0 also adds smooth transitions, improved layout scaling, and options to disable tips or animations.

### Furnace Outlines

Selected furnaces are outlined in the world. **Purple** outlines distinguish collection selections from distribution selections. Rendering is optimized to avoid unnecessary drawing of off-screen previews.

### Action Bar and Operation Results

Temporary messages summarize successful operations, transferred item counts, furnaces used or skipped, remaining items, cancellations, warnings, and errors — without unnecessarily filling the chat.

## Safe Item Handling

Furnace Distributor includes safeguards designed to prevent:

- Inserting invalid items or incompatible fuels.
- Overwriting incompatible stacks or exceeding stack and furnace limits.
- Removing more items from a player than were actually inserted.
- Item loss or duplication during normal distribution and collection.
- Loading distant, unloaded chunks merely to perform an operation.

Items that cannot be inserted stay with the player. Operations validate the current state of their target furnaces instead of relying only on an earlier visual preview.

## Experience Collection

When collecting finished items, Furnace Distributor awards stored processing experience where appropriate:

- **Vanilla furnaces:** Preserve normal Minecraft experience and recipe handling.
- **Supported Iron Furnaces:** Provide stored processing experience when collecting their outputs.
- **Generator Mode:** Does not grant processing experience.

Experience is awarded only when the corresponding collection actually occurs.

## Configuration

Gameplay and visual settings include configurable keybindings, validated values and defaults, recovery from invalid configuration files, and preservation of existing preferences whenever possible.

- **Fabric:** Install **Mod Menu** (optional) to access the configuration screen through the mod list.
- **NeoForge:** Open the configuration screen directly from the mod list.

The 4.0.0 interface also offers **Show Contextual Tips** and **Enable UI Animations** options. Server owners can configure bulk distribution availability and selection limits.

## Multiplayer

Furnace Distributor supports multiplayer. Gameplay-changing actions are validated by the server, including checks for player distance and state, selected area size, furnace count, loaded chunks, furnace accessibility, and valid item transfers.

The mod does **not** force-load unloaded chunks to perform operations. HUD elements, outlines, keybindings, and configuration screens remain client-side.

## Performance

The mod avoids rescanning selected furnace areas on every rendered frame. Visual previews briefly reuse recent results, while actual operations are validated again when executed. Search, selection, and preview limits help prevent unnecessary processing and visual clutter.

## Languages

Built-in translations are available for **12 languages**:

| | | |
| --- | --- | --- |
| 🇺🇸 English | 🇧🇷 Português do Brasil | 🇪🇸 Español |
| 🇫🇷 Français | 🇩🇪 Deutsch | 🇮🇹 Italiano |
| 🇵🇱 Polski | 🇷🇺 Русский | 🇨🇳 简体中文 |
| 🇹🇼 繁體中文 | 🇯🇵 日本語 | 🇰🇷 한국어 |

## Requirements

The **4.0.0 release for Minecraft 26.2** requires **Java 25**.

| Dependency | Fabric | NeoForge |
| --- | --- | --- |
| Minecraft | `26.2` | `26.2` |
| Java | `25` | `25` |
| Mod loader | Fabric Loader | NeoForge |
| Architectury API | Required | Required |
| Fabric API | Required | — |
| Mod Menu | Optional | — |
| Iron Furnaces | — | Optional, compatible 26.2 build |

Install the dependency versions matching your Minecraft version and loader. Iron Furnaces integration is available **only on NeoForge**.

## Installation

1. Install **Minecraft Java Edition 26.2** and use **Java 25**.
2. Install the matching **Fabric** or **NeoForge** loader.
3. Install **Architectury API** for your loader, plus **Fabric API** if using Fabric.
4. Download the corresponding Furnace Distributor JAR from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/furnace-distributor) or [GitHub Releases](https://github.com/thonyydev/FurnaceDistributor/releases).
5. Optionally add **Mod Menu** on Fabric or a compatible **Iron Furnaces** version on NeoForge.
6. Place the mod and its required dependencies in your Minecraft instance's `mods` folder.
7. Launch Minecraft.

Example release filenames:

```text
furnacedistributor-fabric-4.0.0+26.2.jar
furnacedistributor-neoforge-4.0.0+26.2.jar
```

Install **only the JAR for your loader**. Development (`-dev`) and source (`-sources`) JARs are not intended for normal installation.

## Building From Source

Install **JDK 25**. Configure `JAVA_HOME` if necessary. Machine-specific Java paths belong in your local environment or user Gradle configuration, not in this repository.

```bash
git clone https://github.com/thonyydev/FurnaceDistributor.git
cd FurnaceDistributor
git switch 26.2
```

**Windows (PowerShell):**

```powershell
.\gradlew.bat clean build
```

**Linux / macOS:**

```bash
./gradlew clean build
```

Distributable JARs are generated in `fabric/build/libs/` and `neoforge/build/libs/`. The filenames depend on `mod_version` and `minecraft_version` in `gradle.properties`.

## Reporting Issues

Found a bug? [Open an issue](https://github.com/thonyydev/FurnaceDistributor/issues) and include:

- Minecraft version and Furnace Distributor version.
- Mod loader and version.
- Singleplayer or multiplayer.
- Steps to reproduce the problem.
- Relevant logs or crash reports.

For distribution or collection issues, also mention the item/fuel and quantity, furnace types and modes, any Iron Furnaces upgrades, existing furnace contents, selected area, and other relevant mods.

## Contributing

Contributions are welcome: bug fixes, translations, performance improvements, mod compatibility, documentation, and quality-of-life features. For larger changes, consider [opening an issue](https://github.com/thonyydev/FurnaceDistributor/issues) before starting implementation.

## License

Furnace Distributor is licensed under the **MIT License**. See [LICENSE](LICENSE).

---

**Make large furnace setups easier to manage and spend less time opening furnaces one by one.**

If Furnace Distributor helps your smelting setup, consider giving the project a ⭐ on GitHub!