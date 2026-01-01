package com.NumberGoUp;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("numbergoup")
public interface NumberGoUpConfig extends Config
{
    @ConfigItem(
            keyName = "xpModifier",
            name = "Global XP Modifier",
            description = "Multiplier for XP gained during session (1.0 = normal XP, 2.0 = double XP, etc.)"
    )
    @Range(
            min = 1,
            max = 15
    )
    default double xpModifier()
    {
        return 1.0; // Default 1.0 (normal XP)
    }

    @ConfigItem(
            keyName = "resetMode",
            name = "Reset Mode",
            description = "Which skills to reset when the plugin starts"
    )
    default ResetMode resetMode()
    {
        return ResetMode.ALL_SKILLS;
    }

    // Reset progress button - we'll handle this differently in the plugin
    @ConfigItem(
            keyName = "resetProgress",
            name = "Reset All Progress",
            description = "Reset all saved progress and start fresh from current XP",
            position = 100
    )
    default boolean resetProgress()
    {
        return false;
    }

    // Rest of the file remains EXACTLY THE SAME as your original
    // Per-skill custom XP thresholds for THRESHOLD mode
    @ConfigSection(name = "Custom XP Thresholds", description = "Set custom XP thresholds for reset (only works with 'Per Skill Threshold' mode)", position = 5, closedByDefault = true)
    String thresholdSection = "thresholdSection";

    @ConfigItem(keyName = "attackThreshold", name = "Attack XP Threshold", description = "Reset Attack when XP reaches this amount (0 = never reset)", section = thresholdSection, position = 1)
    @Range(min = 0, max = 200000000)
    default int attackThreshold() { return 0; }

    @ConfigItem(keyName = "strengthThreshold", name = "Strength XP Threshold", description = "Reset Strength when XP reaches this amount (0 = never reset)", section = thresholdSection, position = 2)
    @Range(min = 0, max = 200000000)
    default int strengthThreshold() { return 0; }

    @ConfigItem(keyName = "defenceThreshold", name = "Defence XP Threshold", description = "Reset Defence when XP reaches this amount (0 = never reset)", section = thresholdSection, position = 3)
    @Range(min = 0, max = 200000000)
    default int defenceThreshold() { return 0; }

    @ConfigItem(keyName = "rangedThreshold", name = "Ranged XP Threshold", description = "Reset Ranged when XP reaches this amount (0 = never reset)", section = thresholdSection, position = 4)
    @Range(min = 0, max = 200000000)
    default int rangedThreshold() { return 0; }

    @ConfigItem(keyName = "prayerThreshold", name = "Prayer XP Threshold", description = "Reset Prayer when XP reaches this amount (0 = never reset)", section = thresholdSection, position = 5)
    @Range(min = 0, max = 200000000)
    default int prayerThreshold() { return 0; }

    @ConfigItem(keyName = "magicThreshold", name = "Magic XP Threshold", description = "Reset Magic when XP reaches this amount (0 = never reset)", section = thresholdSection, position = 6)
    @Range(min = 0, max = 200000000)
    default int magicThreshold() { return 0; }

    @ConfigItem(keyName = "runecraftThreshold", name = "Runecraft XP Threshold", description = "Reset Runecraft when XP reaches this amount (0 = never reset)", section = thresholdSection, position = 7)
    @Range(min = 0, max = 200000000)
    default int runecraftThreshold() { return 0; }

    @ConfigItem(keyName = "constructionThreshold", name = "Construction XP Threshold", description = "Reset Construction when XP reaches this amount (0 = never reset)", section = thresholdSection, position = 8)
    @Range(min = 0, max = 200000000)
    default int constructionThreshold() { return 0; }

    @ConfigItem(keyName = "hitpointsThreshold", name = "Hitpoints XP Threshold", description = "Reset Hitpoints when XP reaches this amount (0 = never reset)", section = thresholdSection, position = 9)
    @Range(min = 0, max = 200000000)
    default int hitpointsThreshold() { return 0; }

    @ConfigItem(keyName = "agilityThreshold", name = "Agility XP Threshold", description = "Reset Agility when XP reaches this amount (0 = never reset)", section = thresholdSection, position = 10)
    @Range(min = 0, max = 200000000)
    default int agilityThreshold() { return 0; }

    @ConfigItem(keyName = "herbloreThreshold", name = "Herblore XP Threshold", description = "Reset Herblore when XP reaches this amount (0 = never reset)", section = thresholdSection, position = 11)
    @Range(min = 0, max = 200000000)
    default int herbloreThreshold() { return 0; }

    @ConfigItem(keyName = "thievingThreshold", name = "Thieving XP Threshold", description = "Reset Thieving when XP reaches this amount (0 = never reset)", section = thresholdSection, position = 12)
    @Range(min = 0, max = 200000000)
    default int thievingThreshold() { return 0; }

    @ConfigItem(keyName = "craftingThreshold", name = "Crafting XP Threshold", description = "Reset Crafting when XP reaches this amount (0 = never reset)", section = thresholdSection, position = 13)
    @Range(min = 0, max = 200000000)
    default int craftingThreshold() { return 0; }

    @ConfigItem(keyName = "fletchingThreshold", name = "Fletching XP Threshold", description = "Reset Fletching when XP reaches this amount (0 = never reset)", section = thresholdSection, position = 14)
    @Range(min = 0, max = 200000000)
    default int fletchingThreshold() { return 0; }

    @ConfigItem(keyName = "slayerThreshold", name = "Slayer XP Threshold", description = "Reset Slayer when XP reaches this amount (0 = never reset)", section = thresholdSection, position = 15)
    @Range(min = 0, max = 200000000)
    default int slayerThreshold() { return 0; }

    @ConfigItem(keyName = "hunterThreshold", name = "Hunter XP Threshold", description = "Reset Hunter when XP reaches this amount (0 = never reset)", section = thresholdSection, position = 16)
    @Range(min = 0, max = 200000000)
    default int hunterThreshold() { return 0; }

    @ConfigItem(keyName = "miningThreshold", name = "Mining XP Threshold", description = "Reset Mining when XP reaches this amount (0 = never reset)", section = thresholdSection, position = 17)
    @Range(min = 0, max = 200000000)
    default int miningThreshold() { return 0; }

    @ConfigItem(keyName = "smithingThreshold", name = "Smithing XP Threshold", description = "Reset Smithing when XP reaches this amount (0 = never reset)", section = thresholdSection, position = 18)
    @Range(min = 0, max = 200000000)
    default int smithingThreshold() { return 0; }

    @ConfigItem(keyName = "fishingThreshold", name = "Fishing XP Threshold", description = "Reset Fishing when XP reaches this amount (0 = never reset)", section = thresholdSection, position = 19)
    @Range(min = 0, max = 200000000)
    default int fishingThreshold() { return 0; }

    @ConfigItem(keyName = "cookingThreshold", name = "Cooking XP Threshold", description = "Reset Cooking when XP reaches this amount (0 = never reset)", section = thresholdSection, position = 20)
    @Range(min = 0, max = 200000000)
    default int cookingThreshold() { return 0; }

    @ConfigItem(keyName = "firemakingThreshold", name = "Firemaking XP Threshold", description = "Reset Firemaking when XP reaches this amount (0 = never reset)", section = thresholdSection, position = 21)
    @Range(min = 0, max = 200000000)
    default int firemakingThreshold() { return 0; }

    @ConfigItem(keyName = "woodcuttingThreshold", name = "Woodcutting XP Threshold", description = "Reset Woodcutting when XP reaches this amount (0 = never reset)", section = thresholdSection, position = 22)
    @Range(min = 0, max = 200000000)
    default int woodcuttingThreshold() { return 0; }

    @ConfigItem(keyName = "farmingThreshold", name = "Farming XP Threshold", description = "Reset Farming when XP reaches this amount (0 = never reset)", section = thresholdSection, position = 23)
    @Range(min = 0, max = 200000000)
    default int farmingThreshold() { return 0; }

    @ConfigItem(keyName = "sailingThreshold", name = "Sailing XP Threshold", description = "Reset Sailing when XP reaches this amount (0 = never reset)", section = thresholdSection, position = 24)
    @Range(min = 0, max = 200000000)
    default int sailingThreshold() { return 0; }

    // Per-skill XP multipliers with override checkboxes
    @ConfigSection(name = "Attack", description = "Attack XP multiplier settings", position = 10, closedByDefault = true)
    String attackSection = "attackSection";

    @ConfigItem(keyName = "overrideAttack", name = "Override Global Modifier", description = "Use custom XP modifier for Attack instead of global", section = attackSection, position = 1)
    default boolean overrideAttack() { return false; }

    @ConfigItem(keyName = "attackMultiplier", name = "Attack XP Modifier", description = "Multiplier for Attack XP (only used if override is enabled)", section = attackSection, position = 2)
    @Range(min = 1, max = 15)
    default double attackMultiplier() { return 1.0; }

    @ConfigItem(keyName = "resetAttack", name = "Custom Mode: Reset Skill?", description = "Reset this skill when Custom mode is selected", section = attackSection, position = 3)
    default boolean resetAttack() { return false; }

    @ConfigSection(name = "Strength", description = "Strength XP multiplier settings", position = 11, closedByDefault = true)
    String strengthSection = "strengthSection";

    @ConfigItem(keyName = "overrideStrength", name = "Override Global Modifier", description = "Use custom XP modifier for Strength instead of global", section = strengthSection, position = 1)
    default boolean overrideStrength() { return false; }

    @ConfigItem(keyName = "strengthMultiplier", name = "Strength XP Modifier", description = "Multiplier for Strength XP (only used if override is enabled)", section = strengthSection, position = 2)
    @Range(min = 1, max = 15)
    default double strengthMultiplier() { return 1.0; }

    @ConfigItem(keyName = "resetStrength", name = "Custom Mode: Reset Skill?", description = "Reset this skill when Custom mode is selected", section = strengthSection, position = 3)
    default boolean resetStrength() { return false; }

    @ConfigSection(name = "Defence", description = "Defence XP multiplier settings", position = 12, closedByDefault = true)
    String defenceSection = "defenceSection";

    @ConfigItem(keyName = "overrideDefence", name = "Override Global Modifier", description = "Use custom XP modifier for Defence instead of global", section = defenceSection, position = 1)
    default boolean overrideDefence() { return false; }

    @ConfigItem(keyName = "defenceMultiplier", name = "Defence XP Modifier", description = "Multiplier for Defence XP (only used if override is enabled)", section = defenceSection, position = 2)
    @Range(min = 1, max = 15)
    default double defenceMultiplier() { return 1.0; }

    @ConfigItem(keyName = "resetDefence", name = "Custom Mode: Reset Skill?", description = "Reset this skill when Custom mode is selected", section = defenceSection, position = 3)
    default boolean resetDefence() { return false; }

    @ConfigSection(name = "Ranged", description = "Ranged XP multiplier settings", position = 13, closedByDefault = true)
    String rangedSection = "rangedSection";

    @ConfigItem(keyName = "overrideRanged", name = "Override Global Modifier", description = "Use custom XP modifier for Ranged instead of global", section = rangedSection, position = 1)
    default boolean overrideRanged() { return false; }

    @ConfigItem(keyName = "rangedMultiplier", name = "Ranged XP Modifier", description = "Multiplier for Ranged XP (only used if override is enabled)", section = rangedSection, position = 2)
    @Range(min = 1, max = 15)
    default double rangedMultiplier() { return 1.0; }

    @ConfigItem(keyName = "resetRanged", name = "Custom Mode: Reset Skill?", description = "Reset this skill when Custom mode is selected", section = rangedSection, position = 3)
    default boolean resetRanged() { return false; }

    @ConfigSection(name = "Prayer", description = "Prayer XP multiplier settings", position = 14, closedByDefault = true)
    String prayerSection = "prayerSection";

    @ConfigItem(keyName = "overridePrayer", name = "Override Global Modifier", description = "Use custom XP modifier for Prayer instead of global", section = prayerSection, position = 1)
    default boolean overridePrayer() { return false; }

    @ConfigItem(keyName = "prayerMultiplier", name = "Prayer XP Modifier", description = "Multiplier for Prayer XP (only used if override is enabled)", section = prayerSection, position = 2)
    @Range(min = 1, max = 15)
    default double prayerMultiplier() { return 1.0; }

    @ConfigItem(keyName = "resetPrayer", name = "Custom Mode: Reset Skill?", description = "Reset this skill when Custom mode is selected", section = prayerSection, position = 3)
    default boolean resetPrayer() { return false; }

    @ConfigSection(name = "Magic", description = "Magic XP multiplier settings", position = 15, closedByDefault = true)
    String magicSection = "magicSection";

    @ConfigItem(keyName = "overrideMagic", name = "Override Global Modifier", description = "Use custom XP modifier for Magic instead of global", section = magicSection, position = 1)
    default boolean overrideMagic() { return false; }

    @ConfigItem(keyName = "magicMultiplier", name = "Magic XP Modifier", description = "Multiplier for Magic XP (only used if override is enabled)", section = magicSection, position = 2)
    @Range(min = 1, max = 15)
    default double magicMultiplier() { return 1.0; }

    @ConfigItem(keyName = "resetMagic", name = "Custom Mode: Reset Skill?", description = "Reset this skill when Custom mode is selected", section = magicSection, position = 3)
    default boolean resetMagic() { return false; }

    @ConfigSection(name = "Runecraft", description = "Runecraft XP multiplier settings", position = 16, closedByDefault = true)
    String runecraftSection = "runecraftSection";

    @ConfigItem(keyName = "overrideRunecraft", name = "Override Global Modifier", description = "Use custom XP modifier for Runecraft instead of global", section = runecraftSection, position = 1)
    default boolean overrideRunecraft() { return false; }

    @ConfigItem(keyName = "runecraftMultiplier", name = "Runecraft XP Modifier", description = "Multiplier for Runecraft XP (only used if override is enabled)", section = runecraftSection, position = 2)
    @Range(min = 1, max = 15)
    default double runecraftMultiplier() { return 1.0; }

    @ConfigItem(keyName = "resetRunecraft", name = "Custom Mode: Reset Skill?", description = "Reset this skill when Custom mode is selected", section = runecraftSection, position = 3)
    default boolean resetRunecraft() { return false; }

    @ConfigSection(name = "Construction", description = "Construction XP multiplier settings", position = 17, closedByDefault = true)
    String constructionSection = "constructionSection";

    @ConfigItem(keyName = "overrideConstruction", name = "Override Global Modifier", description = "Use custom XP modifier for Construction instead of global", section = constructionSection, position = 1)
    default boolean overrideConstruction() { return false; }

    @ConfigItem(keyName = "constructionMultiplier", name = "Construction XP Modifier", description = "Multiplier for Construction XP (only used if override is enabled)", section = constructionSection, position = 2)
    @Range(min = 1, max = 15)
    default double constructionMultiplier() { return 1.0; }

    @ConfigItem(keyName = "resetConstruction", name = "Custom Mode: Reset Skill?", description = "Reset this skill when Custom mode is selected", section = constructionSection, position = 3)
    default boolean resetConstruction() { return false; }

    @ConfigSection(name = "Hitpoints", description = "Hitpoints XP multiplier settings", position = 18, closedByDefault = true)
    String hitpointsSection = "hitpointsSection";

    @ConfigItem(keyName = "overrideHitpoints", name = "Override Global Modifier", description = "Use custom XP modifier for Hitpoints instead of global", section = hitpointsSection, position = 1)
    default boolean overrideHitpoints() { return false; }

    @ConfigItem(keyName = "hitpointsMultiplier", name = "Hitpoints XP Modifier", description = "Multiplier for Hitpoints XP (only used if override is enabled)", section = hitpointsSection, position = 2)
    @Range(min = 1, max = 15)
    default double hitpointsMultiplier() { return 1.0; }

    @ConfigItem(keyName = "resetHitpoints", name = "Custom Mode: Reset Skill?", description = "Reset this skill when Custom mode is selected", section = hitpointsSection, position = 3)
    default boolean resetHitpoints() { return false; }

    @ConfigSection(name = "Agility", description = "Agility XP multiplier settings", position = 19, closedByDefault = true)
    String agilitySection = "agilitySection";

    @ConfigItem(keyName = "overrideAgility", name = "Override Global Modifier", description = "Use custom XP modifier for Agility instead of global", section = agilitySection, position = 1)
    default boolean overrideAgility() { return false; }

    @ConfigItem(keyName = "agilityMultiplier", name = "Agility XP Modifier", description = "Multiplier for Agility XP (only used if override is enabled)", section = agilitySection, position = 2)
    @Range(min = 1, max = 15)
    default double agilityMultiplier() { return 1.0; }

    @ConfigItem(keyName = "resetAgility", name = "Custom Mode: Reset Skill?", description = "Reset this skill when Custom mode is selected", section = agilitySection, position = 3)
    default boolean resetAgility() { return false; }

    @ConfigSection(name = "Herblore", description = "Herblore XP multiplier settings", position = 20, closedByDefault = true)
    String herbloreSection = "herbloreSection";

    @ConfigItem(keyName = "overrideHerblore", name = "Override Global Modifier", description = "Use custom XP modifier for Herblore instead of global", section = herbloreSection, position = 1)
    default boolean overrideHerblore() { return false; }

    @ConfigItem(keyName = "herbloreMultiplier", name = "Herblore XP Modifier", description = "Multiplier for Herblore XP (only used if override is enabled)", section = herbloreSection, position = 2)
    @Range(min = 1, max = 15)
    default double herbloreMultiplier() { return 1.0; }

    @ConfigItem(keyName = "resetHerblore", name = "Custom Mode: Reset Skill?", description = "Reset this skill when Custom mode is selected", section = herbloreSection, position = 3)
    default boolean resetHerblore() { return false; }

    @ConfigSection(name = "Thieving", description = "Thieving XP multiplier settings", position = 21, closedByDefault = true)
    String thievingSection = "thievingSection";

    @ConfigItem(keyName = "overrideThieving", name = "Override Global Modifier", description = "Use custom XP modifier for Thieving instead of global", section = thievingSection, position = 1)
    default boolean overrideThieving() { return false; }

    @ConfigItem(keyName = "thievingMultiplier", name = "Thieving XP Modifier", description = "Multiplier for Thieving XP (only used if override is enabled)", section = thievingSection, position = 2)
    @Range(min = 1, max = 15)
    default double thievingMultiplier() { return 1.0; }

    @ConfigItem(keyName = "resetThieving", name = "Custom Mode: Reset Skill?", description = "Reset this skill when Custom mode is selected", section = thievingSection, position = 3)
    default boolean resetThieving() { return false; }

    @ConfigSection(name = "Crafting", description = "Crafting XP multiplier settings", position = 22, closedByDefault = true)
    String craftingSection = "craftingSection";

    @ConfigItem(keyName = "overrideCrafting", name = "Override Global Modifier", description = "Use custom XP modifier for Crafting instead of global", section = craftingSection, position = 1)
    default boolean overrideCrafting() { return false; }

    @ConfigItem(keyName = "craftingMultiplier", name = "Crafting XP Modifier", description = "Multiplier for Crafting XP (only used if override is enabled)", section = craftingSection, position = 2)
    @Range(min = 1, max = 15)
    default double craftingMultiplier() { return 1.0; }

    @ConfigItem(keyName = "resetCrafting", name = "Custom Mode: Reset Skill?", description = "Reset this skill when Custom mode is selected", section = craftingSection, position = 3)
    default boolean resetCrafting() { return false; }

    @ConfigSection(name = "Fletching", description = "Fletching XP multiplier settings", position = 23, closedByDefault = true)
    String fletchingSection = "fletchingSection";

    @ConfigItem(keyName = "overrideFletching", name = "Override Global Modifier", description = "Use custom XP modifier for Fletching instead of global", section = fletchingSection, position = 1)
    default boolean overrideFletching() { return false; }

    @ConfigItem(keyName = "fletchingMultiplier", name = "Fletching XP Modifier", description = "Multiplier for Fletching XP (only used if override is enabled)", section = fletchingSection, position = 2)
    @Range(min = 1, max = 15)
    default double fletchingMultiplier() { return 1.0; }

    @ConfigItem(keyName = "resetFletching", name = "Custom Mode: Reset Skill?", description = "Reset this skill when Custom mode is selected", section = fletchingSection, position = 3)
    default boolean resetFletching() { return false; }

    @ConfigSection(name = "Slayer", description = "Slayer XP multiplier settings", position = 24, closedByDefault = true)
    String slayerSection = "slayerSection";

    @ConfigItem(keyName = "overrideSlayer", name = "Override Global Modifier", description = "Use custom XP modifier for Slayer instead of global", section = slayerSection, position = 1)
    default boolean overrideSlayer() { return false; }

    @ConfigItem(keyName = "slayerMultiplier", name = "Slayer XP Modifier", description = "Multiplier for Slayer XP (only used if override is enabled)", section = slayerSection, position = 2)
    @Range(min = 1, max = 15)
    default double slayerMultiplier() { return 1.0; }

    @ConfigItem(keyName = "resetSlayer", name = "Custom Mode: Reset Skill?", description = "Reset this skill when Custom mode is selected", section = slayerSection, position = 3)
    default boolean resetSlayer() { return false; }

    @ConfigSection(name = "Hunter", description = "Hunter XP multiplier settings", position = 25, closedByDefault = true)
    String hunterSection = "hunterSection";

    @ConfigItem(keyName = "overrideHunter", name = "Override Global Modifier", description = "Use custom XP modifier for Hunter instead of global", section = hunterSection, position = 1)
    default boolean overrideHunter() { return false; }

    @ConfigItem(keyName = "hunterMultiplier", name = "Hunter XP Modifier", description = "Multiplier for Hunter XP (only used if override is enabled)", section = hunterSection, position = 2)
    @Range(min = 1, max = 15)
    default double hunterMultiplier() { return 1.0; }

    @ConfigItem(keyName = "resetHunter", name = "Custom Mode: Reset Skill?", description = "Reset this skill when Custom mode is selected", section = hunterSection, position = 3)
    default boolean resetHunter() { return false; }

    @ConfigSection(name = "Mining", description = "Mining XP multiplier settings", position = 26, closedByDefault = true)
    String miningSection = "miningSection";

    @ConfigItem(keyName = "overrideMining", name = "Override Global Modifier", description = "Use custom XP modifier for Mining instead of global", section = miningSection, position = 1)
    default boolean overrideMining() { return false; }

    @ConfigItem(keyName = "miningMultiplier", name = "Mining XP Modifier", description = "Multiplier for Mining XP (only used if override is enabled)", section = miningSection, position = 2)
    @Range(min = 1, max = 15)
    default double miningMultiplier() { return 1.0; }

    @ConfigItem(keyName = "resetMining", name = "Custom Mode: Reset Skill?", description = "Reset this skill when Custom mode is selected", section = miningSection, position = 3)
    default boolean resetMining() { return false; }

    @ConfigSection(name = "Smithing", description = "Smithing XP multiplier settings", position = 27, closedByDefault = true)
    String smithingSection = "smithingSection";

    @ConfigItem(keyName = "overrideSmithing", name = "Override Global Modifier", description = "Use custom XP modifier for Smithing instead of global", section = smithingSection, position = 1)
    default boolean overrideSmithing() { return false; }

    @ConfigItem(keyName = "smithingMultiplier", name = "Smithing XP Modifier", description = "Multiplier for Smithing XP (only used if override is enabled)", section = smithingSection, position = 2)
    @Range(min = 1, max = 15)
    default double smithingMultiplier() { return 1.0; }

    @ConfigItem(keyName = "resetSmithing", name = "Custom Mode: Reset Skill?", description = "Reset this skill when Custom mode is selected", section = smithingSection, position = 3)
    default boolean resetSmithing() { return false; }

    @ConfigSection(name = "Fishing", description = "Fishing XP multiplier settings", position = 28, closedByDefault = true)
    String fishingSection = "fishingSection";

    @ConfigItem(keyName = "overrideFishing", name = "Override Global Modifier", description = "Use custom XP modifier for Fishing instead of global", section = fishingSection, position = 1)
    default boolean overrideFishing() { return false; }

    @ConfigItem(keyName = "fishingMultiplier", name = "Fishing XP Modifier", description = "Multiplier for Fishing XP (only used if override is enabled)", section = fishingSection, position = 2)
    @Range(min = 1, max = 15)
    default double fishingMultiplier() { return 1.0; }

    @ConfigItem(keyName = "resetFishing", name = "Custom Mode: Reset Skill?", description = "Reset this skill when Custom mode is selected", section = fishingSection, position = 3)
    default boolean resetFishing() { return false; }

    @ConfigSection(name = "Cooking", description = "Cooking XP multiplier settings", position = 29, closedByDefault = true)
    String cookingSection = "cookingSection";

    @ConfigItem(keyName = "overrideCooking", name = "Override Global Modifier", description = "Use custom XP modifier for Cooking instead of global", section = cookingSection, position = 1)
    default boolean overrideCooking() { return false; }

    @ConfigItem(keyName = "cookingMultiplier", name = "Cooking XP Modifier", description = "Multiplier for Cooking XP (only used if override is enabled)", section = cookingSection, position = 2)
    @Range(min = 1, max = 15)
    default double cookingMultiplier() { return 1.0; }

    @ConfigItem(keyName = "resetCooking", name = "Custom Mode: Reset Skill?", description = "Reset this skill when Custom mode is selected", section = cookingSection, position = 3)
    default boolean resetCooking() { return false; }

    @ConfigSection(name = "Firemaking", description = "Firemaking XP multiplier settings", position = 30, closedByDefault = true)
    String firemakingSection = "firemakingSection";

    @ConfigItem(keyName = "overrideFiremaking", name = "Override Global Modifier", description = "Use custom XP modifier for Firemaking instead of global", section = firemakingSection, position = 1)
    default boolean overrideFiremaking() { return false; }

    @ConfigItem(keyName = "firemakingMultiplier", name = "Firemaking XP Modifier", description = "Multiplier for Firemaking XP (only used if override is enabled)", section = firemakingSection, position = 2)
    @Range(min = 1, max = 15)
    default double firemakingMultiplier() { return 1.0; }

    @ConfigItem(keyName = "resetFiremaking", name = "Custom Mode: Reset Skill?", description = "Reset this skill when Custom mode is selected", section = firemakingSection, position = 3)
    default boolean resetFiremaking() { return false; }

    @ConfigSection(name = "Woodcutting", description = "Woodcutting XP multiplier settings", position = 31, closedByDefault = true)
    String woodcuttingSection = "woodcuttingSection";

    @ConfigItem(keyName = "overrideWoodcutting", name = "Override Global Modifier", description = "Use custom XP modifier for Woodcutting instead of global", section = woodcuttingSection, position = 1)
    default boolean overrideWoodcutting() { return false; }

    @ConfigItem(keyName = "woodcuttingMultiplier", name = "Woodcutting XP Modifier", description = "Multiplier for Woodcutting XP (only used if override is enabled)", section = woodcuttingSection, position = 2)
    @Range(min = 1, max = 15)
    default double woodcuttingMultiplier() { return 1.0; }

    @ConfigItem(keyName = "resetWoodcutting", name = "Custom Mode: Reset Skill?", description = "Reset this skill when Custom mode is selected", section = woodcuttingSection, position = 3)
    default boolean resetWoodcutting() { return false; }

    @ConfigSection(name = "Farming", description = "Farming XP multiplier settings", position = 32, closedByDefault = true)
    String farmingSection = "farmingSection";

    @ConfigItem(keyName = "overrideFarming", name = "Override Global Modifier", description = "Use custom XP modifier for Farming instead of global", section = farmingSection, position = 1)
    default boolean overrideFarming() { return false; }

    @ConfigItem(keyName = "farmingMultiplier", name = "Farming XP Modifier", description = "Multiplier for Farming XP (only used if override is enabled)", section = farmingSection, position = 2)
    @Range(min = 1, max = 15)
    default double farmingMultiplier() { return 1.0; }

    @ConfigItem(keyName = "resetFarming", name = "Custom Mode: Reset Skill?", description = "Reset this skill when Custom mode is selected", section = farmingSection, position = 3)
    default boolean resetFarming() { return false; }

    @ConfigSection(name = "Sailing", description = "Sailing XP multiplier settings", position = 33, closedByDefault = true)
    String sailingSection = "sailingSection";

    @ConfigItem(keyName = "overrideSailing", name = "Override Global Modifier", description = "Use custom XP modifier for Sailing instead of global", section = sailingSection, position = 1)
    default boolean overrideSailing() { return false; }

    @ConfigItem(keyName = "sailingMultiplier", name = "Sailing XP Modifier", description = "Multiplier for Sailing XP (only used if override is enabled)", section = sailingSection, position = 2)
    @Range(min = 1, max = 15)
    default double sailingMultiplier() { return 1.0; }

    @ConfigItem(keyName = "resetSailing", name = "Custom Mode: Reset Skill?", description = "Reset this skill when Custom mode is selected", section = sailingSection, position = 3)
    default boolean resetSailing() { return false; }

    enum ResetMode
    {
        ALL_SKILLS("All Skills"),
        ONLY_99S("Only Lvl 99 Skills"),
        CUSTOM("Custom"),
        PER_SKILL_THRESHOLD("Per Skill Threshold");

        private final String name;

        ResetMode(String name)
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }
}