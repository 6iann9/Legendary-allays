# Legendary Allays

A Minecraft mod that lets you use a lute to command up to eight Allay companions. They follow you, collect items, and deliver them into your inventory.

Authors: iann and emovinh. Version: 0.1.0.

## Install

Requirements: Minecraft Java Edition **1.21.1**, **NeoForge 21.1.267** (the version used to build this release), and **Java 21**. No extra library mods are required for this build.

1. Install NeoForge for Minecraft 1.21.1 and select its profile in your launcher.
2. Open that profile's game folder and find or create the `mods` folder. For the default Windows game directory, this is `%APPDATA%\.minecraft\mods`. Custom launcher instances have their own game folders.
3. Copy `legendaryallays-0.1.0.jar` from this folder into `mods`. Keep it as a JAR; do not extract it. Remove an older copy of this mod when replacing it.
4. Launch the NeoForge profile and enter a world.

For a multiplayer game, install the same mod JAR on the NeoForge server and on each player's client.

## Get a lute

Craft it at a crafting table using any planks:

| | | |
| --- | --- | --- |
| Iron nugget | Planks | Iron nugget |
| Planks | String | Planks |
| Planks | Planks | Planks |

You can also find the lute in the Creative Tools & Utilities tab, or use this command with cheats enabled:

```text
/give @s legendaryallays:lute
```

## Controls

- Hold the lute in your **main hand** and right-click to open the wheel.
- Point the mouse at an option and left-click to select it. You can still walk while the wheel is open.
- Right-click, Escape, or click the center to close the main wheel. In the Collection submenu, those controls return to the main wheel.
- Selecting a collection mode closes the wheel and starts that mode.

## Bind and release Allays

Look at a normal Allay within 16 blocks and select **Bind Allay**. You can bind up to eight. Detach leads first; an Allay following another player cannot be bound.

Bound Allays follow you, pass through walls, and cannot be attacked. Select **Unbind Allay** to release one at a safe position where you are looking. An Allay without cargo is preferred. Named Allays keep their names, and carried items are dropped when released.

## Collection modes

| Mode | Behavior |
| --- | --- |
| Collect Dropped | Picks up new drops from blocks you break and mobs you kill while this mode is active. Allays teleport to the drops, then fly back. Remaining drops wait in a queue while companions are busy, as long as those items still exist. |
| Collect Nearby | Allays fly to eligible dropped items within 16 blocks of you, pick them up, and fly back. |
| Collect Everywhere | Combines the two modes. Your mining and mob drops take priority over nearby pickups. This does not collect across the entire world. |
| Idle | Stops new pickups and clears pending jobs. Allays already carrying items finish delivering them. |

Items go into your inventory. Anything that does not fit drops near you. Keep a lute somewhere in your inventory for collection to continue; without it, Allays follow passively and finish deliveries already underway.

## Item filters

Hold an item in your **offhand**, open the wheel with the lute in your main hand, and choose **Add Item to Filter**. Repeat for additional item types. Once the filter contains items, Allays collect only those types in all collection modes. Filtering uses item type, not custom names or enchantments.

Choose **Clear Filter** to allow any item again. Adding a filter does not consume the offhand item. Already-carried items still get delivered.

## Logging out

Bound companions disappear when you log out and return when you log in. They drop cargo when you log out, forget queued pickups, and return in Idle mode. Ownership and item filters are saved.

## Quick demonstration

1. Get a lute and bind two or more Allays.
2. Select Collect Dropped, then break several blocks to see pickup and delivery.
3. Select Collect Nearby and drop an item on the ground.
4. Add an offhand item to the filter and demonstrate selective collection.
5. Select Idle, then unbind an Allay.

## Build from source

Open a terminal in the project root with a Java 21 JDK available and run:

```powershell
.\gradlew.bat build
```

The compiled JAR appears in `build/libs/legendaryallays-0.1.0.jar`. Copy that new file into this README folder when preparing an updated package; this folder's bundled JAR is a snapshot, not an automatically updated build output.

## Credits

The lute model and texture are from steppen's Lute from Minecraft Legends CIT resource pack. See the included `ASSET_CREDITS.md` for the original asset attribution and terms note. Third-party assets are separate from the project's MIT code license.
