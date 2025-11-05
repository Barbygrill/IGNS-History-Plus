package com.barby.ignshistoryplus.mixin;

import net.minecraft.client.MinecraftClient;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;


@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    
    @Accessor
    YggdrasilAuthenticationService getAuthenticationService();
}
