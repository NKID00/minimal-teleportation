package name.nkid00.minimaltp.model;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record Waypoint(BlockPos position, Identifier dimension, Text recorder) {
}
