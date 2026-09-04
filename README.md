# Better Admin

Server-side Forge 1.20.1 admin/chaos command mod. Fork it, modify it, redistribute it, or do whatever you want with it.

## Commands

- `/lava` — fills the executor's current 16x16 chunk at the dimension's top build level with lava source blocks.
- `/warden` — spawns up to 5 Wardens near each online player.
- `/creeper <player>` — spawns a normal Creeper with 300 max health, hidden nearby when possible, and keeps it focused on the selected player. The hunter can break nearby non-bedrock obstructions when it loses line of sight.
- `/rich <player>` — equips full Netherite armor, puts a Totem of Undying in the off-hand, adds two more Totems, and adds a full set of Netherite tools.
- `/tnt` — toggles a TNT spawn at the executor's position every 4 seconds.
- `/tnt stop` — stops the executor's repeating TNT.
- `/vanish` — toggles invisibility and removes the player from other clients' tab lists. Toggling on broadcasts a normal `left the game` message; toggling off broadcasts a normal `joined the game` message.

Commands do not require operator permission and intentionally produce no command feedback.

The repeating TNT system has a global active-entity cap and the Warden command is bounded to avoid accidentally overwhelming the server.

## Server-side

Better Admin is intended to run on the server. Players do not need the mod installed on their clients.

## Building

GitHub Actions builds the mod automatically and uploads the server-side JAR as a workflow artifact.
