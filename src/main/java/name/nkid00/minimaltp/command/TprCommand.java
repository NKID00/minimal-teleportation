package name.nkid00.minimaltp.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import name.nkid00.minimaltp.MinimalTp;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.literal;

public class TprCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
            CommandRegistryAccess registryAccess,
            CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(
                literal("/tpr").executes(TprCommand::execute));
    }

    public static int execute(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        var source = c.getSource();
        var destination = source.getPlayerOrThrow();
        var uuid = destination.getUuid();
        if (MinimalTp.TpRequests.containsKey(uuid)) {
            var request = MinimalTp.TpRequests.remove(uuid);
            var target = request.target;
            if (request.isValid()) {
                var destMsg = Text.literal("已拒绝")
                        .append(target.getDisplayName().copy())
                        .append("的传送请求")
                        .setStyle(MinimalTp.MSG_STYLE);
                source.sendFeedback(destMsg, false);
                var targetMsg = Text.literal("向")
                        .append(destination.getDisplayName().copy())
                        .append("的传送请求被拒绝")
                        .setStyle(MinimalTp.REFUSE_STYLE);
                // refer to ServerCommandSource method sendError(Text message)
                request.target.sendMessage(targetMsg.formatted(Formatting.RED));
                return 1;
            }
        }
        source.sendError(Text.literal("传送请求已过期或不存在"));
        return 0;
    }
}
