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
            description = "Discord webhook link or API proxy URL"
    ) default String webhookLink(){
        return "";
    }
    
    @ConfigItem(
            keyName = "apiKey",
            name = "API Key",
            description = "API key for authentication with proxy (Only leave empty if using discord webhook)",
            secret = true
    ) default String apiKey(){
        return "";
    }

    @ConfigItem(
            keyName = "testLink",
            name = "Test Link",
            description = "Test if your link is set correctly by checking this.",
            position = 3
    )
    default boolean testLink(){
        return false;
    }
}
