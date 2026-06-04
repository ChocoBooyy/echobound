package echobound.config;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public final class Config {

    public static final String MSG_UNKNOWN = "unknownCommand";
    public static final String MSG_NO_EXIT = "noExit";
    public static final String MSG_LOCKED_EXIT = "lockedExit";
    public static final String MSG_INVENTORY_FULL = "inventoryFull";
    public static final String MSG_NOTHING_HERE = "nothingHere";
    public static final String MSG_ALREADY_SOLVED = "alreadySolved";
    public static final String MSG_PUZZLE_REJECT = "puzzleReject";
    public static final String MSG_TAKEN = "taken";
    public static final String MSG_USE_ITEM = "useItem";
    public static final String MSG_SAVE_OK = "saveOk";
    public static final String MSG_SAVE_FAIL = "saveFail";
    public static final String MSG_LOAD_OK = "loadOk";
    public static final String MSG_LOAD_NONE = "loadNone";
    public static final String MSG_LOAD_CORRUPT = "loadCorrupt";
    public static final String MSG_CORRUPTED_TAG = "corruptedTag";
    public static final String MSG_NARRATIVE_BLOCKED = "narrativeBlocked";
    public static final String MSG_EXITS_LABEL = "exitsLabel";
    public static final String MSG_ITEMS_LABEL = "itemsLabel";
    public static final String MSG_LOGS_LABEL = "logsLabel";
    public static final String MSG_INVENTORY_LABEL = "inventoryLabel";
    public static final String MSG_INVENTORY_EMPTY = "inventoryEmpty";
    public static final String MSG_HINT_CIPHER = "hintCipher";
    public static final String MSG_HINT_LOGIC = "hintLogic";
    public static final String MSG_HINT_NARRATIVE = "hintNarrative";
    public static final String MSG_MAP_HEADER = "mapHeader";
    public static final String MSG_MAP_LOCKED = "mapLocked";
    public static final String MSG_ENDING_HINT = "endingHint";
    public static final String MSG_DEBUG_TRUST = "debugTrust";

    public static final String TEXT_INTRO = "intro";
    public static final String TEXT_HELP = "help";

    private static final String RESOURCE = "/config.json";
    private static final String KEY_DELAY = "typewriterDelayMs";
    private static final String KEY_LIMIT = "inventoryLimit";
    private static final String KEY_DEBUG = "debug";
    private static final String KEY_MESSAGES = "messages";
    private static final String KEY_TEXT = "text";

    private static final int DEFAULT_DELAY = 18;
    private static final int DEFAULT_LIMIT = 6;

    private final int typewriterDelayMs;
    private final int inventoryLimit;
    private final boolean debug;
    private final JSONObject messages;
    private final JSONObject text;

    private Config(int typewriterDelayMs, int inventoryLimit, boolean debug, JSONObject messages, JSONObject text) {
        this.typewriterDelayMs = typewriterDelayMs;
        this.inventoryLimit = inventoryLimit;
        this.debug = debug;
        this.messages = messages;
        this.text = text;
    }

    public static Config load() {
        try (InputStream in = Config.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                return defaults();
            }
            String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(new JSONTokener(body));
            return new Config(
                root.optInt(KEY_DELAY, DEFAULT_DELAY),
                root.optInt(KEY_LIMIT, DEFAULT_LIMIT),
                root.optBoolean(KEY_DEBUG, false),
                root.optJSONObject(KEY_MESSAGES, new JSONObject()),
                root.optJSONObject(KEY_TEXT, new JSONObject()));
        } catch (Exception e) {
            return defaults();
        }
    }

    private static Config defaults() {
        return new Config(DEFAULT_DELAY, DEFAULT_LIMIT, false, new JSONObject(), new JSONObject());
    }

    public int typewriterDelayMs() {
        return typewriterDelayMs;
    }

    public int inventoryLimit() {
        return inventoryLimit;
    }

    public boolean debug() {
        return debug;
    }

    public String msg(String key) {
        return messages.optString(key, key);
    }

    public List<String> text(String key) {
        List<String> lines = new ArrayList<>();
        JSONArray array = text.optJSONArray(key);
        if (array == null) {
            return lines;
        }
        for (int i = 0; i < array.length(); i++) {
            lines.add(array.getString(i));
        }
        return lines;
    }
}
