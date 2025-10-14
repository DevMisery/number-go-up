package com.NumberGoUp;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@PluginDescriptor(
        name = "Number Go Up!",
        description = "Reset stats visually back down to level 1 to watch those numbers go up!",
        tags = {"skills", "stats", "level", "progress", "xp"}
)
public class NumberGoUpPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private NumberGoUpConfig config;

    private boolean enabled = false;
    private Map<Skill, Integer> initialXp = new HashMap<>();
    private Map<Skill, Integer> currentRealXp = new HashMap<>();
    private Map<Skill, Integer> currentRealLevel = new HashMap<>();
    private Map<Skill, Integer> currentRealBoosted = new HashMap<>();

    // XP required for level 99
    private static final int XP_FOR_99 = 13034431;

    @Override
    protected void startUp() throws Exception
    {
        log.info("Number Go Up started! XP Modifier: {}, Reset Mode: {}", config.xpModifier(), config.resetMode());
        enabled = true;

        // Store initial values - with reset mode logic
        for (Skill skill : Skill.values())
        {
            if (skill != Skill.OVERALL)
            {
                int xp = client.getSkillExperience(skill);
                int level = client.getRealSkillLevel(skill);
                int boosted = client.getBoostedSkillLevel(skill);

                initialXp.put(skill, xp);
                currentRealXp.put(skill, xp);
                currentRealLevel.put(skill, level);
                currentRealBoosted.put(skill, boosted);
            }
        }

        updateAllStats();
    }

    @Override
    protected void shutDown() throws Exception
    {
        log.info("Number Go Up stopped!");
        enabled = false;

        // Restore original stats using the current real values
        for (Skill skill : Skill.values())
        {
            if (skill != Skill.OVERALL)
            {
                int realXp = currentRealXp.get(skill);
                int realLevel = currentRealLevel.get(skill);
                int realBoosted = currentRealBoosted.get(skill);

                // Restore the actual values
                client.getRealSkillLevels()[skill.ordinal()] = realLevel;
                client.getSkillExperiences()[skill.ordinal()] = realXp;
                client.getBoostedSkillLevels()[skill.ordinal()] = realBoosted;

                client.queueChangedSkill(skill);
            }
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged statChanged)
    {
        Skill skill = statChanged.getSkill();

        // Always update current real values with the actual values from the event
        currentRealXp.put(skill, statChanged.getXp());
        currentRealLevel.put(skill, statChanged.getLevel());
        currentRealBoosted.put(skill, statChanged.getBoostedLevel());

        if (enabled)
        {
            updateStat(skill);
        }
    }

    // NEW: Listen for config changes
    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!event.getGroup().equals("numbergoup")) {
            return;
        }

        // If reset mode changed, update all stats immediately
        if (event.getKey().equals("resetMode")) {
            log.info("Reset mode changed to: {}", config.resetMode());
            if (enabled) {
                updateAllStats();
            }
        }

        // If a custom reset checkbox changed and we're in custom mode, update that specific skill
        if (event.getKey().startsWith("reset") && config.resetMode() == NumberGoUpConfig.ResetMode.CUSTOM) {
            if (enabled) {
                // Extract skill name from key (e.g., "resetAttack" -> "ATTACK")
                String skillName = event.getKey().substring(5); // Remove "reset" prefix
                try {
                    Skill skill = Skill.valueOf(skillName.toUpperCase());
                    updateStat(skill);
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown skill from config key: {}", event.getKey());
                }
            }
        }

        // If XP modifier changed, update all stats
        if (event.getKey().equals("xpModifier") || event.getKey().endsWith("Multiplier")) {
            if (enabled) {
                updateAllStats();
            }
        }
    }

    private void updateAllStats()
    {
        for (Skill skill : Skill.values())
        {
            if (skill != Skill.OVERALL)
            {
                updateStat(skill);
            }
        }
    }

    private void updateStat(Skill skill)
    {
        if (!enabled || skill == Skill.OVERALL)
        {
            return;
        }

        int realXp = currentRealXp.get(skill);
        int initialSkillXp = initialXp.get(skill);

        // Get multiplier for this specific skill (respects override checkbox)
        double xpModifier = getXpModifierForSkill(skill);

        // Calculate session XP with modifier
        int gainedXp = realXp - initialSkillXp;
        int modifiedSessionXp = (int)(gainedXp * xpModifier);

        // Check if this skill should be reset based on reset mode
        boolean shouldReset = shouldResetSkill(skill, initialSkillXp);

        int sessionLevel;
        int displayXp;

        if (shouldReset)
        {
            // Reset skill behavior - start from level 1 (or 10 for HP)
            sessionLevel = Experience.getLevelForXp(modifiedSessionXp);
            displayXp = modifiedSessionXp;

            // Cap level at 99
            if (sessionLevel > 99)
            {
                sessionLevel = 99;
                displayXp = XP_FOR_99;
            }

            // Hitpoints starts at level 10 with 1,154 XP
            if (skill == Skill.HITPOINTS)
            {
                if (sessionLevel < 10)
                {
                    sessionLevel = 10;
                    displayXp = Experience.getXpForLevel(10); // 1,154 XP
                }
            }
        }
        else
        {
            // Non-reset skill behavior - show actual progression with multiplier applied
            displayXp = initialSkillXp + modifiedSessionXp;
            sessionLevel = Experience.getLevelForXp(displayXp);

            // Still cap at 99
            if (sessionLevel > 99)
            {
                sessionLevel = 99;
                displayXp = XP_FOR_99;
            }
        }

        // Update client to show session level (visual only)
        client.getRealSkillLevels()[skill.ordinal()] = sessionLevel;
        client.getSkillExperiences()[skill.ordinal()] = displayXp;
        client.getBoostedSkillLevels()[skill.ordinal()] = sessionLevel;

        client.queueChangedSkill(skill);
    }

    // Determine if a skill should be reset based on reset mode
    private boolean shouldResetSkill(Skill skill, int initialXp)
    {
        switch (config.resetMode())
        {
            case ALL_SKILLS:
                return true;
            case ONLY_99S:
                return initialXp >= XP_FOR_99;
            case CUSTOM:
                return shouldResetCustomSkill(skill);
            default:
                return true;
        }
    }

    // Check if a skill should be reset in Custom mode
    private boolean shouldResetCustomSkill(Skill skill)
    {
        switch (skill)
        {
            case ATTACK: return config.resetAttack();
            case STRENGTH: return config.resetStrength();
            case DEFENCE: return config.resetDefence();
            case RANGED: return config.resetRanged();
            case PRAYER: return config.resetPrayer();
            case MAGIC: return config.resetMagic();
            case RUNECRAFT: return config.resetRunecraft();
            case CONSTRUCTION: return config.resetConstruction();
            case HITPOINTS: return config.resetHitpoints();
            case AGILITY: return config.resetAgility();
            case HERBLORE: return config.resetHerblore();
            case THIEVING: return config.resetThieving();
            case CRAFTING: return config.resetCrafting();
            case FLETCHING: return config.resetFletching();
            case SLAYER: return config.resetSlayer();
            case HUNTER: return config.resetHunter();
            case MINING: return config.resetMining();
            case SMITHING: return config.resetSmithing();
            case FISHING: return config.resetFishing();
            case COOKING: return config.resetCooking();
            case FIREMAKING: return config.resetFiremaking();
            case WOODCUTTING: return config.resetWoodcutting();
            case FARMING: return config.resetFarming();
            default: return false;
        }
    }

    // Get XP modifier for specific skill (respects override checkbox)
    private double getXpModifierForSkill(Skill skill)
    {
        switch (skill)
        {
            case ATTACK:
                return config.overrideAttack() ? config.attackMultiplier() : config.xpModifier();
            case STRENGTH:
                return config.overrideStrength() ? config.strengthMultiplier() : config.xpModifier();
            case DEFENCE:
                return config.overrideDefence() ? config.defenceMultiplier() : config.xpModifier();
            case RANGED:
                return config.overrideRanged() ? config.rangedMultiplier() : config.xpModifier();
            case PRAYER:
                return config.overridePrayer() ? config.prayerMultiplier() : config.xpModifier();
            case MAGIC:
                return config.overrideMagic() ? config.magicMultiplier() : config.xpModifier();
            case RUNECRAFT:
                return config.overrideRunecraft() ? config.runecraftMultiplier() : config.xpModifier();
            case CONSTRUCTION:
                return config.overrideConstruction() ? config.constructionMultiplier() : config.xpModifier();
            case HITPOINTS:
                return config.overrideHitpoints() ? config.hitpointsMultiplier() : config.xpModifier();
            case AGILITY:
                return config.overrideAgility() ? config.agilityMultiplier() : config.xpModifier();
            case HERBLORE:
                return config.overrideHerblore() ? config.herbloreMultiplier() : config.xpModifier();
            case THIEVING:
                return config.overrideThieving() ? config.thievingMultiplier() : config.xpModifier();
            case CRAFTING:
                return config.overrideCrafting() ? config.craftingMultiplier() : config.xpModifier();
            case FLETCHING:
                return config.overrideFletching() ? config.fletchingMultiplier() : config.xpModifier();
            case SLAYER:
                return config.overrideSlayer() ? config.slayerMultiplier() : config.xpModifier();
            case HUNTER:
                return config.overrideHunter() ? config.hunterMultiplier() : config.xpModifier();
            case MINING:
                return config.overrideMining() ? config.miningMultiplier() : config.xpModifier();
            case SMITHING:
                return config.overrideSmithing() ? config.smithingMultiplier() : config.xpModifier();
            case FISHING:
                return config.overrideFishing() ? config.fishingMultiplier() : config.xpModifier();
            case COOKING:
                return config.overrideCooking() ? config.cookingMultiplier() : config.xpModifier();
            case FIREMAKING:
                return config.overrideFiremaking() ? config.firemakingMultiplier() : config.xpModifier();
            case WOODCUTTING:
                return config.overrideWoodcutting() ? config.woodcuttingMultiplier() : config.xpModifier();
            case FARMING:
                return config.overrideFarming() ? config.farmingMultiplier() : config.xpModifier();
            default:
                return config.xpModifier();
        }
    }

    @Provides
    NumberGoUpConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(NumberGoUpConfig.class);
    }
}