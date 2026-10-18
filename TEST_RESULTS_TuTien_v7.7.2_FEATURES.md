# TuTienPlugin v7.7.2 — Static validation

{
  "release": "7.7.2",
  "editions": [
    {
      "edition": "FREE",
      "java_files": 103,
      "yaml_files_recursive": 21,
      "java_lexical_errors": [],
      "yaml_errors": [],
      "pom_xml_valid": true,
      "checks": {
        "land_break_place_protection": true,
        "land_entry_and_interaction": true,
        "land_fluid_fire_explosion_piston": true,
        "worldguard_native_region": true,
        "griefprevention_native_claim": true,
        "mutual_alliance_land_consent": true,
        "mutual_alliance_pvp_enforcement": true,
        "skill_chienkhiquyet": true,
        "skill_phatthuan": true,
        "skill_thienmatrao": true,
        "npc_randomskip_and_stats": true,
        "npc_follower": true,
        "claim_config": true,
        "custom_congphap_config": true,
        "custom_dao_config": true,
        "custom_phabao_config": true,
        "custom_hubs_config": true
      },
      "checks_passed": true
    },
    {
      "edition": "PREMIUM",
      "java_files": 103,
      "yaml_files_recursive": 21,
      "java_lexical_errors": [],
      "yaml_errors": [],
      "pom_xml_valid": true,
      "checks": {
        "land_break_place_protection": true,
        "land_entry_and_interaction": true,
        "land_fluid_fire_explosion_piston": true,
        "worldguard_native_region": true,
        "griefprevention_native_claim": true,
        "mutual_alliance_land_consent": true,
        "mutual_alliance_pvp_enforcement": true,
        "skill_chienkhiquyet": true,
        "skill_phatthuan": true,
        "skill_thienmatrao": true,
        "npc_randomskip_and_stats": true,
        "npc_follower": true,
        "claim_config": true,
        "custom_congphap_config": true,
        "custom_dao_config": true,
        "custom_phabao_config": true,
        "custom_hubs_config": true
      },
      "checks_passed": true
    }
  ],
  "shared_source_identical": {
    "tongmon/TongMonLandManager.java": true,
    "tongmon/TongMonAllianceManager.java": true,
    "tongmon/TongMonManager.java": true,
    "skills/SkillManager.java": true,
    "skills/SkillItemManager.java": true,
    "commands/TuTienCommand.java": true,
    "congphap/ConGPhapManager.java": true,
    "suphudedo/SuPhuDeDoManager.java": true,
    "hooks/TuTienPlaceholderExpansion.java": true
  },
  "passed": true,
  "scope_note": "Static source validation only; no Maven build or Paper runtime integration test was possible in this environment."
}

Maven build/runtime chưa được chạy vì không có Maven/Paper hook dependencies trong môi trường đóng gói.
