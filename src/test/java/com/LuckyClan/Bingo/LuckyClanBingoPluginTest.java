package com.LuckyClan.Bingo;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class LuckyClanBingoPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(LuckyClanBingoPlugin.class);
		RuneLite.main(args);
	}
}