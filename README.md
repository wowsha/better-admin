# Better Admin

Server-side Forge 1.20.1 admin/chaos command mod. It adds extra commands without adding custom blocks, items, or client-side content.

The commands do not require operator permission.

## Commands

- `/lava` — fills the executor's current 16x16 chunk at the dimension's top build level with lava source blocks.
- `/warden` — spawns up to 5 Wardens near each online player.
- `/creeper <player>` — spawns a normal Creeper with 300 max health, hidden nearby when possible, and keeps it focused on the selected player. The hunter can break nearby non-bedrock obstructions when it loses line of sight.
- `/rich <player>` — equips full Netherite armor, puts a Totem of Undying in the off-hand, adds two more Totems, and adds a full set of Netherite tools. Replaced armor/off-hand items are returned to the player's inventory when possible.
- `/tnt` — toggles a TNT spawn at the executor's position every 4 seconds.
- `/tnt stop` — stops the executor's repeating TNT.
- `/vanish` — toggles invisibility and removes the player from other clients' tab lists. Toggling on broadcasts a normal `left the game` message; toggling off broadcasts a normal `joined the game` message.

Commands intentionally produce no command feedback.

The repeating TNT system has a global active-entity cap and the Warden command is bounded to avoid accidentally overwhelming the server.

## Server-side

This mod is intended to run on the server. `mods.toml` declares the mod for the server side, and clients do not need Better Admin installed.

## Building

GitHub Actions builds the JAR on pushes and pull requests to `main`, and the finished server-side JAR is uploaded as an Actions artifact named `better-admin-server`.

## License

MIT
