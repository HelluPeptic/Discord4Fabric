package me.reimnop.d4f.utils;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import org.samo_lego.fabrictailor.casts.TailoredPlayer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import eu.pb4.placeholders.api.PlaceholderHandler;
import eu.pb4.placeholders.api.Placeholders;
import me.reimnop.d4f.Discord4Fabric;
import me.reimnop.d4f.NameToUUIDConverter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class Utils {

    private Utils() {
    }

    private static final NameToUUIDConverter nameToUUIDConverter;

    static {
        nameToUUIDConverter = new NameToUUIDConverter();

        File file = new File(Utils.getNameCachePath());

        if (file.exists()) {
            try {
                nameToUUIDConverter.loadCache(file);
            } catch (IOException e) {
                Utils.logException(e);
            }
        }
    }

    public static void logException(Exception e) {
        e.printStackTrace();
    }

    public static String getAvatarUrl(ServerPlayerEntity player) {
        if (Utils.isModLoaded("fabrictailor") && player instanceof TailoredPlayer tailoredPlayer && tailoredPlayer.getSkinValue() != null) {
            String profileJsonStr = new String(Base64.getDecoder().decode(tailoredPlayer.getSkinValue()));
            JsonObject profileJson = new Gson().fromJson(profileJsonStr, JsonObject.class);
            String skinUrl = profileJson.get("textures").getAsJsonObject().get("SKIN").getAsJsonObject().get("url").getAsString();
            String[] splitSkinUrl = skinUrl.split("/");
            return Utils.getAvatarUrlFromTextureHash(splitSkinUrl[splitSkinUrl.length - 1]);
        }

        UUID uuid = player.getUuid();
        if (Discord4Fabric.CONFIG.forceOnlineUuid && !((MinecraftDedicatedServer) FabricLoader.getInstance().getGameInstance()).isOnlineMode()) {
            uuid = nameToUUIDConverter.getUuid(player);
        }
        return Utils.getAvatarUrl(uuid);
    }

    public static String getAvatarUrl(UUID uuid) {
        return String.format(Discord4Fabric.CONFIG.avatarUrl, uuid.toString());
    }

    public static String getAvatarUrlFromTextureHash(String hash) {
        return String.format(Discord4Fabric.CONFIG.avatarUrlTextureHash, hash);
    }

    public static String getConfigPath() {
        return FabricLoader.getInstance().getConfigDir() + "/discord4fabric.json";
    }

    public static String getUserdataPath() {
        return FabricLoader.getInstance().getConfigDir() + "/d4f_userdata.json";
    }

    public static String getCustomEventsPath() {
        return FabricLoader.getInstance().getConfigDir() + "/d4f_custom_events.json";
    }

    public static PlaceholderHandler getPlaceholderHandler(String placeholder, Map<Identifier, PlaceholderHandler> handlers) {
        Identifier id = Identifier.tryParse(placeholder);
        if (handlers.containsKey(id)) {
            return handlers.get(id);
        }
        return (ctx, arg) -> Placeholders.parsePlaceholder(id, arg, ctx);
    }

    public static String getNameCachePath() {
        return FabricLoader.getInstance().getConfigDir() + "/d4f_name_cache.json";
    }

    public static String getTpsAsString() {
        if (FabricLoader.getInstance().isModLoaded("spark")) {
            try {
                // Use reflection to avoid compile-time dependencies
                Class<?> sparkProviderClass = Class.forName("me.lucko.spark.api.SparkProvider");
                Method getMethod = sparkProviderClass.getMethod("get");
                Object spark = getMethod.invoke(null);

                Class<?> sparkClass = Class.forName("me.lucko.spark.api.Spark");
                Method tpsMethod = sparkClass.getMethod("tps");
                Object tpsStatistic = tpsMethod.invoke(spark);

                if (tpsStatistic == null) {
                    return String.valueOf(roundTps(getShittyTps((MinecraftServer) FabricLoader.getInstance().getGameInstance())));
                }

                // Get the TicksPerSecond enum values
                Class<?> ticksPerSecondClass = Class.forName("me.lucko.spark.api.statistic.StatisticWindow$TicksPerSecond");
                Object seconds10 = ticksPerSecondClass.getField("SECONDS_10").get(null);
                Object minutes5 = ticksPerSecondClass.getField("MINUTES_5").get(null);
                Object minutes15 = ticksPerSecondClass.getField("MINUTES_15").get(null);

                // Call poll method
                Method pollMethod = tpsStatistic.getClass().getMethod("poll", Object.class);
                double tpsLast10Secs = (Double) pollMethod.invoke(tpsStatistic, seconds10);
                double tpsLast5Mins = (Double) pollMethod.invoke(tpsStatistic, minutes5);
                double tpsLast15Mins = (Double) pollMethod.invoke(tpsStatistic, minutes15);

                return roundTps(tpsLast10Secs) + " / " + roundTps(tpsLast5Mins) + " / " + roundTps(tpsLast15Mins) + " (10 secs / 5 mins / 15 mins)";
            } catch (Exception e) {
                Discord4Fabric.LOGGER.warn("Failed to get TPS from Spark, falling back to basic TPS calculation", e);
                return String.valueOf(roundTps(getShittyTps((MinecraftServer) FabricLoader.getInstance().getGameInstance())));
            }
        }
        return String.valueOf(roundTps(getShittyTps((MinecraftServer) FabricLoader.getInstance().getGameInstance())));
    }

    private static double roundTps(double tps) {
        return Math.floor(tps * 10.0) / 10.0;
    }

    // very appropriate function name
    private static double getShittyTps(MinecraftServer server) {
        return Math.min(1000.0 / server.getAverageTickTime(), 20.0);
    }

    public static String getNicknameFromUser(User user) {
        Member member = Discord4Fabric.DISCORD.getMember(user);
        if (member == null) {
            return user.getName();
        }
        return member.getEffectiveName();
    }

    public static String getColoredNicknameFromUser(User user) {
        Member member = Discord4Fabric.DISCORD.getMember(user);
        if (member == null) {
            return user.getName();
        }
        Color color = member.getColor();
        if (color == null) {
            return member.getEffectiveName();
        }
        return "<c:" + "#" + Integer.toHexString(color.getRGB()).substring(2) + ">" + member.getEffectiveName() + "</c>";
    }

    public static boolean isModLoaded(String modid) {
        return FabricLoader.getInstance().isModLoaded(modid);
    }
}
