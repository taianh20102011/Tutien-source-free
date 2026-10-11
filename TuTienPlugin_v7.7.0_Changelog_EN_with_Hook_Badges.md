# TuTienPlugin v7.7.0 — Cultivation RPG Update

A cultivation-themed RPG experience for Minecraft Paper servers, featuring realm progression, spiritual energy, breakthroughs, classes, skills, artifacts, alchemy, clans, events, and optional third-party integrations.

![Paper](https://img.shields.io/badge/Platform-Paper-2D6A4F?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-21%2B-E76F00?style=for-the-badge&logo=openjdk&logoColor=white)
![Hooks](https://img.shields.io/badge/Integrations-Optional_Hooks-5865F2?style=for-the-badge)

## 🔌 Plugin Hooks & Integrations

TuTienPlugin declares optional hooks for the plugins below. Install a compatible plugin and configure it as needed to use the corresponding integration. These are integration badges, not official plugin logos, and the third-party plugins are **not bundled** with TuTienPlugin.

<p align="center">
  <a href="https://www.spigotmc.org/resources/itemsadder.73355/"><img src="https://img.shields.io/badge/ItemsAdder-Hook-8A2BE2?style=for-the-badge" alt="ItemsAdder Hook"></a>
  <a href="https://www.spigotmc.org/resources/vault.34315/"><img src="https://img.shields.io/badge/Vault-Economy_Hook-239B56?style=for-the-badge" alt="Vault Economy Hook"></a>
  <a href="https://enginehub.org/worldedit/"><img src="https://img.shields.io/badge/WorldEdit-Region_Tools-E67E22?style=for-the-badge" alt="WorldEdit Hook"></a>
  <a href="https://enginehub.org/worldguard/"><img src="https://img.shields.io/badge/WorldGuard-Region_Protection-C0392B?style=for-the-badge" alt="WorldGuard Hook"></a>
  <a href="https://www.spigotmc.org/resources/placeholderapi.6245/"><img src="https://img.shields.io/badge/PlaceholderAPI-Placeholders-2980B9?style=for-the-badge" alt="PlaceholderAPI Hook"></a>
  <img src="https://img.shields.io/badge/BetterHud-HUD_Integration-16A085?style=for-the-badge" alt="BetterHud Hook">
  <img src="https://img.shields.io/badge/MythicLib-Combat_Integration-7D3C98?style=for-the-badge" alt="MythicLib Hook">
  <a href="https://mythiccraft.io/"><img src="https://img.shields.io/badge/MythicMobs-Custom_Mobs-34495E?style=for-the-badge" alt="MythicMobs Hook"></a>
  <img src="https://img.shields.io/badge/SimplePets-Pet_Integration-D35400?style=for-the-badge" alt="SimplePets Hook">
  <a href="https://www.spigotmc.org/resources/citizens.13811/"><img src="https://img.shields.io/badge/Citizens-NPC_Integration-5D6D7E?style=for-the-badge" alt="Citizens Hook"></a>
</p>

| Integration | Intended use |
|---|---|
| **ItemsAdder** | Map supported skill effects to configured ItemsAdder entity/effect IDs. |
| **Vault** | Connect compatible economy operations through the Vault economy provider. |
| **WorldEdit** | Support region/selection workflows used by configured cultivation zones. |
| **WorldGuard** | Integrate with protected-region environments where supported. |
| **PlaceholderAPI** | Expose player cultivation data to scoreboards, tab lists, and compatible plugins. |
| **BetterHud** | Display cultivation information in compatible HUD layouts. |
| **MythicLib** | Provide compatibility hooks for supported combat/skill integrations. |
| **MythicMobs** | Use compatible custom mobs for configured encounters and boss systems. |
| **SimplePets** | Connect supported pet-related functionality. |
| **Citizens** | Connect supported NPC-related functionality. |

> **Compatibility note:** `plugin.yml` declares these as `softdepend` integrations. This means they are optional for plugin startup; it does not mean every feature works without the relevant dependency, configuration, or a compatible version. Verify each integration on a staging server before production use.

## ✨ Cultivation & Realm Progression

- 25 major cultivation realms across five cultivation worlds.
- 115 realm/rank milestones in the current source; minor-stage counts vary by realm.
- Spiritual Energy (Linh Khí) progression tied to cultivation and character development.
- Passive meditation through `/thiendinh`.
- Configurable Spiritual Veins (Linh Mạch) for enhanced cultivation in designated areas.

## ⚡ Breakthroughs & Heavenly Tribulations

- Breakthrough-related commands include `/vuottamma` and `/dotpha`.
- Multi-wave Heavenly Tribulation mechanics scale with progression and configuration.
- Failed attempts may cause rank loss according to the configured rules.
- Successful attempts provide progression feedback and configured rewards.

## ⚔️ Classes & Skills

- 8 class archetypes: Sword, Spell, Mind, Poison, Body, Demon, Wind, and Fire cultivators.
- 127 skill/technique IDs are registered in the current source.
- Skills may have class/rank requirements, cooldowns, configurable behavior, and item requirements.
- Combat options include sword techniques, fire and ice skills, poison, defensive formations, healing, and support effects.

## 🧬 Races, Spiritual Roots & Dao Paths

- 12 races with different configured traits and stat modifiers.
- Elemental and special Spiritual Roots influence selected attributes and cultivation progression.
- Four Dao paths are available in the current source: Righteous Dao, Demonic Dao, King Scholar, and Gu King.

## 🧿 Magical Artifacts — Pháp Bảo

- Artifacts cover offense, defense, support, and control effects.
- Supported progression commands include `/renduc`, `/cuonghoa`, `/khamnam`, `/dunghop`, `/linhthuc`, and `/khilinh`.
- Artifact options depend on artifact type, configuration, and player requirements.

## 🌿 Herbs & Alchemy

- The default configuration contains 32 herb types and 32 alchemy recipes.
- Herb collection and drop rates can be configured.
- Recipes may require Minecraft materials, herbs, cultivation rank, and Spirit Stones.
- Common commands include `/luyen`, `/thao`, and `/linhchungshop` where enabled by the current command implementation.

## 🐉 Bosses, Events & Quests

- World Boss support includes configured scheduling, warnings, BossBars, time limits, and damage-contribution tracking.
- MythicMobs integration is available when compatible mobs and configuration are provided.
- Blood Moon events provide server-wide encounters and Blood Moon Crystal mechanics.
- Karma, Daily/Weekly Quests, and combat statistics add further progression goals.

## 🏯 Clans, Parties & Trading

- Clan features are accessed through `/tongmon`.
- Party/group features are accessed through `/party` and `/group`.
- Direct trading uses `/trade`; item gifting uses `/tang`.
- Cultivation inventory interfaces include `/tuitutien` and `/ttbag`.

## ✨ Effects, Aura & UI

- Effect Engine provides configured particles and visual effects for skills, characters, and encounters.
- Cultivation Aura can be toggled with `/aura on` and `/aura off`.
- Supports player information display through nametags and compatible HUD/placeholder integrations.
- Premium configurations can map supported skill effects to ItemsAdder entity IDs when the relevant dependencies and resources are installed.

## ⚙️ Requirements

- **Server:** Paper 1.21.1 or compatible version as stated by the release.
- **Java:** 21 or newer.
- Back up configuration and player data before updating.
- Install optional integrations only when required by your chosen features.
- Check the server console for missing dependencies, unsupported versions, and configuration errors.

## 📌 Known Limitations

- Some integrations require an installed compatible plugin and additional configuration.
- The breakthrough Boss Arena is not fully connected to the breakthrough flow in the current source.
- An `/auction` command is not registered in the current source; do not assume an auction house is available.
- Features may differ between FREE and PREMIUM editions. Check the release notes and license terms for the edition you use.

## 🔗 Official Links

- **Website:** https://taianh20102011.github.io/TUTIEN/
- **Update announcements:** https://taianh20102011.github.io/TUTIEN_PLUGINS_UPDATE/
- **Q&A:** https://script.google.com/macros/s/AKfycbyK1wIucrV8KP3WW2gDpYskaomHGfzttr2epXA0eIvct_SzlBfYoHbyd0MtlTIP_kE9uQ/exec
- **Cultivation realm chart:** https://postimg.cc/1V89DPJ7
- **Email:** cachgiaconline@gmail.com
- **Discord:** `taianh201011`

**Cultivate your power. Break through your limits. Build your own path to immortality.**

*TuTienPlugin — Your cultivation journey begins here.*
