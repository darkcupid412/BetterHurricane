package org.geysermc.hurricane.paper;

import io.papermc.paper.event.player.PlayerFailMoveEvent;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.geysermc.hurricane.core.collision.MovementResolver;
import org.geysermc.hurricane.core.collision.ResolvedMovement;
import org.geysermc.hurricane.core.math.Aabb;
import org.geysermc.hurricane.core.math.Movement;

public final class PaperCollisionListener implements Listener {
    private static final double TARGET_INSET = 0.00001D;
    private static final double MAX_PACKET_MOVE = 8.0D;
    private static final long DIAGNOSTIC_INTERVAL_NANOS = 500_000_000L;

    private final BedrockPlayerResolver playerResolver;
    private final Logger logger;
    private final boolean bambooEnabled;
    private final boolean pointedDripstoneEnabled;
    private final boolean diagnostics;
    private final DiagnosticThrottle failDiagnostics =
            new DiagnosticThrottle(DIAGNOSTIC_INTERVAL_NANOS);
    private final DiagnosticThrottle guardDiagnostics =
            new DiagnosticThrottle(DIAGNOSTIC_INTERVAL_NANOS);

    public PaperCollisionListener(
            BedrockPlayerResolver playerResolver,
            Logger logger,
            boolean bambooEnabled,
            boolean pointedDripstoneEnabled,
            boolean diagnostics
    ) {
        this.playerResolver = Objects.requireNonNull(playerResolver, "playerResolver");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.bambooEnabled = bambooEnabled;
        this.pointedDripstoneEnabled = pointedDripstoneEnabled;
        this.diagnostics = diagnostics;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFailMove(PlayerFailMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getFailReason() != PlayerFailMoveEvent.FailReason.CLIPPED_INTO_BLOCK
                || !playerResolver.isBedrockPlayer(player.getUniqueId())
                || !sameWorld(event.getFrom(), event.getTo())) {
            return;
        }

        Aabb target = PaperBoxes.deflate(boxAt(player, event.getTo()), TARGET_INSET);
        PaperCollisionWorld.TargetScan scan = PaperCollisionWorld.scanTarget(
                player,
                target,
                bambooEnabled,
                pointedDripstoneEnabled
        );
        PaperMovementDecision.FailedMoveEvaluation evaluation =
                new PaperMovementDecision.FailedMoveEvaluation(
                        true,
                        true,
                        scan.javaTranslatedOverlap(),
                        scan.bedrockTranslatedOverlap(),
                        scan.otherBlockCollision(),
                        scan.entityCollision()
                );
        boolean allowed = PaperMovementDecision.shouldAllowFailedMove(evaluation);
        logFailDiagnostic(scan, allowed);
        if (allowed) {
            event.setAllowed(true);
            event.setLogWarning(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event instanceof PlayerTeleportEvent
                || !event.hasChangedPosition()
                || !playerResolver.isBedrockPlayer(event.getPlayer().getUniqueId())
                || !sameWorld(event.getFrom(), event.getTo())) {
            return;
        }

        Movement requested = new Movement(
                event.getTo().getX() - event.getFrom().getX(),
                event.getTo().getY() - event.getFrom().getY(),
                event.getTo().getZ() - event.getFrom().getZ()
        );
        if (isImplausiblyLarge(requested)) {
            event.setCancelled(true);
            return;
        }

        Player player = event.getPlayer();
        Aabb start = boxAt(player, event.getFrom());
        Aabb target = boxAt(player, event.getTo());
        List<Aabb> collisionBoxes = PaperCollisionWorld.bedrockCollisionBoxes(
                player.getWorld(),
                start.union(target),
                bambooEnabled,
                pointedDripstoneEnabled
        );
        ResolvedMovement resolved = MovementResolver.resolve(start, target, collisionBoxes);
        if (!resolved.collided()) {
            return;
        }

        Movement movement = resolved.movement();
        Location corrected = event.getTo().clone();
        corrected.setX(event.getFrom().getX() + movement.x());
        corrected.setY(event.getFrom().getY() + movement.y());
        corrected.setZ(event.getFrom().getZ() + movement.z());
        logGuardDiagnostic(event.getFrom(), event.getTo(), corrected, collisionBoxes.size());
        event.setTo(corrected);
    }

    private static Aabb boxAt(Player player, Location location) {
        Location current = player.getLocation();
        return PaperBoxes.atPosition(
                player.getBoundingBox(),
                current.getX(),
                current.getY(),
                current.getZ(),
                location.getX(),
                location.getY(),
                location.getZ()
        );
    }

    private static boolean sameWorld(Location from, Location to) {
        World fromWorld = from.getWorld();
        return fromWorld != null && fromWorld.equals(to.getWorld());
    }

    private static boolean isImplausiblyLarge(Movement movement) {
        return Math.abs(movement.x()) > MAX_PACKET_MOVE
                || Math.abs(movement.y()) > MAX_PACKET_MOVE
                || Math.abs(movement.z()) > MAX_PACKET_MOVE;
    }

    private void logFailDiagnostic(PaperCollisionWorld.TargetScan scan, boolean allowed) {
        if (!diagnostics) {
            return;
        }
        if (!failDiagnostics.tryAcquire(System.nanoTime())) {
            return;
        }
        logger.info("Collision fail-move scan: java=" + scan.javaTranslatedOverlap()
                + ", bedrock=" + scan.bedrockTranslatedOverlap()
                + ", other=" + scan.otherBlockCollision()
                + ", entity=" + scan.entityCollision()
                + ", allowed=" + allowed);
    }

    private void logGuardDiagnostic(
            Location from,
            Location requested,
            Location corrected,
            int collisionBoxCount
    ) {
        if (!diagnostics) {
            return;
        }
        if (!guardDiagnostics.tryAcquire(System.nanoTime())) {
            return;
        }
        logger.info("Collision resolver clipped movement from "
                + format(from) + " to " + format(requested)
                + ", preserving " + format(corrected)
                + " across " + collisionBoxCount + " translated box(es).");
    }

    private static String format(Location location) {
        return String.format(
                "(%.5f, %.5f, %.5f)",
                location.getX(),
                location.getY(),
                location.getZ()
        );
    }
}
