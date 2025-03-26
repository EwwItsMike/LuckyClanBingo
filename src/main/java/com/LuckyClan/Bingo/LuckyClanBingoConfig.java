package com.LuckyClan.Bingo;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("LuckyClanBingo")
public interface LuckyClanBingoConfig extends Config
{

    @ConfigItem(
            keyName = "Webhook",
            name = "Webhook link",
            description = "Discord webhook link"
    ) default String webhookLink(){
        return "";
    }
}
