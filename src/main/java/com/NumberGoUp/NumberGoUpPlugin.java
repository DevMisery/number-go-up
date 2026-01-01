package com.NumberGoUp;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
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
    private ClientThread clientThread;

    @Inject
    private NumberGoUpConfig config;

    @Inject
    private ConfigManager configManager;

    private boolean enabled = false;

    // Simple session XP tracking - XP gained since last reset for each skill
    private Map<Skill, Integer> sessionXp = new HashMap<>();

    // Current real XP values (for non-reset skills)
    private Map<Skill, Integer> currentRealXp = new HashMap<>();

    // Track when plugin was last enabled
    private Map<Skill, Integer> xpWhenPluginDisabled = new HashMap<>();

    // Track reset button state
    private boolean lastResetButtonState = false;

    @Override
    protected void startUp() throws Exception
    {
        log.info("Number Go Up started! XP Modifier: {}, Reset Mode: {}", config.xpModifier(), config.resetMode());
        enabled = true;

        // Load saved session XP
        loadSessionXp();

        // Initialize session XP for any missing skills
        for (Skill skill : Skill.values())
        {
            if (skill != Skill.OVERALL)
            {
                int realXp = client.getSkillExperience(skill);
                currentRealXp.put(skill, realXp);

                if (!sessionXp.containsKey(skill))
                {
                    sessionXp.put(skill, 0);
                }

                // Clear disabled tracking
                xpWhenPluginDisabled.remove(skill);
            }
        }

        // Force update all stats on startup
        updateAllStats();

        // Initialize reset button state
        lastResetButtonState = config.resetProgress();
    }

    @Override
    protected void shutDown() throws Exception
    {
        log.info("Number Go Up stopped!");

        // Store current XP values when plugin is disabled
        if (enabled) {
            for (Skill skill : Skill.values()) {
                if (skill != Skill.OVERALL) {
                    xpWhenPluginDisabled.put(skill, currentRealXp.get(skill));
                }
            }
        }

        enabled = false;

        // Save session XP before shutting down
        saveSessionXp();

        // Restore original stats
        for (Skill skill : Skill.values())
        {
            if (skill != Skill.OVERALL)
            {
                int realXp = currentRealXp.get(skill);

                // Restore the actual values
                client.getRealSkillLevels()[skill.ordinal()] = Experience.getLevelForXp(realXp);
                client.getSkillExperiences()[skill.ordinal()] = realXp;
                client.getBoostedSkillLevels()[skill.ordinal()] = Experience.getLevelForXp(realXp);

                client.queueChangedSkill(skill);
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        // Check if reset button was toggled
        boolean currentResetButtonState = config.resetProgress();

        // Check for rising edge (false -> true transition)
        if (currentResetButtonState && !lastResetButtonState)
        {
            // Button was just pressed
            log.info("Reset progress button pressed!");

            // Show confirmation message
            if (client.getGameState() == GameState.LOGGED_IN && enabled)
            {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                        "Number Go Up! Resetting all progress...", null);
            }

            // Perform the reset - SIMPLE: set all session XP to 0
            clearSessionXp();

            // Reset the button state back to false
            clientThread.invokeLater(() -> {
                configManager.setConfiguration("numbergoup", "resetProgress", false);
            });
        }

        // Update the last state
        lastResetButtonState = currentResetButtonState;
    }

    // Listen for config changes
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
            // Skip if it's the reset button key
            if (event.getKey().equals("resetProgress")) {
                return;
            }

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

        // If a threshold value changed and we're in threshold mode, update that skill
        if (event.getKey().endsWith("Threshold") && config.resetMode() == NumberGoUpConfig.ResetMode.PER_SKILL_THRESHOLD) {
            if (enabled) {
                // Extract skill name from key (e.g., "attackThreshold" -> "ATTACK")
                String skillName = event.getKey().replace("Threshold", "");
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

    @Subscribe
    public void onStatChanged(StatChanged statChanged)
    {
        Skill skill = statChanged.getSkill();

        // Only update when plugin is enabled
        if (enabled)
        {
            int oldXp = currentRealXp.getOrDefault(skill, 0);
            int newXp = statChanged.getXp();
            int xpGained = newXp - oldXp;

            // Update current real XP
            currentRealXp.put(skill, newXp);

            // Add to session XP if this skill should be reset
            if (shouldResetSkill(skill) && xpGained > 0)
            {
                int currentSessionXp = sessionXp.getOrDefault(skill, 0);
                sessionXp.put(skill, currentSessionXp + xpGained);

                // Check threshold reset for PER_SKILL_THRESHOLD mode
                if (config.resetMode() == NumberGoUpConfig.ResetMode.PER_SKILL_THRESHOLD)
                {
                    checkThresholdReset(skill);
                }
            }

            updateStat(skill);

            // Save session XP
            saveSessionXp();
        }
        else
        {
            // Plugin is disabled - track XP for when plugin re-enables
            xpWhenPluginDisabled.put(skill, statChanged.getXp());
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
        int skillSessionXp = sessionXp.getOrDefault(skill, 0);

        // Get multiplier for this specific skill (respects override checkbox)
        double xpModifier = getXpModifierForSkill(skill);

        // Check if this skill should be reset based on reset mode
        boolean shouldReset = shouldResetSkill(skill);

        int displayXp;
        int sessionLevel;

        if (shouldReset)
        {
            // Reset skill behavior - use session XP with multiplier
            displayXp = (int)(skillSessionXp * xpModifier);
            sessionLevel = Experience.getLevelForXp(displayXp);

            // For display purposes, cap at level 99 but keep actual XP value
            if (sessionLevel > 99) {
                sessionLevel = 99;
            }

            // Hitpoints starts at level 10 (1154 XP)
            if (skill == Skill.HITPOINTS)
            {
                if (sessionLevel < 10)
                {
                    sessionLevel = 10;
                    // Set display XP to 1154 to show level 10
                    displayXp = 1154;
                }
            }
        }
        else
        {
            // Non-reset skill behavior - show actual progression with modifier applied
            displayXp = (int)(realXp * xpModifier);
            sessionLevel = Experience.getLevelForXp(realXp);

            // Cap level at 99 for display
            if (sessionLevel > 99) {
                sessionLevel = 99;
            }
        }

        // Update client to show session level (visual only)
        client.getRealSkillLevels()[skill.ordinal()] = sessionLevel;
        client.getSkillExperiences()[skill.ordinal()] = displayXp;
        client.getBoostedSkillLevels()[skill.ordinal()] = sessionLevel;

        client.queueChangedSkill(skill);
    }

    // Determine if a skill should be reset based on reset mode
    private boolean shouldResetSkill(Skill skill)
    {
        switch (config.resetMode())
        {
            case ALL_SKILLS:
                return true;
            case ONLY_99S:
                int currentXp = currentRealXp.get(skill);
                return currentXp >= Experience.getXpForLevel(99);
            case CUSTOM:
                return shouldResetCustomSkill(skill);
            case PER_SKILL_THRESHOLD:
                return shouldResetThresholdSkill(skill, sessionXp.getOrDefault(skill, 0));
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
            case SAILING: return config.resetSailing();
            default: return false;
        }
    }

    // Check if a skill should be reset in Threshold mode
    private boolean shouldResetThresholdSkill(Skill skill, int sessionXp)
    {
        int threshold = getThresholdForSkill(skill);

        // If threshold is 0, never reset
        if (threshold <= 0) {
            return false;
        }

        // Check if we've crossed the threshold
        return sessionXp >= threshold;
    }

    // Check if a skill has crossed its threshold and needs to be reset
    private void checkThresholdReset(Skill skill)
    {
        int threshold = getThresholdForSkill(skill);
        int currentSessionXp = sessionXp.getOrDefault(skill, 0);

        // If threshold is 0, do nothing
        if (threshold <= 0) {
            return;
        }

        // Check if we've crossed the threshold
        if (currentSessionXp >= threshold)
        {
            log.info("Skill {} reached threshold {} (Session XP: {}), resetting!",
                    skill.getName(), threshold, currentSessionXp);

            // Reset session XP for this skill
            sessionXp.put(skill, 0);

            // Save immediately
            saveSessionXp();

            // Update the skill display
            updateStat(skill);

            // Show notification
            if (client.getGameState() == GameState.LOGGED_IN && enabled)
            {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                        "Number Go Up! " + skill.getName() + " has been reset (reached threshold: " + threshold + " XP)", null);
            }
        }
    }

    // Get threshold for a specific skill
    private int getThresholdForSkill(Skill skill)
    {
        switch (skill)
        {
            case ATTACK: return config.attackThreshold();
            case STRENGTH: return config.strengthThreshold();
            case DEFENCE: return config.defenceThreshold();
            case RANGED: return config.rangedThreshold();
            case PRAYER: return config.prayerThreshold();
            case MAGIC: return config.magicThreshold();
            case RUNECRAFT: return config.runecraftThreshold();
            case CONSTRUCTION: return config.constructionThreshold();
            case HITPOINTS: return config.hitpointsThreshold();
            case AGILITY: return config.agilityThreshold();
            case HERBLORE: return config.herbloreThreshold();
            case THIEVING: return config.thievingThreshold();
            case CRAFTING: return config.craftingThreshold();
            case FLETCHING: return config.fletchingThreshold();
            case SLAYER: return config.slayerThreshold();
            case HUNTER: return config.hunterThreshold();
            case MINING: return config.miningThreshold();
            case SMITHING: return config.smithingThreshold();
            case FISHING: return config.fishingThreshold();
            case COOKING: return config.cookingThreshold();
            case FIREMAKING: return config.firemakingThreshold();
            case WOODCUTTING: return config.woodcuttingThreshold();
            case FARMING: return config.farmingThreshold();
            case SAILING: return config.sailingThreshold();
            default: return 0;
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
            case SAILING:
                return config.overrideSailing() ? config.sailingMultiplier() : config.xpModifier();
            default:
                return config.xpModifier();
        }
    }

    // Save session XP to config
    private void saveSessionXp()
    {
        if (!enabled) return;

        for (Skill skill : Skill.values())
        {
            if (skill != Skill.OVERALL)
            {
                String key = "sessionXp_" + skill.name().toLowerCase();
                configManager.setConfiguration("numbergoup", key, sessionXp.getOrDefault(skill, 0));
            }
        }
        log.debug("Session XP saved");
    }

    // Load session XP from config
    private void loadSessionXp()
    {
        for (Skill skill : Skill.values())
        {
            if (skill != Skill.OVERALL)
            {
                String key = "sessionXp_" + skill.name().toLowerCase();
                Integer savedXp = configManager.getConfiguration("numbergoup", key, Integer.class);
                if (savedXp != null)
                {
                    sessionXp.put(skill, savedXp);
                    log.debug("Loaded session XP for {}: {}", skill.getName(), savedXp);
                }
                else
                {
                    sessionXp.put(skill, 0);
                }
            }
        }
    }

    // Clear all session XP (reset progress)
    private void clearSessionXp()
    {
        log.info("Clearing all session XP...");

        // Clear all session XP
        for (Skill skill : Skill.values())
        {
            if (skill != Skill.OVERALL)
            {
                String key = "sessionXp_" + skill.name().toLowerCase();
                configManager.unsetConfiguration("numbergoup", key);
                sessionXp.put(skill, 0);
            }
        }

        // Immediately update all stats to show reset state
        updateAllStats();

        // Save the reset state immediately
        saveSessionXp();

        // Show a game message to confirm
        if (client.getGameState() == GameState.LOGGED_IN && enabled)
        {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Number Go Up! All progress has been reset.", null);
        }

        log.info("All progress has been cleared.");
    }

    @Provides
    NumberGoUpConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(NumberGoUpConfig.class);
    }
}