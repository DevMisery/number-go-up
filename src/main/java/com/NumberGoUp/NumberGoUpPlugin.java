package com.NumberGoUp;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
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

    @Override
    protected void startUp() throws Exception
    {
        log.info("Number Go Up started! XP Modifier: {}", config.xpModifier());
        enabled = true;

        // Store initial values
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
        double xpModifier = config.xpModifier();

        // Calculate session XP with modifier
        int gainedXp = realXp - initialXp.get(skill);
        int modifiedSessionXp = (int)(gainedXp * xpModifier);
        int sessionLevel = Experience.getLevelForXp(modifiedSessionXp);

        // Cap level at 99
        if (sessionLevel > 99)
        {
            sessionLevel = 99;
            modifiedSessionXp = Experience.getXpForLevel(99); // 13,034,431 XP
        }

        // Hitpoints starts at level 10 with 1,154 XP
        if (skill == Skill.HITPOINTS)
        {
            if (sessionLevel < 10)
            {
                sessionLevel = 10;
                modifiedSessionXp = Experience.getXpForLevel(10); // 1,154 XP
            }
        }

        // Update client to show session level (visual only)
        client.getRealSkillLevels()[skill.ordinal()] = sessionLevel;
        client.getSkillExperiences()[skill.ordinal()] = modifiedSessionXp;
        client.getBoostedSkillLevels()[skill.ordinal()] = sessionLevel;

        client.queueChangedSkill(skill);
    }

    @Provides
    NumberGoUpConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(NumberGoUpConfig.class);
    }
}