package name.nkid00.minimaltp.helper;

import java.util.Collections;
import java.util.TimerTask;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import name.nkid00.minimaltp.MinimalTp;
import name.nkid00.minimaltp.mixin.TeleportCommandMixin;

public class TpHelper {
    public static int teleportImmediately(ServerPlayerEntity target,
            ServerPlayerEntity destination) throws CommandSyntaxException {
        var feedback = Text.literal("已将")
                .append(target.getDisplayName().copy())
                .append("传送至你")
                .setStyle(MinimalTp.MSG_STYLE);
        destination.getCommandSource().sendFeedback(feedback, false);
        return TeleportCommandMixin.execute(target.getCommandSource(), Collections.singleton(target), destination);
    }

    public static void teleportTimed(ServerPlayerEntity target,
            ServerPlayerEntity destination) {
        var targetMsg = Text.literal("向")
                .append(destination.getDisplayName().copy())
                .append(Text.literal(String.format("的传送请求被接受, 将在%d秒后传送",
                        MinimalTp.options.teleportInterval)))
                .setStyle(MinimalTp.ACCEPT_STYLE);
        target.getCommandSource().sendFeedback(targetMsg, false);
        var destMsg = Text.literal("已接受")
                .append(target.getDisplayName().copy())
                .append(Text.literal(String.format("的传送请求, 将在%d秒后传送",
                        MinimalTp.options.teleportInterval)))
                .setStyle(MinimalTp.MSG_STYLE);
        destination.getCommandSource().sendFeedback(destMsg, false);
        MinimalTp.TELEPORT_TIMER.schedule(new TpTask(target, destination), MinimalTp.options.teleportInterval * 1000);
    }

    private static class TpTask extends TimerTask {
        private final ServerPlayerEntity target;
        private final ServerPlayerEntity destination;

        public TpTask(ServerPlayerEntity target,
                ServerPlayerEntity destination) {
            this.target = target;
            this.destination = destination;
        }

        public void run() {
            if (target.isDisconnected() || destination.isDisconnected()) {
                return;
            }
            try {
                TpHelper.teleportImmediately(target, destination);
            } catch (CommandSyntaxException ignored) {
            }
        }
    }
}
