package name.nkid00.minimaltp.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import name.nkid00.minimaltp.MinimalTp;
import name.nkid00.minimaltp.model.Waypoint;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class WaypointCommand {
    public static AtomicInteger color;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("/waypoint")
                .executes(WaypointCommand::executeList)
                .then(literal("add")
                        .executes(WaypointCommand::executeAddShared)
                        .then(argument("name", StringArgumentType.string())
                                .executes(WaypointCommand::executeAddCurrent)
                                .then(argument("position", BlockPosArgumentType.blockPos())
                                        .executes(WaypointCommand::executeAddGiven))))
                .then(literal("info")
                        .then(argument("name", StringArgumentType.string())
                                .executes(WaypointCommand::executeInfo)))
                .then(literal("list")
                        .executes(WaypointCommand::executeList))
                .then(literal("receive")
                        .then(argument("name", StringArgumentType.string())
                                .executes(WaypointCommand::executeReceive)))
                .then(literal("rename")
                        .then(argument("name", StringArgumentType.string())
                                .then(argument("new name", StringArgumentType.word())
                                        .executes(WaypointCommand::executeRename))))
                .then(literal("remove")
                        .then(argument("name", StringArgumentType.string())
                                .executes(WaypointCommand::executeRemove))));
    }

    public static int executeAddShared(CommandContext<ServerCommandSource> c) {
        Pair<String, Waypoint> latest = MinimalTp.latestWaypoint;
        String name = latest.getLeft();
        Waypoint w = latest.getRight();
        if (w == null) {
            c.getSource().sendError(Text.literal("无共享坐标"));
            return 0;
        }

        add(name, w);
        var addMsg = Text.literal("已共享坐标 " + name
                        + "(" + w.dimension().toString() + ", " + w.position().toShortString() + ")")
                .setStyle(MinimalTp.MSG_STYLE);
        c.getSource().sendFeedback(addMsg, true);
        return 1;
    }

    public static int executeAddCurrent(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        var name = StringArgumentType.getString(c, "name");

        var source = c.getSource();
        var dimension = source.getWorld().getDimension().effects();

        var player = source.getPlayerOrThrow();
        var position = player.getBlockPos();
        var recorder = player.getDisplayName().copy();

        add(name, new Waypoint(position, dimension, recorder));

        var addMsg = Text.literal("已共享坐标 " + name
                        + "(" + dimension.toString() + ", " + position.toShortString() + ")")
                .setStyle(MinimalTp.MSG_STYLE);
        source.sendFeedback(addMsg, true);
        return 1;
    }

    public static int executeAddGiven(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        var name = StringArgumentType.getString(c, "name");
        var position = BlockPosArgumentType.getBlockPos(c, "position");

        var source = c.getSource();
        var dimension = source.getWorld().getDimension().effects();

        var recorder = source.getPlayerOrThrow().getDisplayName().copy();

        add(name, new Waypoint(position, dimension, recorder));

        var addMsg = Text.literal("已共享坐标 " + name
                        + "(" + dimension.toString() + ", " + position.toShortString() + ")")
                .setStyle(MinimalTp.MSG_STYLE);
        source.sendFeedback(addMsg, true);
        return 1;
    }

    private static void add(String name, Waypoint w) {
        MinimalTp.waypoints.put(name, w);
    }

    public static int executeInfo(CommandContext<ServerCommandSource> c) {
        var name = StringArgumentType.getString(c, "name");

        Waypoint w;
        if (MinimalTp.waypoints.isEmpty() || (w = MinimalTp.waypoints.get(name)) == null) {
            c.getSource().sendError(Text.literal("未找到共享坐标 " + name));
            return 0;
        }
        var infoMsg = Text.literal("坐标 " + name
                        + ": " + w.dimension().toString()
                        + ", " + w.position().toShortString() + ", "
                        + "记录者:")
                .append(w.recorder())
                .setStyle(MinimalTp.MSG_STYLE);
        c.getSource().sendFeedback(infoMsg, false);
        return 1;
    }

    public static int executeList(CommandContext<ServerCommandSource> c) {
        var source = c.getSource();

        if (MinimalTp.waypoints.isEmpty()) {
            source.sendError(Text.literal("无共享坐标"));
        } else {
            var listMsg = Text.literal("共有" + MinimalTp.waypoints.size() + "个共享坐标: ")
                    .setStyle(MinimalTp.MSG_STYLE);
            Set<String> list = MinimalTp.waypoints.keySet();

            listMsg.append(String.join(", ", list));
            source.sendFeedback(listMsg, false);
        }
        return 1;
    }

    public static int executeReceive(CommandContext<ServerCommandSource> c) {
        var name = StringArgumentType.getString(c, "name");

        Waypoint w;
        if (MinimalTp.waypoints.isEmpty() || (w = MinimalTp.waypoints.get(name)) == null) {
            c.getSource().sendError(Text.literal("未找到共享坐标 " + name));
            return 0;
        }

        String dimension = w.dimension().toString().split(":")[1];
        String[] contents = {"xaero-waypoint",
                name,
                name.substring(0, 1),
                String.valueOf(w.position().getX()),
                String.valueOf(w.position().getY()),
                String.valueOf(w.position().getZ()),
                String.valueOf(color),
                "false:0",
                "Internal-" + dimension + "-waypoints"
        };

        if (color.incrementAndGet() > 15) color.set(0);

        var rcvMsg = Text.literal(String.join(":", contents));
        c.getSource().sendFeedback(rcvMsg, false);
        return 1;
    }

    public static int executeRemove(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        var name = StringArgumentType.getString(c, "name");

        var source = c.getSource();
        if (source.hasPermissionLevel(2)) {
            if (MinimalTp.waypoints.isEmpty() || MinimalTp.waypoints.remove(name) == null) {
                source.sendError(Text.literal("未找到共享坐标 " + name));
                return 0;
            }

            var removeMsg = Text.literal("已移除共享坐标 " + name)
                    .setStyle(MinimalTp.MSG_STYLE);
            source.sendFeedback(removeMsg, true);
            return 1;
        }

        var executor = source.getPlayerOrThrow().getDisplayName().copy();

        Waypoint w;
        if (MinimalTp.waypoints.isEmpty() || (w = MinimalTp.waypoints.get(name)) == null) {
            source.sendError(Text.literal("未找到共享坐标 " + name));
            return 0;
        } else if (!w.recorder().equals(executor)) {
            source.sendError(Text.literal("您无权限移除坐标 " + name));
            return 0;
        }

        MinimalTp.waypoints.remove(name);
        var removeMsg = Text.literal("已移除共享坐标 " + name)
                .setStyle(MinimalTp.MSG_STYLE);
        source.sendFeedback(removeMsg, true);
        return 1;
    }

    public static int executeRename(CommandContext<ServerCommandSource> c) {
        var oldName = StringArgumentType.getString(c, "name");
        var newName = StringArgumentType.getString(c, "new name");

        Waypoint w;
        if (MinimalTp.waypoints.isEmpty() || (w = MinimalTp.waypoints.remove(oldName)) == null) {
            c.getSource().sendError(Text.literal("未找到坐标 " + oldName));
            return 0;
        }
        MinimalTp.waypoints.put(newName, w);

        var renameMsg = Text.literal("已将坐标 " + oldName + "重命名为 " + newName)
                .setStyle(MinimalTp.MSG_STYLE);
        c.getSource().sendFeedback(renameMsg, true);
        return 1;
    }

}
