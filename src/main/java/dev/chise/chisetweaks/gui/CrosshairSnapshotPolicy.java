package dev.chise.chisetweaks.gui;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Pure, privacy-bounded Block Info snapshot formatting. */
final class CrosshairSnapshotPolicy {
    private static final int MAX_STATE_PROPERTIES = 32;
    private static final int MAX_TOKEN_LENGTH = 64;

    private CrosshairSnapshotPolicy() {}

    static CrosshairInspector.Snapshot blockSnapshot(
            BlockState state,
            BlockState predictedPlacement,
            BlockState actualPlacement,
            int placementResult,
            net.minecraft.core.Direction clickedFace,
            boolean upperClick) {
        if (state == null) return CrosshairInspector.Snapshot.noTarget();
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String targetId = id == null ? "" : safeIdentifier(id.toString());
        if (targetId.isEmpty()) return CrosshairInspector.Snapshot.noTarget();

        return new CrosshairInspector.Snapshot(
                HitResult.Type.BLOCK,
                targetId,
                stateProperties(state),
                predictedPlacement,
                actualPlacement,
                placementResult,
                clickedFace,
                upperClick);
    }

    static CrosshairInspector.Snapshot entitySnapshot(Entity entity) {
        Identifier id = entity == null ? null : BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String targetId = id == null ? "" : safeIdentifier(id.toString());
        if (targetId.isEmpty()) return CrosshairInspector.Snapshot.noTarget();
        return new CrosshairInspector.Snapshot(
                HitResult.Type.ENTITY,
                targetId,
                List.of(),
                null,
                null,
                PlacementInspector.NONE,
                null,
                false);
    }

    static List<String> stateProperties(BlockState state) {
        LinkedHashMap<String, String> properties = new LinkedHashMap<>();
        for (Property<?> property : state.getProperties()) {
            addProperty(state, property, properties);
        }
        return formatStateProperties(properties);
    }

    static String semanticPropertyGroup(String property) {
        return switch (property == null ? "" : property) {
            case "facing", "axis" -> "orientation";
            case "half", "type", "shape", "face" -> "shape";
            case "north", "south", "east", "west", "up", "down", "in_wall" -> "connection";
            case "open", "powered", "lit", "honey_level" -> "interaction";
            case "waterlogged" -> "fluid";
            default -> "other";
        };
    }

    static String humanize(String token) {
        if (token == null || token.isEmpty()) return "";
        String value = token.replace('_', ' ');
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    static List<String> formatStateProperties(Map<String, String> rawProperties) {
        if (rawProperties == null || rawProperties.isEmpty()) return List.of();
        ArrayList<String> result = new ArrayList<>();
        rawProperties.entrySet().stream()
                .sorted(Comparator.comparing(entry -> safeToken(entry.getKey())))
                .forEach(entry -> {
                    if (result.size() >= MAX_STATE_PROPERTIES) return;
                    String name = safeToken(entry.getKey());
                    String value = safeToken(entry.getValue());
                    if (!name.isEmpty() && !value.isEmpty()) result.add(name + "=" + value);
                });
        return List.copyOf(result);
    }

    private static <T extends Comparable<T>> void addProperty(
            BlockState state,
            Property<T> property,
            Map<String, String> properties) {
        properties.put(property.getName(), property.getName(state.getValue(property)));
    }

    private static String safeIdentifier(String raw) {
        if (raw == null) return "";
        String value = raw.trim();
        Identifier parsed = value.length() <= 256 ? Identifier.tryParse(value) : null;
        return parsed == null ? "" : parsed.toString();
    }

    private static String safeToken(String raw) {
        if (raw == null) return "";
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return value.length() <= MAX_TOKEN_LENGTH && Identifier.isValidPath(value) ? value : "";
    }
}
