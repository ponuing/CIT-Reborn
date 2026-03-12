package com.ponuing.pcustomtextures.client.cit;

import com.ponuing.pcustomtextures.client.NbtRenderOverrideResolver;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

public final class CitMatchers {
    private CitMatchers() {
    }

    public enum SourceNbt {
        FULL_STACK,
        COMPONENTS_ONLY
    }

    public record PathMatcherRule(SourceNbt source, String path, ValueMatcher matcher) {
        public boolean matches(NbtCompound root) {
            if (root == null) {
                return matcher.matchesNullable(null);
            }
            NbtElement element = resolvePath(root, path);
            return matcher.matchesNullable(element);
        }

        public static NbtElement resolvePathWithFallback(NbtCompound root, String path) {
            return resolvePath(root, path);
        }

        public static NbtElement resolvePathOnElement(NbtElement element, String path) {
            if (element instanceof NbtCompound compound) {
                return resolvePathInternal(compound, path);
            }
            return null;
        }

        private static NbtElement resolvePath(NbtCompound root, String path) {
            NbtElement direct = resolvePathInternal(root, path);
            if (direct != null) {
                return direct;
            }

            // OptiFine-style nbt.* often refers to custom_data; fall back to it if present.
            if (root.contains("components", NbtElement.COMPOUND_TYPE)) {
                NbtCompound components = root.getCompound("components");
                if (components.contains("minecraft:custom_data", NbtElement.COMPOUND_TYPE)) {
                    return resolvePathInternal(components.getCompound("minecraft:custom_data"), path);
                }
            }

            if (root.contains("tag", NbtElement.COMPOUND_TYPE)) {
                return resolvePathInternal(root.getCompound("tag"), path);
            }

            return null;
        }

        private static NbtElement resolvePathInternal(NbtCompound root, String path) {
            NbtElement current = root;
            for (String part : path.split("\\.")) {
                if (current == null) {
                    return null;
                }
                if (current instanceof NbtCompound compound) {
                    current = compound.contains(part) ? compound.get(part) : null;
                    continue;
                }
                if (current instanceof NbtList list) {
                    if ("count".equals(part)) {
                        return NbtInt.of(list.size());
                    }
                    Integer index = parseIntIndex(part);
                    if (index == null || index < 0 || index >= list.size()) {
                        return null;
                    }
                    current = list.get(index);
                    continue;
                }
                return null;
            }
            return current;
        }

        private static Integer parseIntIndex(String raw) {
            try {
                return Integer.parseInt(raw);
            } catch (Exception e) {
                return null;
            }
        }
    }

    public record ListMatcherKey(SourceNbt source, String listPath, int index) {
    }

    public record ListMatcher(String subPath, ValueMatcher matcher) {
    }

    public record ListIndexPath(String listPath, int index, String subPath) {
        public static ListIndexPath parse(String path) {
            if (path == null || path.isBlank()) {
                return null;
            }
            String[] parts = path.split("\\.");
            for (int i = 0; i < parts.length; i++) {
                boolean wildcard = "*".equals(parts[i]);
                Integer index = wildcard ? Integer.valueOf(-1) : tryParse(parts[i]);
                if (index == null) {
                    continue;
                }
                if (i == 0) {
                    return null;
                }
                if (!wildcard && i == parts.length - 1) {
                    return null;
                }
                String listPath = String.join(".", java.util.Arrays.copyOfRange(parts, 0, i));
                String subPath = i + 1 < parts.length
                        ? String.join(".", java.util.Arrays.copyOfRange(parts, i + 1, parts.length))
                        : "";
                return new ListIndexPath(listPath, index, subPath);
            }
            return null;
        }

        private static Integer tryParse(String raw) {
            try {
                return Integer.parseInt(raw);
            } catch (Exception e) {
                return null;
            }
        }
    }

    public record RangeMatcher(List<Range> ranges, boolean percent) {
        public boolean matches(int value, int max, boolean ignorePercent) {
            int checked = value;
            if (percent && !ignorePercent) {
                checked = (int) Math.floor((value * 100.0) / Math.max(1, max));
            }

            for (Range range : ranges) {
                if (checked >= range.min() && checked <= range.max()) {
                    return true;
                }
            }
            return false;
        }
    }

    public record Range(int min, int max) {
    }

    public record ValueMatcher(
            Mode mode,
            String value,
            Pattern regex,
            boolean ignoreCase,
            RangeMatcher rangeMatcher,
            Boolean existsExpected,
            boolean raw,
            boolean negate
    ) {
        public static ValueMatcher parse(String raw) {
            if (raw == null) {
                return null;
            }
            String value = raw.trim();
            if (value.isBlank()) {
                return null;
            }

            boolean negate = false;
            if (value.startsWith("!")) {
                negate = true;
                value = value.substring(1).trim();
            }

            boolean rawMode = false;
            if (value.startsWith("raw:")) {
                rawMode = true;
                value = value.substring("raw:".length());
            }

            if (value.startsWith("exists:")) {
                String flag = value.substring("exists:".length()).trim().toLowerCase(Locale.ROOT);
                Boolean expected = flag.equals("true") ? Boolean.TRUE : flag.equals("false") ? Boolean.FALSE : null;
                return new ValueMatcher(Mode.EXISTS, value, null, false, null, expected, rawMode, negate);
            }

            if (value.startsWith("range:")) {
                String rangeRaw = value.substring("range:".length());
                RangeMatcher matcher = parseRangeMatcher(rangeRaw);
                return matcher == null ? null : new ValueMatcher(Mode.RANGE, value, null, false, matcher, null, rawMode, negate);
            }

            if (value.startsWith("regex:")) {
                String pattern = value.substring("regex:".length());
                return new ValueMatcher(Mode.REGEX, pattern, Pattern.compile(pattern), false, null, null, rawMode, negate);
            }
            if (value.startsWith("iregex:")) {
                String pattern = value.substring("iregex:".length());
                return new ValueMatcher(Mode.REGEX, pattern, Pattern.compile(pattern, Pattern.CASE_INSENSITIVE), true, null, null, rawMode, negate);
            }
            if (value.startsWith("pattern:")) {
                return new ValueMatcher(Mode.PATTERN, value.substring("pattern:".length()), null, false, null, null, rawMode, negate);
            }
            if (value.startsWith("ipattern:")) {
                return new ValueMatcher(Mode.PATTERN, value.substring("ipattern:".length()), null, true, null, null, rawMode, negate);
            }

            return new ValueMatcher(Mode.EXACT, value, null, false, null, null, rawMode, negate);
        }

        public boolean matchesNullable(NbtElement element) {
            boolean result;
            if (mode == Mode.EXISTS) {
                boolean exists = element != null;
                result = existsExpected == null ? exists : existsExpected == exists;
            } else {
                if (element == null) {
                    result = false;
                } else {
                    result = matchesElement(element);
                }
            }
            return negate ? !result : result;
        }

        private boolean matchesElement(NbtElement element) {
            if (mode == Mode.RANGE) {
                Integer number = nbtToInt(element);
                return number != null && rangeMatcher != null && rangeMatcher.matches(number, number, true);
            }

            String actual = raw ? element.toString() : nbtToComparableString(element);
            if (actual == null) {
                return false;
            }

            if (mode == Mode.EXACT) {
                if (compareExact(actual, value, ignoreCase)) {
                    return true;
                }
                Integer expectedNum = NbtRenderOverrideResolver.parseIntFlexible(value).orElse(null);
                if (expectedNum != null) {
                    Integer actualNum = nbtToInt(element);
                    if (actualNum != null && actualNum.intValue() == expectedNum.intValue()) {
                        return true;
                    }
                }
                return false;
            }

            return switch (mode) {
                case PATTERN -> wildcardMatch(actual, value, ignoreCase);
                case REGEX -> regex.matcher(actual).matches();
                case RANGE, EXISTS, EXACT -> false;
            };
        }

        private static String nbtToComparableString(NbtElement element) {
            if (element instanceof NbtString nbtString) {
                return nbtString.asString();
            }
            if (element instanceof net.minecraft.nbt.NbtByte b && (b.byteValue() == 0 || b.byteValue() == 1)) {
                return b.byteValue() == 1 ? "true" : "false";
            }
            if (element instanceof AbstractNbtNumber number) {
                return number.numberValue().toString();
            }
            return element.toString();
        }

        private static Integer nbtToInt(NbtElement element) {
            if (element instanceof AbstractNbtNumber number) {
                return number.intValue();
            }
            if (element instanceof NbtString nbtString) {
                String raw = nbtString.asString();
                Integer parsed = NbtRenderOverrideResolver.parseIntFlexible(raw).orElse(null);
                if (parsed != null) {
                    return parsed;
                }
                Identifier id = Identifier.tryParse(raw);
                if (id != null) {
                    Integer legacy = NbtRenderOverrideResolver.LEGACY_STATUS_EFFECT_IDS.get(id);
                    if (legacy != null) {
                        return legacy;
                    }
                }
                return null;
            }
            String raw = element.toString();
            return NbtRenderOverrideResolver.parseIntFlexible(raw).orElse(null);
        }

        private static boolean compareExact(String actual, String expected, boolean ignoreCase) {
            return ignoreCase ? actual.equalsIgnoreCase(expected) : actual.equals(expected);
        }

        private static boolean wildcardMatch(String actual, String pattern, boolean ignoreCase) {
            String regex = Pattern.quote(pattern).replace("\\*", ".*").replace("\\?", ".");
            Pattern compiled = ignoreCase ? Pattern.compile(regex, Pattern.CASE_INSENSITIVE) : Pattern.compile(regex);
            return compiled.matcher(actual).matches();
        }
    }

    public enum Mode {
        EXACT,
        PATTERN,
        REGEX,
        RANGE,
        EXISTS
    }

    public static List<PathMatcherRule> parseMatchers(Properties properties) {
        List<PathMatcherRule> matchers = new ArrayList<>();
        for (String key : properties.stringPropertyNames()) {
            String normalized = CitPropertyUtils.stripNamespacePrefix(key);
            if (normalized.startsWith("nbt.")) {
                String path = normalized.substring("nbt.".length());
                ValueMatcher matcher = ValueMatcher.parse(properties.getProperty(key, ""));
                if (matcher != null) {
                    matchers.add(new PathMatcherRule(SourceNbt.FULL_STACK, path, matcher));
                }
            } else if (normalized.startsWith("components.") || normalized.startsWith("component.")) {
                String prefix = normalized.startsWith("components.") ? "components." : "component.";
                String path = NbtComponentUtils.normalizeComponentPath(normalized.substring(prefix.length()));
                ValueMatcher matcher = ValueMatcher.parse(properties.getProperty(key, ""));
                if (matcher != null && path != null && !path.isBlank()) {
                    matchers.add(new PathMatcherRule(SourceNbt.COMPONENTS_ONLY, path, matcher));
                }
            }
        }
        return matchers;
    }

    public static RangeMatcher parseRangeMatcher(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        List<Range> ranges = new ArrayList<>();
        boolean percent = raw.contains("%");
        String cleaned = raw.replace("%", "").trim();

        for (String tokenRaw : cleaned.split("\\s+")) {
            String token = tokenRaw.replace("(", "").replace(")", "");
            if (token.isBlank()) {
                continue;
            }

            int sep = NbtRenderOverrideResolver.findRangeSeparator(token);
            if (sep < 0) {
                Integer value = NbtRenderOverrideResolver.parseIntFlexible(token).orElse(null);
                if (value != null) {
                    ranges.add(new Range(value, value));
                }
            } else {
                String left = token.substring(0, sep);
                String right = token.substring(sep + 1);
                Integer min = left.isBlank() ? Integer.MIN_VALUE : NbtRenderOverrideResolver.parseIntFlexible(left).orElse(null);
                Integer max = right.isBlank() ? Integer.MAX_VALUE : NbtRenderOverrideResolver.parseIntFlexible(right).orElse(null);
                if (min != null && max != null) {
                    ranges.add(new Range(min, max));
                }
            }
        }

        return ranges.isEmpty() ? null : new RangeMatcher(ranges, percent);
    }

    public static boolean matchesListGroup(NbtList list, int preferredIndex, List<ListMatcher> matchers) {
        if (list == null || list.isEmpty()) {
            return false;
        }
        if (preferredIndex >= 0 && preferredIndex < list.size()) {
            if (matchesListElement(list.get(preferredIndex), matchers)) {
                return true;
            }
        }
        for (int i = 0; i < list.size(); i++) {
            if (i == preferredIndex) {
                continue;
            }
            if (matchesListElement(list.get(i), matchers)) {
                return true;
            }
        }
        return false;
    }

    public static boolean allMatchersMatchNull(List<ListMatcher> matchers) {
        if (matchers == null || matchers.isEmpty()) {
            return true;
        }
        for (ListMatcher matcher : matchers) {
            if (!matcher.matcher().matchesNullable(null)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesListElement(NbtElement element, List<ListMatcher> matchers) {
        if (element == null) {
            return false;
        }
        if (matchers == null || matchers.isEmpty()) {
            return true;
        }
        for (ListMatcher matcher : matchers) {
            NbtElement resolved = matcher.subPath().isEmpty()
                    ? element
                    : PathMatcherRule.resolvePathOnElement(element, matcher.subPath());
            if (!matcher.matcher().matchesNullable(resolved)) {
                return false;
            }
        }
        return true;
    }
}
