package com.NumberGoUp;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("numbergoup")
public interface NumberGoUpConfig extends Config
{
    @ConfigItem(
            keyName = "xpModifier",
            name = "XP Modifier",
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
}