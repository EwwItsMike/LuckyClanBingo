package com.LuckyClan.Bingo;

import com.google.common.base.Splitter;
import com.google.inject.Provides;

import javax.imageio.ImageIO;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@PluginDescriptor(
        name = "Lucky Clan Bingo",
        description = "Sends a screenshot, player name and drop source to a Discord webhook when receiving a drop from a specified set of items.",
        tags = {"discord", "loot", "clan", "event", "bingo", "screenshot"}
)
public class LuckyClanBingoPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private LuckyClanBingoConfig config;

    @Inject
    private ItemManager itemManager;

    @Inject
    private ConfigManager configManager;

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
    private static String BIG_FISH = "You catch an enormous";


    private String webhookLink = "";

    public static final Set<String> SPECIAL_LOOT_NPC_NAMES = Set.of("The Whisperer", "Araxxor", "Branda the Fire Queen", "Eldric the Ice King", "Yama");

    @Override
    protected void startUp() throws Exception {
        log.info("Plugin starting...");
        webhookLink = config.webhookLink();
        loadItems();
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Plugin shutting down.");
    }

    private void loadItems() throws Exception {
        Request.Builder requestBuilder = new Request.Builder().url("https://raw.githubusercontent.com/EwwItsMike/LuckyClanBingo/refs/heads/master/src/main/resources/items.txt");

        okHttpClient.newCall(requestBuilder.get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("Could not load items list from Github.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()){
                    BufferedReader reader = new BufferedReader(response.body().charStream());
                    for (String line; (line = reader.readLine()) != null; ){
                        if (line.isEmpty() || line.charAt(0) == '#')
                            continue;
                        items.add(line.toLowerCase(Locale.ROOT).trim());
                    }

                    log.info("Successfully loaded items list.");

                    response.close();
                }
            }
        });
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!event.getGroup().equalsIgnoreCase("LuckyClanBingo"))
            return;

        log.info("Config changed -- updating Webhook link");
        webhookLink = config.webhookLink();

        if (config.testLink()){
            testLink();
        }
    }

    private void testLink(){
        if (config.webhookLink().isEmpty() || client.getGameState() != GameState.LOGGED_IN){
            configManager.unsetConfiguration("LuckyClanBingo", "testLink");
            return;
        }

        Request.Builder requestBuilder = new Request.Builder();
        try {
            requestBuilder.url(config.webhookLink());
        }
        catch (IllegalArgumentException e){
            sendChatMessage("Plugin failed to connect to the specified link.");
            return;
        }

        if (!config.apiKey().isEmpty()) {
            requestBuilder.header("X-API-Key", config.apiKey());
        }

        Request request = requestBuilder.get().build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                sendChatMessage("Lucky Clan Bingo plugin failed to connect to the specified link.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 200){
                    sendChatMessage("Lucky Clan Bingo plugin is successfully set up!");
                    response.close();
                }
                else {
                    sendChatMessage("Lucky Clan Bingo plugin could connect to the destination, but it responded with an error.");
                    response.close();
                }
            }
        });

        // Set config checkmark back to False
        configManager.unsetConfiguration("LuckyClanBingo", "testLink");
    }

    private void sendChatMessage(String message){
        clientThread.invokeLater(() -> client.addChatMessage(ChatMessageType.GAMEMESSAGE, "Lucky", message, null));
    }

    @Subscribe
    public void onNpcLootReceived(NpcLootReceived event) {
        lastLootSource = event.getNpc().getName();
        handleReceivedLoot(event.getItems());
    }

    @Subscribe
    public void onLootReceived(LootReceived event) {
        lastLootSource = event.getName();

        if (event.getType() == LootRecordType.NPC && SPECIAL_LOOT_NPC_NAMES.contains(event.getName())) {
            handleReceivedLoot(event.getItems());
        } else if (event.getType() == LootRecordType.EVENT || event.getType() == LootRecordType.PICKPOCKET || event.getType() == LootRecordType.PLAYER) {
            if (event.getName().equalsIgnoreCase("Loot Chest") || event.getType() == LootRecordType.PLAYER) {
                handlePkLoot(event.getItems());
            }
            handleReceivedLoot(event.getItems());
        }
    }

    //Source: Discord Loot Logger plugin by Adam
    private byte[] convertImageToByteArray(BufferedImage bufferedImage) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", out);
        return out.toByteArray();
    }

    private void handlePkLoot(Collection<ItemStack> loot) {
        int value = 0;

        for (ItemStack item : loot) {
            value += itemManager.getItemPrice(item.getId()) * item.getQuantity();
        }

        if (value < 1000000) {
            return;
        }

        int finalValue = value;
        drawManager.requestNextFrameListener(image -> {
            BufferedImage bufImg = (BufferedImage) image;
            byte[] bytes = null;
            try {
                bytes = convertImageToByteArray(bufImg);
            } catch (IOException e) {
                log.error("Cannot convert image to byte array.");
            }

            sendWebhook("A worse pker", 1, finalValue, "PKed goodies", bytes);
        });
    }

    private void handleReceivedLoot(Collection<ItemStack> drops) {
        AtomicReference<String> npcName = new AtomicReference<>(lastLootSource);

        if (items.isEmpty()) {
            log.error("Could not read items list. Retrying.");

            try {
                loadItems();
            } catch (Exception e) {
                log.error("Could not reload items list. Aborting.");
                return;
            }
        }

        List<String> alreadySent = new ArrayList<>();

        for (ItemStack item : drops) {
            ItemComposition comp = itemManager.getItemComposition(item.getId());
            String itemName = comp.getName().toLowerCase(Locale.ROOT);
            int value = itemManager.getItemPrice(item.getId()) * item.getQuantity();

            //Received item is in predefined list?
            if (items.stream().anyMatch(itemName::equalsIgnoreCase) && !alreadySent.contains(itemName)) {

                //Source: Discord Loot Logger plugin by Adam
                drawManager.requestNextFrameListener(image -> {
                    BufferedImage bufImg = (BufferedImage) image;
                    byte[] bytes = null;
                    try {
                        bytes = convertImageToByteArray(bufImg);
                    } catch (IOException ioe) {
                        log.error("Cannot convert image to byte array.");
                    }
                    alreadySent.add(itemName);
                    sendWebhook(npcName.get(), item.getQuantity(), value, itemName, bytes);
                });
            }
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage chatmessage) {
        if (chatmessage.getType() != ChatMessageType.GAMEMESSAGE)
            return;

        String message = chatmessage.getMessage();
        AtomicReference<String> npcName = new AtomicReference<String>(lastLootSource);

        if (message.contains(DUPE_CHAMPSCROLL)) {
            drawManager.requestNextFrameListener(image -> {
                BufferedImage bufImg = (BufferedImage) image;
                byte[] bytes = null;
                try {
                    bytes = convertImageToByteArray(bufImg);
                } catch (IOException ioe) {
                    log.error("Cannot convert image to byte array.");
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
                    log.error("Cannot convert image to byte array.");
                    return;
                }

                sendWebhook(npcName.get(), 1, 0, "Funny feeling...", bytes);
            });
        } else if (message.contains(BIG_FISH)){
            String fish = message.replace(BIG_FISH, "").replace("!", "");
            drawManager.requestNextFrameListener(image -> {
                BufferedImage bufImg = (BufferedImage) image;
                byte[] bytes = null;
                try {
                    bytes = convertImageToByteArray(bufImg);
                } catch (IOException ioe) {
                    log.error("Cannot convert image to byte array.");
                    return;
                }

                sendWebhook("Fishing spot", 1, 0, "Big " + fish, bytes);
            });
        }
    }

    //Source: Discord Loot Logger plugin by Adam
    private void sendWebhook(String npc, int itemQnty, int value, String itemName, byte[] screenshot) {
        WebhookBody webhookBody = new WebhookBody();
        StringBuilder stringBuilder = new StringBuilder();
        String playerName = client.getLocalPlayer().getName();

        stringBuilder.append("\n**").append(playerName).append("**").append(" received:\n\n");
        stringBuilder.append("*").append(String.format("%,d", itemQnty)).append(" x ").append(itemName).append("*\n");
        stringBuilder.append("From source: ").append("*").append(npc).append("*\n");
        stringBuilder.append("For a stack value of: ").append("*").append(String.format("%,d", value)).append(" gp*\n");

        stringBuilder.append("-# Powered by Lucky Clan Bingo plugin\n");
        webhookBody.setContent(stringBuilder.toString());

        if (webhookLink.isEmpty()) {
            log.error("Webhook url is not set.");
            return;
        }

        MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("payload_json", GSON.toJson(webhookBody));

        if (screenshot == null) {
            log.error("Screenshot was null.");
            return;
        }

        requestBodyBuilder.addFormDataPart("file", String.format("%s_%s.png", client.getLocalPlayer().getName(), LocalDateTime.now()),
                RequestBody.create(MediaType.parse("image/png"), screenshot));

        MultipartBody requestBody = requestBodyBuilder.build();

        List<String> urls = Splitter.on("\n").omitEmptyStrings().trimResults().splitToList(config.webhookLink());

        for (String url : urls) {
            HttpUrl u = HttpUrl.parse(url);
            if (u == null) {
                log.warn("Could not parse webhook url. Continuing with next link.");
                continue;
            }

            // If Api key provided, use it as header, if not then not
            Request.Builder requestBuilder = new Request.Builder().url(url);

            if (!config.apiKey().isEmpty()) {
                requestBuilder.header("X-API-Key", config.apiKey());
            }

            Request request = requestBuilder.post(requestBody).build();

            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    log.error("Could not submit webhook message.");
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
