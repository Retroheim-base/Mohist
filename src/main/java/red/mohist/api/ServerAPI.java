package red.mohist.api;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class ServerAPI {

    public static Map<String, Integer> mods = Maps.newHashMap();
    public static HashSet<String> modlists = Sets.newHashSet();
    public static Map<String, Integer> injectmaterials = Maps.newHashMap();
    public static Map<String, Integer> injectblock = Maps.newHashMap();
    public static Map<String, String> forgecmdper = Maps.newHashMap();

    // Don't count the default number of mods
    public static int getModSize() {
        return mods.get("mods") == null ? 0 : mods.get("mods") - 4;
    }

    public static String getModList() {
        return modlists.toString();
    }

    public static Boolean hasMod(String modid) {
        return getModList().contains(modid);
    }

    public static Boolean hasPlugin(String pluginname) {
        return Bukkit.getPluginManager().getPlugin(pluginname) != null;
    }

    public static void registerBukkitEvents(Listener listener, Plugin plugin){
        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }

    public static MinecraftServer getNMSServer(){
        return MinecraftServer.getServerInst();
    }
}
