package name.nkid00.minimaltp;

import name.nkid00.minimaltp.model.TpRequest;
import name.nkid00.minimaltp.model.Waypoint;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import name.nkid00.minimaltp.command.*;

public class MinimalTp implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("MinimalTp");
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
    public static final Timer TELEPORT_TIMER = new Timer(true);

    public static final Style MSG_STYLE = Style.EMPTY.withColor(Formatting.YELLOW);
    public static final Style ACCEPT_STYLE = Style.EMPTY.withColor(Formatting.GREEN);
    public static final Style REFUSE_STYLE = Style.EMPTY.withColor(Formatting.RED);
    public static final Style CLICK_TPA_CMD_STYLE = Style.EMPTY
            .withColor(Formatting.DARK_GREEN)
            .withUnderline(true)
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("点击执行")))
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "//tpa"));
    public static final Style CLICK_TPR_CMD_STYLE = Style.EMPTY
            .withColor(Formatting.DARK_RED)
            .withUnderline(true)
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("点击执行")))
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "//tpr"));
    public static final boolean POTENTIAL_COMMAND_CONFLICT = FabricLoader.getInstance().isModLoaded("worldedit");

    public static Options options;
    public static Data data;
    public static ConcurrentHashMap<UUID, TpRequest> TpRequests = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Waypoint> waypoints = new ConcurrentHashMap<>();
    public static volatile Pair<String, Waypoint> latestWaypoint;

    @Override
    public void onInitialize() {
        var loader = FabricLoader.getInstance();

        // options (static and shared globally)
        Options.file = loader.getConfigDir().resolve("minimaltp.json").toFile();
        Options.load();

        // data (dynamic and stored respectively for each world)
        Data.file = loader.getConfigDir().resolve("minimaltp/data.json").toFile();
        Data.load();
        ServerLifecycleEvents.SERVER_STOPPED.register((server) -> Data.save());

        // commands
        if (POTENTIAL_COMMAND_CONFLICT) {
            MinimalTp.LOGGER.warn("Commands are disabled due to potential conflict with WorldEdit");
        } else {
            CommandRegistrationCallback.EVENT.register(HelpCommand::register);
            CommandRegistrationCallback.EVENT.register(ReloadOptionsCommand::register);
            CommandRegistrationCallback.EVENT.register(TpCommand::register);
            CommandRegistrationCallback.EVENT.register(TpaCommand::register);
            CommandRegistrationCallback.EVENT.register(TprCommand::register);
            CommandRegistrationCallback.EVENT.register(WaypointCommand::register);
        }

        // banners
        if (POTENTIAL_COMMAND_CONFLICT) {
            ServerPlayConnectionEvents.JOIN.register(Banner::registerPotentialCommandConflict);
        } else {
            ServerPlayConnectionEvents.JOIN.register(Banner::register);
        }
    }
}
