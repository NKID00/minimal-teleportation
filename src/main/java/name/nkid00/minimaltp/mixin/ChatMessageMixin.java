package name.nkid00.minimaltp.mixin;

import name.nkid00.minimaltp.MinimalTp;
import name.nkid00.minimaltp.Waypoint;

import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.math.BlockPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static java.lang.Integer.parseInt;

@Mixin(ServerPlayNetworkHandler.class)
public class ChatMessageMixin {
    @Shadow public ServerPlayerEntity player;

    @Inject(method = "onChatMessage(Lnet/minecraft/network/packet/c2s/play/ChatMessageC2SPacket;)V",
            at = @At(value = "INVOKE",
                    target = "net/minecraft/server/network/ServerPlayNetworkHandler.filterText(Ljava/lang/String;Ljava/util/function/Consumer;)V"))
    public void onChatMessage(ChatMessageC2SPacket packet, CallbackInfo ci) {
        recordWaypoint(packet);
    }

    private void recordWaypoint(ChatMessageC2SPacket packet) {
        String message = packet.getChatMessage();
        if (message == null
                /* The shortest legal Xaero waypoint sharing text is
                *  "xaero-waypoint:a:a:0:0:0:0:true:0:Internal-overworld-waypoints",
                *  whose length is 62. */
                || message.length() < 62
                || !message.startsWith("xaero-waypoint:")) {
            return;
        }

        /* Xaero Waypoint Sharing Format
        * [xaero-waypoint:
        *  name(String):
        *  initial(String, one character):
        *  x(int):
        *  y(int):
        *  z(int):
        *  color(int, 0-15):
        *  disabled(boolean):
        *  type(int):
        *  dimension(string, Internal-overworld/the_neither/the_end-waypoints)] */
        String[] params = message.split(":");
        if (params.length == 10) {
            BlockPos position;
            Identifier dimension;

            try {
                position = new BlockPos(parseInt(params[3]), parseInt(params[4]), parseInt(params[5]));
                dimension = new Identifier("minecraft", params[9].split("-")[1]);
                MinimalTp.lastWaypoint = new Waypoint(position, dimension, player.getDisplayName().copy());
                MinimalTp.lastName = params[1];
            } catch (NumberFormatException | InvalidIdentifierException ignored) {
            }
        }
    }
}