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
import java.util.HashSet;
import java.util.Set;

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

    // Track XP gained since last reset for each skill
    private Map<Skill, Integer> sessionXp = new HashMap<>();

    // Track the last milestone XP for threshold mode
    private Map<Skill, Integer> lastMilestoneXp = new HashMap<>();

    // Track the last known XP for each skill to calculate gains
    private Map<Skill, Integer> lastKnownXp = new HashMap<>();

    // Track the XP at which we started resetting for each skill (for non-threshold modes)
    private Map<Skill, Integer> resetStartXp = new HashMap<>();

    // Track actual skill values (like Prestige plugin)
    private Map<Skill, Integer> actualSkillXp = new HashMap<>();
    private Map<Skill, Integer> actualSkillBoost = new HashMap<>();

    // Define combat skills
    private static final Set<Skill> COMBAT_SKILLS = new HashSet<Skill>() {{
        add(Skill.ATTACK);
        add(Skill.STRENGTH);
        add(Skill.DEFENCE);
        add(Skill.HITPOINTS);
        add(Skill.MAGIC);
        add(Skill.RANGED);
        add(Skill.PRAYER);
    }};

    @Override
    protected void startUp() throws Exception
    {
        log.info("Number Go Up started! XP Modifier: {}, Reset Mode: {}", config.xpModifier(), config.resetMode());
        enabled = true;

        // Initialize actual skill values
        for (Skill skill : Skill.values()) {
            if (skill != Skill.OVERALL) {
                int currentXp = client.getSkillExperience(skill);
                int currentBoost = client.getBoostedSkillLevel(skill);

                actualSkillXp.put(skill, currentXp);
                actualSkillBoost.put(skill, currentBoost);
                lastKnownXp.put(skill, currentXp);
            }
        }

        // Load saved data
        loadSessionXp();
        loadLastMilestoneXp();
        loadResetStartXp();

        // Initialize tracking based on current reset mode
        for (Skill skill : Skill.values()) {
            if (skill != Skill.OVERALL) {
                initializeSkillForCurrentMode(skill, actualSkillXp.get(skill));
            }
        }

        // Force update all stats on startup
        updateAllStats();

        // Save initial state
        saveSessionXp();
        saveLastMilestoneXp();
        saveResetStartXp();
    }

    private void initializeSkillForCurrentMode(Skill skill, int currentXp) {
        switch (config.resetMode()) {
            case NONE:
                // NONE mode - don't reset any skills
                sessionXp.put(skill, 0);
                resetStartXp.remove(skill);
                lastMilestoneXp.remove(skill);
                break;

            case PER_SKILL_THRESHOLD:
                int threshold = getThresholdForSkill(skill);
                if (threshold > 0) {
                    // If current XP is already above threshold, treat as if threshold was reached
                    int lastMilestone = 0;
                    if (currentXp >= threshold) {
                        // Find the highest multiple of threshold <= current XP
                        lastMilestone = (currentXp / threshold) * threshold;
                    }
                    lastMilestoneXp.put(skill, lastMilestone);
                    sessionXp.put(skill, currentXp - lastMilestone);
                    log.debug("Initialized threshold for {}: current={}, threshold={}, lastMilestone={}, session={}",
                            skill.getName(), currentXp, threshold, lastMilestone, currentXp - lastMilestone);
                } else {
                    sessionXp.put(skill, 0);
                    lastMilestoneXp.remove(skill);
                }
                break;

            default:
                // For other modes (ALL_SKILLS, NINETY_NINES_ONLY, COMBAT_ONLY, NON_COMBAT_ONLY, CUSTOM)
                if (shouldResetSkill(skill)) {
                    // If we have a saved reset start XP, use it to calculate session XP
                    if (resetStartXp.containsKey(skill)) {
                        sessionXp.put(skill, currentXp - resetStartXp.get(skill));
                    } else {
                        // First time resetting - start from current XP
                        resetStartXp.put(skill, currentXp);
                        sessionXp.put(skill, 0);
                    }
                } else {
                    sessionXp.put(skill, 0);
                    resetStartXp.remove(skill);
                }
                break;
        }
    }

    @Override
    protected void shutDown() throws Exception
    {
        log.info("Number Go Up stopped!");
        enabled = false;

        // Save session XP and milestones before shutting down
        saveSessionXp();
        saveLastMilestoneXp();
        saveResetStartXp();

        // Restore original stats
        restoreAllOriginalValues(true);

        // Force a full client refresh to ensure visual update
        clientThread.invokeLater(() -> {
            if (client.getGameState() == GameState.LOGGED_IN) {
                for (Skill skill : Skill.values()) {
                    if (skill != Skill.OVERALL) {
                        client.queueChangedSkill(skill);
                    }
                }
            }
        });
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!event.getGroup().equals("numbergoup")) {
            return;
        }

        // If reset mode changed, update all stats immediately
        if (event.getKey().equals("resetMode")) {
            log.info("Reset mode changed to: {}", config.resetMode());

            // First restore all skills to their original values
            restoreAllOriginalValues(false);

            if (config.keepProgressBetweenModes()) {
                // Keep progress between modes - preserve session XP for skills that are still being reset
                log.info("Keeping progress between reset modes");

                // Create temporary maps to preserve progress for skills that will still be reset
                Map<Skill, Integer> preservedSessionXp = new HashMap<>();
                Map<Skill, Integer> preservedResetStartXp = new HashMap<>();
                Map<Skill, Integer> preservedLastMilestoneXp = new HashMap<>();

                // Check each skill to see if it will still be reset in the new mode
                for (Skill skill : Skill.values()) {
                    if (skill != Skill.OVERALL) {
                        int currentXp = actualSkillXp.get(skill);
                        lastKnownXp.put(skill, currentXp);

                        boolean willResetInNewMode = shouldResetSkill(skill);
                        boolean wasResettingPreviously = sessionXp.containsKey(skill) && sessionXp.get(skill) > 0;

                        if (willResetInNewMode) {
                            // Skill will be reset in new mode - preserve its progress
                            if (wasResettingPreviously) {
                                // Keep existing progress
                                preservedSessionXp.put(skill, sessionXp.get(skill));
                                if (resetStartXp.containsKey(skill)) {
                                    preservedResetStartXp.put(skill, resetStartXp.get(skill));
                                }
                                if (lastMilestoneXp.containsKey(skill)) {
                                    preservedLastMilestoneXp.put(skill, lastMilestoneXp.get(skill));
                                }
                                log.debug("Preserved progress for {}: sessionXp={}", skill.getName(), sessionXp.get(skill));
                            } else {
                                // Skill wasn't being reset before but will be now - initialize fresh
                                initializeSkillForCurrentMode(skill, currentXp);
                            }
                        } else {
                            // Skill won't be reset in new mode - clear its progress
                            sessionXp.put(skill, 0);
                            resetStartXp.remove(skill);
                            lastMilestoneXp.remove(skill);
                        }
                    }
                }

                // Update the main maps with preserved values
                for (Skill skill : preservedSessionXp.keySet()) {
                    sessionXp.put(skill, preservedSessionXp.get(skill));
                }
                for (Skill skill : preservedResetStartXp.keySet()) {
                    resetStartXp.put(skill, preservedResetStartXp.get(skill));
                }
                for (Skill skill : preservedLastMilestoneXp.keySet()) {
                    lastMilestoneXp.put(skill, preservedLastMilestoneXp.get(skill));
                }

            } else {
                // Don't keep progress - clear everything and start fresh
                log.info("Resetting progress for new reset mode");

                lastKnownXp.clear();
                resetStartXp.clear();
                lastMilestoneXp.clear();
                sessionXp.clear();

                for (Skill skill : Skill.values()) {
                    if (skill != Skill.OVERALL) {
                        int currentXp = actualSkillXp.get(skill);
                        lastKnownXp.put(skill, currentXp);

                        initializeSkillForCurrentMode(skill, currentXp);
                    }
                }
            }

            saveSessionXp();
            saveLastMilestoneXp();
            saveResetStartXp();

            if (enabled) {
                updateAllStats();
            }
        }

        // If the keepProgressBetweenModes setting itself changed
        if (event.getKey().equals("keepProgressBetweenModes")) {
            log.info("Keep progress between modes changed to: {}", config.keepProgressBetweenModes());
            // No immediate action needed - this will affect future mode changes
        }

        // If a custom reset checkbox changed and we're in custom mode, update that specific skill
        if (event.getKey().startsWith("reset") && config.resetMode() == NumberGoUpConfig.ResetMode.CUSTOM) {
            if (enabled) {
                String skillName = event.getKey().substring(5);
                try {
                    Skill skill = Skill.valueOf(skillName.toUpperCase());
                    int currentXp = actualSkillXp.get(skill);

                    if (shouldResetSkill(skill)) {
                        // Skill is now being reset
                        resetStartXp.put(skill, currentXp);
                        sessionXp.put(skill, 0);
                    } else {
                        // Skill is no longer being reset
                        resetStartXp.remove(skill);
                        sessionXp.put(skill, 0);
                    }

                    saveResetStartXp();
                    saveSessionXp();
                    updateStat(skill);
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown skill from config key: {}", event.getKey());
                }
            }
        }

        // If a threshold value changed and we're in threshold mode
        if (event.getKey().endsWith("Threshold") && config.resetMode() == NumberGoUpConfig.ResetMode.PER_SKILL_THRESHOLD) {
            if (enabled) {
                String skillName = event.getKey().replace("Threshold", "");
                try {
                    Skill skill = Skill.valueOf(skillName.toUpperCase());
                    int currentXp = actualSkillXp.get(skill);
                    int threshold = getThresholdForSkill(skill);

                    lastKnownXp.put(skill, currentXp);

                    if (threshold > 0) {
                        // If current XP is already above threshold, treat as if threshold was reached
                        int lastMilestone = 0;
                        if (currentXp >= threshold) {
                            lastMilestone = (currentXp / threshold) * threshold;
                        }
                        lastMilestoneXp.put(skill, lastMilestone);
                        sessionXp.put(skill, currentXp - lastMilestone);
                        saveLastMilestoneXp();
                        saveSessionXp();
                    } else {
                        lastMilestoneXp.remove(skill);
                        sessionXp.put(skill, 0);
                        saveLastMilestoneXp();
                        saveSessionXp();
                    }

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

        // Update actual skill values
        actualSkillXp.put(skill, statChanged.getXp());
        actualSkillBoost.put(skill, statChanged.getBoostedLevel());

        // Only update when plugin is enabled and not in NONE mode
        if (enabled && skill != Skill.OVERALL && config.resetMode() != NumberGoUpConfig.ResetMode.NONE)
        {
            int newXp = statChanged.getXp();
            int oldXp = lastKnownXp.getOrDefault(skill, newXp);
            int xpGained = newXp - oldXp;

            // Update last known XP
            lastKnownXp.put(skill, newXp);

            // For threshold mode
            if (config.resetMode() == NumberGoUpConfig.ResetMode.PER_SKILL_THRESHOLD)
            {
                handleThresholdMode(skill, newXp);
            }
            else
            {
                // For other modes
                if (shouldResetSkill(skill))
                {
                    // Check if we just started resetting this skill
                    if (!resetStartXp.containsKey(skill)) {
                        resetStartXp.put(skill, newXp - xpGained);
                    }

                    // Calculate session XP from reset start point
                    int resetStart = resetStartXp.get(skill);
                    sessionXp.put(skill, newXp - resetStart);
                }
                else
                {
                    // Skill shouldn't be reset
                    resetStartXp.remove(skill);
                    sessionXp.put(skill, 0);
                }
            }

            updateStat(skill);
            saveSessionXp();
            saveResetStartXp();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        // Continuously update stats to maintain visual override
        if (enabled && client.getGameState() == GameState.LOGGED_IN)
        {
            updateAllStats();
        }
    }

    private void handleThresholdMode(Skill skill, int currentRealXp)
    {
        int threshold = getThresholdForSkill(skill);

        // If threshold is 0, don't reset this skill
        if (threshold <= 0) {
            return;
        }

        // Get last milestone
        int lastMilestone = lastMilestoneXp.getOrDefault(skill, 0);

        // Check if we've crossed a threshold
        int currentMilestone = (currentRealXp / threshold) * threshold;

        // If we haven't set a milestone yet and current XP >= threshold, set the initial milestone
        if (lastMilestone == 0 && currentRealXp >= threshold) {
            lastMilestone = currentMilestone;
            lastMilestoneXp.put(skill, lastMilestone);
            sessionXp.put(skill, currentRealXp - lastMilestone);
            saveLastMilestoneXp();
            saveSessionXp();
            return;
        }

        // Check if we've passed any milestones
        if (currentMilestone > lastMilestone)
        {
            int thresholdsPassed = (currentMilestone - lastMilestone) / threshold;

            // Update last milestone
            lastMilestone = currentMilestone;
            lastMilestoneXp.put(skill, lastMilestone);

            // Session XP is the difference from current milestone
            int sessionXpValue = currentRealXp - lastMilestone;
            sessionXp.put(skill, sessionXpValue);

            // Save the new state
            saveLastMilestoneXp();
            saveSessionXp();

            // Show notification
            if (client.getGameState() == GameState.LOGGED_IN && enabled && thresholdsPassed > 0)
            {
                String message = "Number Go Up! " + skill.getName() + " has been reset ";
                if (thresholdsPassed > 1) {
                    message += "(passed " + thresholdsPassed + " thresholds of " + threshold + " XP)";
                } else {
                    message += "(reached threshold: " + threshold + " XP)";
                }
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
            }

            log.debug("Threshold reset for {}: passed {} thresholds, new milestone={}, session={}",
                    skill.getName(), thresholdsPassed, lastMilestone, sessionXpValue);
        }
        else
        {
            // Update session XP to current difference from last milestone
            int sessionXpValue = currentRealXp - lastMilestone;
            sessionXp.put(skill, sessionXpValue);
            saveSessionXp();
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

        // Get multiplier for this specific skill
        double xpModifier = getXpModifierForSkill(skill);

        // Check if this skill should be reset based on reset mode
        boolean shouldReset = shouldResetSkill(skill);

        if (shouldReset)
        {
            // Reset skill behavior - use session XP with multiplier
            int skillSessionXp = sessionXp.getOrDefault(skill, 0);
            int displayXp = (int)(skillSessionXp * xpModifier);

            // SPECIAL CASE: Hitpoints starts at level 10 (1154 XP), not level 1 (0 XP)
            if (skill == Skill.HITPOINTS)
            {
                // Hitpoints starts at 1154 XP (level 10)
                displayXp = 1154 + displayXp;
            }

            // For display purposes, cap at level 99
            if (displayXp > 200000000) {
                displayXp = 200000000;
            }

            int displayLevel = Experience.getLevelForXp(displayXp);

            // Cap at level 99 for display
            if (displayLevel > 99) {
                displayLevel = 99;
            }

            // For ALL_SKILLS mode, we need to show level 1 with 0 XP when session XP is 0
            // Except for Hitpoints which starts at level 10
            if (config.resetMode() == NumberGoUpConfig.ResetMode.ALL_SKILLS)
            {
                if (skill != Skill.HITPOINTS && skillSessionXp == 0)
                {
                    displayLevel = 1;
                    displayXp = 0;
                }
                // Hitpoints in ALL_SKILLS mode with no session XP should show level 10 (1154 XP)
                else if (skill == Skill.HITPOINTS && skillSessionXp == 0)
                {
                    displayLevel = 10;
                    displayXp = 1154;
                }
            }

            // Update client to show display level
            client.getRealSkillLevels()[skill.ordinal()] = displayLevel;
            client.getSkillExperiences()[skill.ordinal()] = displayXp;
            client.getBoostedSkillLevels()[skill.ordinal()] = displayLevel;

            client.queueChangedSkill(skill);
        }
        else
        {
            // Non-reset skill behavior - restore to original values
            restoreSkillToOriginalValues(skill);
        }
    }

    // Restore a skill to its original values
    private void restoreSkillToOriginalValues(Skill skill) {
        if (skill == Skill.OVERALL) return;

        int realXp = actualSkillXp.getOrDefault(skill, client.getSkillExperience(skill));
        int realLevel = Experience.getLevelForXp(realXp);
        int boostedLevel = actualSkillBoost.getOrDefault(skill, client.getBoostedSkillLevel(skill));

        // Cap at level 99 for display
        if (realLevel > 99) {
            realLevel = 99;
        }
        if (boostedLevel > 99) {
            boostedLevel = 99;
        }

        client.getRealSkillLevels()[skill.ordinal()] = realLevel;
        client.getSkillExperiences()[skill.ordinal()] = realXp;
        client.getBoostedSkillLevels()[skill.ordinal()] = boostedLevel;

        client.queueChangedSkill(skill);
    }

    // Restore all skills to original values
    private void restoreAllOriginalValues(boolean isShutdown) {
        for (Skill skill : Skill.values()) {
            if (skill != Skill.OVERALL) {
                restoreSkillToOriginalValues(skill);
            }
        }
    }

    // Determine if a skill should be reset based on reset mode
    private boolean shouldResetSkill(Skill skill)
    {
        switch (config.resetMode())
        {
            case NONE:
                return false;
            case ALL_SKILLS:
                return true; // ALL_SKILLS mode resets all skills
            case NINETY_NINES_ONLY:
                int currentXp = actualSkillXp.getOrDefault(skill, client.getSkillExperience(skill));
                return currentXp >= 13034431; // XP for level 99
            case COMBAT_ONLY:
                return COMBAT_SKILLS.contains(skill);
            case NON_COMBAT_ONLY:
                return !COMBAT_SKILLS.contains(skill) && skill != Skill.OVERALL;
            case CUSTOM:
                return shouldResetCustomSkill(skill);
            case PER_SKILL_THRESHOLD:
                int threshold = getThresholdForSkill(skill);
                return threshold > 0; // Only reset if threshold is set (> 0)
            default:
                return false;
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

    // Get XP modifier for specific skill
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
                }
                else
                {
                    sessionXp.put(skill, 0);
                }
            }
        }
    }

    // Save last milestone XP to config
    private void saveLastMilestoneXp()
    {
        if (!enabled) return;

        for (Skill skill : Skill.values())
        {
            if (skill != Skill.OVERALL)
            {
                String key = "lastMilestone_" + skill.name().toLowerCase();
                Integer milestone = lastMilestoneXp.get(skill);
                if (milestone != null)
                {
                    configManager.setConfiguration("numbergoup", key, milestone);
                }
                else
                {
                    configManager.unsetConfiguration("numbergoup", key);
                }
            }
        }
    }

    // Load last milestone XP from config
    private void loadLastMilestoneXp()
    {
        for (Skill skill : Skill.values())
        {
            if (skill != Skill.OVERALL)
            {
                String key = "lastMilestone_" + skill.name().toLowerCase();
                Integer savedMilestone = configManager.getConfiguration("numbergoup", key, Integer.class);
                if (savedMilestone != null)
                {
                    lastMilestoneXp.put(skill, savedMilestone);
                }
            }
        }
    }

    // Save reset start XP to config
    private void saveResetStartXp()
    {
        if (!enabled) return;

        for (Skill skill : Skill.values())
        {
            if (skill != Skill.OVERALL)
            {
                String key = "resetStartXp_" + skill.name().toLowerCase();
                Integer resetStart = resetStartXp.get(skill);
                if (resetStart != null)
                {
                    configManager.setConfiguration("numbergoup", key, resetStart);
                }
                else
                {
                    configManager.unsetConfiguration("numbergoup", key);
                }
            }
        }
    }

    // Load reset start XP from config
    private void loadResetStartXp()
    {
        for (Skill skill : Skill.values())
        {
            if (skill != Skill.OVERALL)
            {
                String key = "resetStartXp_" + skill.name().toLowerCase();
                Integer savedResetStart = configManager.getConfiguration("numbergoup", key, Integer.class);
                if (savedResetStart != null)
                {
                    resetStartXp.put(skill, savedResetStart);
                }
            }
        }
    }

    @Provides
    NumberGoUpConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(NumberGoUpConfig.class);
    }
}