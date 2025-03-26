package com.LuckyClan.Bingo;

import com.google.common.base.Splitter;
import com.google.inject.Provides;

import javax.imageio.ImageIO;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.NPC;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.DrawManager;
import net.runelite.http.api.loottracker.LootRecordType;
import okhttp3.*;

import static net.runelite.http.api.RuneLiteAPI.GSON;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@PluginDescriptor(
        name = "LuckyClanBingo",
        description = "Sends a screenshot, player name and drop source to a Discord webhook when receiving a drop from a specified set of items.",
        tags = {"discord", "loot", "clan", "event", "bingo"}
)
public class LuckyClanBingoPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private LuckyClanBingoConfig config;

    @Inject
    private ItemManager itemManager;

    @Inject
    private DrawManager drawManager;

    @Inject
    private OkHttpClient okHttpClient;

    private ArrayList<String> items = new ArrayList<>();

    private String lastLootSource = "";

    private static String DUPE_CHAMPSCROLL = "You have a funny feeling that you would have received a Champion's scroll";
    private static String PET = "You have a funny feeling like you're being followed";
    private static String DUPE_PET = "You have a funny feeling like you would have been followed";
    private static String INVENT_PET = "You feel something weird sneaking into your backpack";


    @Override
    protected void startUp() throws Exception {

        System.out.println("[Lucky Clan Bingo] plugin starting...");

        try {
            if (!loadItems())
                System.out.println("[Lucky Clan Bingo] ERROR - could not load.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    protected void shutDown() throws Exception {
        System.out.println("[Lucky Clan Bingo] shutting down.");
    }

    private boolean loadItems() throws Exception {

        try (InputStream s = getClass().getResourceAsStream("/items.txt");
             InputStreamReader streamReader = new InputStreamReader(s, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)) {

            for (String line; (line = reader.readLine()) != null; ) {
                items.add(line.toLowerCase(Locale.ROOT).trim());
            }

            return true;
        } catch (Exception e) {
            System.out.println("[Lucky Clan Bingo] ERROR - could not open items list.");
            e.printStackTrace();
        }
        return false;
    }

    @Subscribe
    public void onNpcLootReceived(NpcLootReceived event) {
        lastLootSource = event.getNpc().getName();
        handleReceivedLoot(event.getItems());
    }

    @Subscribe
    public void onLootReceived(LootReceived event){
        if (event.getType() != LootRecordType.EVENT && event.getType() != LootRecordType.PICKPOCKET){
            return;
        }

        lastLootSource = event.getName();
        handleReceivedLoot(event.getItems());
    }

    //Source: Discord Loot Logger plugin by Adam
    private byte[] convertImageToByteArray(BufferedImage bufferedImage) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", out);
        return out.toByteArray();
    }

    private void handleReceivedLoot(Collection<ItemStack> drops) {
        AtomicReference<String> npcName = new AtomicReference<>(lastLootSource);

        if (items.isEmpty()) {
            System.out.println("[Lucky Clan Bingo] ERROR - could not read items list. Retrying.");

            try {
                loadItems();
            } catch (Exception e) {
                System.out.println("[Lucky Clan Bingo] ERROR - could not reload items list. Aborting.");
                return;
            }
        }

        for (ItemStack item : drops) {
            ItemComposition comp = itemManager.getItemComposition(item.getId());
            String itemName = comp.getName().toLowerCase(Locale.ROOT);
            int value = itemManager.getItemPrice(item.getId()) * item.getQuantity();

            //Received item is in predefined list?
            if (items.stream().anyMatch(itemName::equalsIgnoreCase)) {

                //Source: Discord Loot Logger plugin by Adam
                drawManager.requestNextFrameListener(image -> {
                    BufferedImage bufImg = (BufferedImage) image;
                    byte[] bytes = null;
                    try {
                        bytes = convertImageToByteArray(bufImg);
                    } catch (IOException ioe) {
                        System.out.println("Lucky Clan Bingo] ERROR - Cannot convert image to byte array.");
                    }

                    sendWebhook(npcName.get(), item.getQuantity(), value, itemName, bytes);
                });
            }
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage chatmessage) {

        if (chatmessage.getType() != ChatMessageType.GAMEMESSAGE)
            return;

        new Thread(() -> {
            String message = chatmessage.getMessage();
            AtomicReference<String> npcName = new AtomicReference<String>();

            try {
                //Attempt to wait for a little bit, to set npc name correctly
                Thread.sleep(50);
            } catch (InterruptedException e) {
                System.out.println("[Lucky Clan Bingo] WARNING - thread sleeping was interrupted.");
            }

            npcName.set(lastLootSource);

            if (message.contains(DUPE_CHAMPSCROLL)) {
                drawManager.requestNextFrameListener(image -> {
                    BufferedImage bufImg = (BufferedImage) image;
                    byte[] bytes = null;
                    try {
                        bytes = convertImageToByteArray(bufImg);
                    } catch (IOException ioe) {
                        System.out.println("Lucky Clan Bingo] ERROR - Cannot convert image to byte array.");
                        return;
                    }

                    sendWebhook(npcName.get(), 1, 0, "Duplicate champion's scroll", bytes);
                });
            } else if (message.contains(PET) || message.contains(DUPE_PET) || message.contains(INVENT_PET)) {
                drawManager.requestNextFrameListener(image -> {
                    BufferedImage bufImg = (BufferedImage) image;
                    byte[] bytes = null;
                    try {
                        bytes = convertImageToByteArray(bufImg);
                    } catch (IOException ioe) {
                        System.out.println("Lucky Clan Bingo] ERROR - Cannot convert image to byte array.");
                        return;
                    }

                    sendWebhook(npcName.get(), 1, 0, "Funny feeling...", bytes);
                });
            }
        }).start();
    }

    //Source: Discord Loot Logger plugin by Adam
    private void sendWebhook(String npc, int itemQnty, int value, String itemName, byte[] screenshot) {
        WebhookBody webhookBody = new WebhookBody();
        StringBuilder stringBuilder = new StringBuilder();
        String playerName = client.getLocalPlayer().getName();

        stringBuilder.append("\n**").append(playerName).append("**").append(" received:\n\n");
        stringBuilder.append("*").append(itemQnty).append(" x ").append(itemName).append("*\n");
        stringBuilder.append("From source: ").append("*").append(npc).append("*\n");
        stringBuilder.append("For a stack value of: ").append("*").append(value).append(" gp*\n");
        stringBuilder.append("-# Powered by Lucky Clan Bingo plugin\n");
        webhookBody.setContent(stringBuilder.toString());

        String webhookUrl = config.webhookLink();
        if (webhookUrl.isEmpty()) {
            System.out.println("[Lucky Clan Bingo] ERROR - Webhook url is not set.");
            return;
        }

        MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("payload_json", GSON.toJson(webhookBody));

        if (screenshot == null) {
            System.out.println("[Lucky Clan Bingo] ERROR - Screenshot was null.");
            return;
        }

        requestBodyBuilder.addFormDataPart("file", "image.png",
                RequestBody.create(MediaType.parse("image/png"), screenshot));

        MultipartBody requestBody = requestBodyBuilder.build();

        List<String> urls = Splitter.on("\n").omitEmptyStrings().trimResults().splitToList(config.webhookLink());

        for (String url : urls) {
            HttpUrl u = HttpUrl.parse(url);
            if (u == null) {
                System.out.println("[Lucky Clan Bingo] WARNING - Could not parse webhook url. Continuing with next link.");
                continue;
            }

            Request request = new Request.Builder().url(url).post(requestBody).build();

            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    System.out.println("[Lucky Clan Bingo] ERROR - could not submit webhook message.");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    response.close();
                }
            });
        }
    }

    @Provides
    LuckyClanBingoConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(LuckyClanBingoConfig.class);
    }
}
