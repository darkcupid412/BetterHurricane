package org.geysermc.hurricane.paper;

import java.util.Objects;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.PointedDripstone;
import org.geysermc.hurricane.core.collision.BedrockPointedDripstoneCollision;
import org.geysermc.hurricane.core.math.Aabb;
import org.geysermc.hurricane.core.math.BlockPosition;

@SuppressWarnings("removal")
public final class PaperPointedDripstoneCollision {
    private PaperPointedDripstoneCollision() {
    }

    public static Aabb boxAt(
            BlockPosition position,
            PointedDripstone.Thickness thickness,
            BlockFace verticalDirection
    ) {
        return BedrockPointedDripstoneCollision.boxAt(
                position,
                toCore(thickness),
                Objects.requireNonNull(verticalDirection, "verticalDirection") == BlockFace.DOWN
        );
    }

    static BedrockPointedDripstoneCollision.Thickness toCore(
            PointedDripstone.Thickness thickness
    ) {
        return switch (Objects.requireNonNull(thickness, "thickness")) {
            case TIP_MERGE -> BedrockPointedDripstoneCollision.Thickness.MERGE;
            case TIP -> BedrockPointedDripstoneCollision.Thickness.TIP;
            case FRUSTUM -> BedrockPointedDripstoneCollision.Thickness.FRUSTUM;
            case MIDDLE -> BedrockPointedDripstoneCollision.Thickness.MIDDLE;
            case BASE -> BedrockPointedDripstoneCollision.Thickness.BASE;
        };
    }
}
