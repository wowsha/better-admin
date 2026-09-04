# Better Admin

Server-side Forge 1.20.1 admin command mod.

## Commands

- `/lava` — fills the executor's current 16x16 chunk at the dimension's top build level with lava source blocks.
- `/warden` — spawns up to 5 Wardens near each online player.
- `/creeper <player>` — spawns a normal Creeper with 300 max health, hidden nearby when possible, and keeps it focused on the selected player. The hunter can break nearby non-bedrock obstructions when it loses line of sight.
- `/rich <player>` — equips full Netherite armor, puts a Totem of Undying in the off-hand, adds two more Totems, and adds a full set of Netherite tools.
- `/tnt` — toggles a TNT spawn at the executor's position every 4 seconds.
- `/tnt stop` — stops the executor's repeating TNT.
- `/vanish` — toggles invisibility and removes the player from other clients' tab lists. Toggling on broadcasts a normal `left the game` message; toggling off broadcasts a normal `joined the game` message.

All commands require operator permission level 2 and intentionally produce no command feedback.

The repeating TNT system has a global active-entity cap and the Warden command is bounded to avoid accidentally overwhelming the server.
