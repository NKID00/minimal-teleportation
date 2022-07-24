package name.nkid00.minimaltp.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import name.nkid00.minimaltp.MinimalTp;
import name.nkid00.minimaltp.model.TpRequest;
import name.nkid00.minimaltp.helper.TpHelper;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import net.minecraft.text.Text;

public class TpCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
            CommandRegistryAccess registryAccess,
            CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(
                literal("/tp").then(
                        argument("destination", EntityArgumentType.player()).executes(TpCommand::execute)));
    }

    public static int execute(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        var source = c.getSource();
        var target = source.getPlayerOrThrow();
        var destination = EntityArgumentType.getPlayer(c, "destination");
        if (target.equals(destination)) {
            source.sendError(Text.literal("请不要随意玩耍硬核自研大数据人工智能黑科技模组!").setStyle(MinimalTp.REFUSE_STYLE));
            return 0;
        }

        var scoreboard = source.getServer().getScoreboard();
        var target_team = scoreboard.getPlayerTeam(target.getEntityName());
        var destination_team = scoreboard.getPlayerTeam(destination.getEntityName());

        var teleportImmediately = false;
        if (target_team != null && destination_team != null && target_team.isEqual(destination_team)) {
            teleportImmediately = MinimalTp.options.immediateTeleportationInTeam;
        } else {
            teleportImmediately = MinimalTp.options.immediateTeleportationBetweenTeam;
        }

        if (teleportImmediately) {
            return TpHelper.teleportImmediately(target, destination);
        } else {
            MinimalTp.TpRequests.put(destination.getUuid(), new TpRequest(source.getPlayerOrThrow(), destination));

            var feedback = Text.empty()
                    .append(target.getDisplayName().copy())
                    .append(String.format("请求传送至你的位置,可以在%d秒内选择 ",
                            MinimalTp.options.requestExpirationInterval))
                    .append(Text.literal("接受(//tpa)").setStyle(MinimalTp.CLICK_TPA_CMD_STYLE))
                    .append(" 或 ")
                    .append(Text.literal("拒绝(//tpr)").setStyle(MinimalTp.CLICK_TPR_CMD_STYLE))
                    .setStyle(MinimalTp.MSG_STYLE);
            destination.getCommandSource().sendFeedback(feedback, false);
            source.sendFeedback(Text.literal("已发送传送请求").setStyle(MinimalTp.MSG_STYLE), false);

            return 1;
        }
    }
}
