package me.reimnop.d4f.mixin;

import me.reimnop.d4f.Config;
import me.reimnop.d4f.Discord4Fabric;
import me.reimnop.d4f.events.PlayerDeathCallback;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SentMessage;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {

    @Inject(method = "sendChatMessage",
            at = @At("HEAD"),
            cancellable = true)
    private void Discord4Fabric$blockChat(SentMessage message, boolean filterMaskEnabled, MessageType.Parameters params, CallbackInfo ci) {
        Config config = Discord4Fabric.CONFIG;
        if (!config.sendMessagesToMinecraft) {
            ci.cancel();
        }
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void Discord4Fabric$onDeath(DamageSource damageSource, CallbackInfo ci) {
        ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity) (Object) this;
        Text deathMessage = serverPlayerEntity.getDamageTracker().getDeathMessage();
        PlayerDeathCallback.EVENT.invoker().onPlayerDeath(serverPlayerEntity, damageSource, deathMessage);
    }
}
