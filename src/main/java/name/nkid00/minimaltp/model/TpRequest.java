package name.nkid00.minimaltp.model;

import name.nkid00.minimaltp.MinimalTp;
import name.nkid00.minimaltp.helper.TpHelper;

import net.minecraft.server.network.ServerPlayerEntity;

public class TpRequest {
    public final ServerPlayerEntity target;
    public final ServerPlayerEntity destination;
    public final long ExpirationTime;

    public TpRequest(ServerPlayerEntity target, ServerPlayerEntity destination) {
        this.target = target;
        this.destination = destination;
        this.ExpirationTime = System.currentTimeMillis() + MinimalTp.options.requestExpirationInterval * 1000;
    }

    public void execute() {
        TpHelper.teleportTimed(target, destination);
    }

    public boolean isValid() {
        return System.currentTimeMillis() < this.ExpirationTime;
    }
}
