package com.barby.ignshistoryplus.util;

import com.barby.ignshistoryplus.IgnsHistoryPlus;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;


public final class PlayerSkinUtil {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private PlayerSkinUtil() {}

    
    public static byte[] downloadSkinBytes(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "IGNSHistoryPlus-Mod/1.0")
                    .GET()
                    .build();
            HttpResponse<byte[]> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() == 200 && resp.body() != null && resp.body().length > 0) {
                return resp.body();
            }
        } catch (Exception e) {
            IgnsHistoryPlus.LOGGER.warn("Failed to download skin from {}", url, e);
        }
        return null;
    }

    
    public static Identifier registerSkinTexture(String username, byte[] pngBytes) {
        if (pngBytes == null || pngBytes.length == 0) return null;
        try {
            NativeImage img = NativeImage.read(new ByteArrayInputStream(pngBytes));
            NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
            Identifier id = Identifier.of(IgnsHistoryPlus.MOD_ID, "skins/" + username.toLowerCase());
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
            return id;
        } catch (Exception e) {
            IgnsHistoryPlus.LOGGER.warn("Failed to register skin texture for {}", username, e);
            return null;
        }
    }

    
    public static Identifier getOrDownloadSkin(String username) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            PlayerSkinProvider provider = client.getSkinProvider();
            GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes()), username);
            SkinTextures textures = provider.getSkinTextures(profile);
            if (textures == null) return null;
            return textures.texture();
        } catch (Exception e) {
            IgnsHistoryPlus.LOGGER.error("Failed to load fallback skin for {}", username, e);
            return null;
        }
    }
}
