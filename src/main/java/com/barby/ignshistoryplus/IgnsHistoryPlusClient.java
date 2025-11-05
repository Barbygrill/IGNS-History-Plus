package com.barby.ignshistoryplus;

import com.barby.ignshistoryplus.gui.NameHistoryScreen;
import com.barby.ignshistoryplus.util.CraftyApi;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.PlayerSkinWidget;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.function.Supplier;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.util.ApiServices;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import com.barby.ignshistoryplus.mixin.MinecraftClientAccessor;

import java.util.ArrayList;
import java.util.List;


public class IgnsHistoryPlusClient implements ClientModInitializer {
    private static final Gson GSON = new Gson();

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            
            dispatcher.register(
                    ClientCommandManager.literal("igns")
                            .then(ClientCommandManager.argument("username", StringArgumentType.word())
                                    .suggests((ctx, builder) -> suggestOnlinePlayers(builder))
                                    .executes(ctx -> {
                                        String username = StringArgumentType.getString(ctx, "username");
                                        handleLookup(username, true);
                                        return 1;
                                    }))
            );

            
            dispatcher.register(
                    ClientCommandManager.literal("names")
                            .then(ClientCommandManager.argument("username", StringArgumentType.word())
                                    .suggests((ctx, builder) -> suggestOnlinePlayers(builder))
                                    .executes(ctx -> {
                                        String username = StringArgumentType.getString(ctx, "username");
                                        handleLookup(username, true);
                                        return 1;
                                    }))
            );
        });
    }

    
    private void handleLookup(String username, boolean showHead) {
        
        MinecraftClient.getInstance().execute(() -> {
            
            MinecraftClient.getInstance().setScreen(null);
            sendChat(Text.of("§eSearching for player " + username + "..."));
        });

        
        new Thread(() -> {
            try {
                String body = CraftyApi.getPlayerJson(username);
                if (body == null) {
                    
                    sendChat(Text.of("§cFailed to contact the API. Please try again later."));
                    return;
                }

                JsonObject root = GSON.fromJson(body, JsonObject.class);
                boolean success = root.has("success") && root.get("success").getAsBoolean();
                
                if (!success) {
                    sendChat(Text.of("§cPlayer not found."));
                    return;
                }
                if (!root.has("data") || !root.get("data").isJsonObject()) {
                    sendChat(Text.of("§cInvalid response from API."));
                    return;
                }

                JsonObject data = root.getAsJsonObject("data");

                String canonical = data.has("username") && !data.get("username").isJsonNull()
                        ? data.get("username").getAsString()
                        : username;

                List<String> names = new ArrayList<>();
                List<String> dates = new ArrayList<>();
                if (data.has("usernames") && data.get("usernames").isJsonArray()) {
                    JsonArray arr = data.getAsJsonArray("usernames");
                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject entry = arr.get(i).getAsJsonObject();
                        if (entry.has("username") && !entry.get("username").isJsonNull()) {
                            names.add(entry.get("username").getAsString());
                            
                            if (entry.has("changedToAt") && !entry.get("changedToAt").isJsonNull()) {
                                try {
                                    long ts = entry.get("changedToAt").getAsLong();
                                    
                                    if (ts < 1000000000000L) ts *= 1000L;
                                    java.time.LocalDate date = java.time.Instant.ofEpochMilli(ts)
                                            .atZone(java.time.ZoneId.systemDefault())
                                            .toLocalDate();
                                    dates.add(date.toString());
                                } catch (Exception e) {
                                    dates.add("");
                                }
                            } else {
                                dates.add("");
                            }
                        }
                    }
                }

                
                String uuidStr = null;
                if (data.has("uuid") && !data.get("uuid").isJsonNull()) {
                    uuidStr = data.get("uuid").getAsString();
                } else if (data.has("id") && !data.get("id").isJsonNull()) {
                    uuidStr = data.get("id").getAsString();
                }

                final String finalCanonical = canonical;
                final List<String> finalNames = List.copyOf(names);
                final List<String> finalDates = List.copyOf(dates);
                
                String resolvedUuid = uuidStr;
                if ((resolvedUuid == null || resolvedUuid.isEmpty()) && finalCanonical != null && !finalCanonical.isEmpty()) {
                    UUID fetched = fetchMojangUuid(finalCanonical);
                    if (fetched != null) {
                        resolvedUuid = fetched.toString();
                    }
                }
                final String finalUuid = resolvedUuid;
                final int finalTotal = names.size();
                final boolean finalShowHead = showHead;

                
                GameProfile fullProfile = null;
                if (finalShowHead) {
                    try {
                        
                        YggdrasilAuthenticationService authService = ((MinecraftClientAccessor) MinecraftClient.getInstance()).getAuthenticationService();
                        ApiServices apiServices = ApiServices.create(authService, MinecraftClient.getInstance().runDirectory);

                        
                        GameProfile basicProfile = fetchProfile(finalCanonical, apiServices).join();
                        if (basicProfile != null) {
                            
                            fullProfile = Optional.ofNullable(apiServices.sessionService().fetchProfile(basicProfile.getId(), true))
                                    .map(ProfileResult::profile)
                                    .orElseGet(() -> new GameProfile(UUID.randomUUID(), finalCanonical));
                        }
                    } catch (Exception e) {
                        fullProfile = null;
                    }
                }
                final GameProfile resolvedProfile = fullProfile;

                
                MinecraftClient.getInstance().execute(() -> {
                    PlayerSkinWidget skinWidget = null;
                    if (finalShowHead) {
                        try {
                            GameProfile profile = resolvedProfile != null
                                    ? resolvedProfile
                                    : new GameProfile(UUID.randomUUID(), finalCanonical);
                            Supplier<SkinTextures> supplier = MinecraftClient.getInstance().getSkinProvider().getSkinTexturesSupplier(profile);
                    
                            skinWidget = new PlayerSkinWidget(80, 160,
                                    MinecraftClient.getInstance().getEntityModelLoader(), supplier);
                        } catch (Exception ignored) {
                            skinWidget = null;
                        }
                    }
                    MinecraftClient.getInstance().setScreen(
                            new NameHistoryScreen(finalCanonical, finalUuid, finalNames, finalDates, finalTotal, skinWidget)
                    );
                });

            } catch (Exception e) {
                IgnsHistoryPlus.LOGGER.error("Lookup failed", e);
                
                sendChat(Text.of("§cAn error occurred while contacting the API. Please try again."));
            }
        }, "ignshistoryplus-lookup").start();
    }

    
    private void sendChat(Text message) {
        MinecraftClient.getInstance().execute(() ->
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(message)
        );
    }

    
    private UUID fetchMojangUuid(String username) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            
            URI uri = URI.create("https://api.mojang.com/users/profiles/minecraft/" + username);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("User-Agent", "IGNSHistoryPlus-Mod/1.0")
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonObject obj = GSON.fromJson(resp.body(), JsonObject.class);
                if (obj != null && obj.has("id")) {
                    String raw = obj.get("id").getAsString();
                    
                    if (raw.length() == 32) {
                        String uuidStr = raw.replaceFirst(
                                "([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)",
                                "$1-$2-$3-$4-$5");
                        return UUID.fromString(uuidStr);
                    }
                }
            }
        } catch (Exception e) {
            
        }
        return null;
    }

    
    private CompletableFuture<GameProfile> fetchProfile(String username, ApiServices services) {
        CompletableFuture<GameProfile> future = new CompletableFuture<>();
        try {
            services.profileRepository().findProfilesByNames(new String[]{username}, new ProfileLookupCallback() {
                @Override
                public void onProfileLookupSucceeded(GameProfile profile) {
                    future.complete(profile);
                }

                @Override
                public void onProfileLookupFailed(String profileName, Exception exception) {
                    future.completeExceptionally(exception);
                }
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    
    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestOnlinePlayers(com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        var client = MinecraftClient.getInstance();
        var handler = client.getNetworkHandler();
        java.util.List<String> names = (handler != null)
                ? handler.getPlayerList().stream()
                        .map(net.minecraft.client.network.PlayerListEntry::getProfile)
                        .map(com.mojang.authlib.GameProfile::getName)
                        .filter(name -> name != null)
                        .toList()
                : java.util.List.of();
        return net.minecraft.command.CommandSource.suggestMatching(names, builder);
    }
}
