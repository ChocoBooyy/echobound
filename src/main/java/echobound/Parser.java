package echobound;

import java.util.Map;
import java.util.Set;

public final class Parser {

    private static final String VERB_GO = "go";

    private static final Set<String> DIRECTIONS =
        Set.of("north", "south", "east", "west", "up", "down");

    private static final Map<String, String> DIRECTION_ALIASES = Map.of(
        "n", "north",
        "s", "south",
        "e", "east",
        "w", "west",
        "u", "up",
        "d", "down");

    private final Set<String> verbs;

    public Parser(Set<String> verbs) {
        this.verbs = Set.copyOf(verbs);
    }

    public Command parse(String raw) {
        String normalized = normalize(raw);
        if (normalized.isEmpty()) {
            return new Command("", "", raw);
        }
        String[] parts = normalized.split(" ", 2);
        String first = parts[0];
        String rest = parts.length > 1 ? parts[1] : "";
        if (isDirection(first)) {
            return new Command(VERB_GO, canonicalDirection(first), raw);
        }
        return new Command(resolveVerb(first), canonicalDirection(rest), raw);
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private boolean isDirection(String token) {
        return DIRECTIONS.contains(token) || DIRECTION_ALIASES.containsKey(token);
    }

    private String canonicalDirection(String token) {
        return DIRECTION_ALIASES.getOrDefault(token, token);
    }

    private String resolveVerb(String token) {
        if (verbs.contains(token)) {
            return token;
        }
        String match = null;
        for (String verb : verbs) {
            if (verb.startsWith(token)) {
                if (match != null) {
                    return token;
                }
                match = verb;
            }
        }
        return match == null ? token : match;
    }
}
