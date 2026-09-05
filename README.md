# Troll Commands

Server-side Forge 1.20.1 troll/chaos command mod. Fork it, modify it, redistribute it, or do whatever you want with it.

## Server mod

The server-side mod provides the full Troll Commands toolkit. Players do not need the mod installed on their clients.

## Client mod

`client/` contains a separate Forge 1.20.1 client-side mod named **Troll Commands Client**. It does **not** require the server-side Troll Commands mod. It adds local shortcuts that translate into vanilla server commands, so the player must have operator permission for the server to accept the commands.

The client mod can approximate these server-side commands with vanilla commands:

- `/lava` — fills the current 16x16 chunk at the dimension's top build level with lava source blocks. Overworld/End use Y 319; Nether uses Y 127.
- `/warden` — summons 5 Wardens around every online player using `execute as @a at @s`.
- `/creeper <player>` — summons a Creeper near the target with 300 max health. Vanilla commands cannot reproduce the server mod's hidden hunter targeting/obstruction-breaking behavior exactly.
- `/rich <player>` — equips Netherite armor, a Totem in the off-hand, two more Totems, and Netherite tools using `/item` and `/give`.
- `/tnt` — toggles a repeating TNT summon at the executing player's position every 4 seconds.
- `/tnt stop` — stops the client-side TNT loop.
- `/vanish` — toggles a long invisibility effect. Vanilla commands cannot remove the player from the tab list like the server mod does.
- `/vanish stop` — clears the client-side invisibility effect.
- `/itemrain` — every second, runs 6 random item summons for every online player, giving an approximation of item rain across player-active chunks.
- `/itemrain stop` — stops the client-side item rain.
- `/restart` — runs `save-all flush` followed by the vanilla `stop` command. A real automatic reboot requires the server host/panel/wrapper to restart the server process after it stops.

The client mod intentionally does not implement `/hackers` because vanilla commands cannot create the fake-player entities used by the server mod. `/ores` regeneration is also not reproduced because the server mod keeps an exact server-side snapshot of removed ore blocks.

All client commands check for permission level 2 before doing anything. This is a client-side convenience check; the server still decides whether the submitted vanilla command is allowed.

## Building

GitHub Actions builds both JARs and uploads them as separate workflow artifacts: `troll-commands-server` and `troll-commands-client`.
