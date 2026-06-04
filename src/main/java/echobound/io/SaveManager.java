package echobound.io;

import echobound.GameState;
import echobound.Terminal;
import echobound.config.Config;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.json.JSONArray;
import org.json.JSONObject;

public final class SaveManager {

    private static final String SAVE_FILE = "save.json";
    private static final int INDENT = 2;

    private static final String KEY_ROOM = "room";
    private static final String KEY_TRUST = "trust";
    private static final String KEY_INVENTORY = "inventory";
    private static final String KEY_VISITED = "visited";
    private static final String KEY_FLAGS = "flags";
    private static final String KEY_LOGS = "logs";
    private static final String KEY_PUZZLES = "puzzles";
    private static final String KEY_TRIGGERS = "triggers";

    private final Terminal terminal;
    private final Config config;

    public SaveManager(Terminal terminal, Config config) {
        this.terminal = terminal;
        this.config = config;
    }

    public void save(GameState state) {
        JSONObject root = new JSONObject();
        root.put(KEY_ROOM, state.currentRoomId());
        root.put(KEY_TRUST, state.wardTrust());
        root.put(KEY_INVENTORY, new JSONArray(state.inventory()));
        root.put(KEY_VISITED, new JSONArray(state.visitedRooms()));
        root.put(KEY_FLAGS, new JSONArray(state.flags()));
        root.put(KEY_LOGS, new JSONArray(state.discoveredLogs()));
        root.put(KEY_PUZZLES, new JSONArray(state.solvedPuzzles()));
        root.put(KEY_TRIGGERS, new JSONArray(state.firedTriggers()));
        try {
            Files.writeString(Path.of(SAVE_FILE), root.toString(INDENT), StandardCharsets.UTF_8);
            terminal.print(Terminal.Voice.SYSTEM, String.format(config.msg(Config.MSG_SAVE_OK), SAVE_FILE));
        } catch (IOException e) {
            terminal.print(Terminal.Voice.ERROR, config.msg(Config.MSG_SAVE_FAIL));
        }
    }

    public Optional<GameState> load() {
        Path path = Path.of(SAVE_FILE);
        if (!Files.exists(path)) {
            terminal.print(Terminal.Voice.SYSTEM, config.msg(Config.MSG_LOAD_NONE));
            return Optional.empty();
        }
        try {
            JSONObject root = new JSONObject(Files.readString(path, StandardCharsets.UTF_8));
            GameState state = new GameState(root.getString(KEY_ROOM), root.getInt(KEY_TRUST));
            readInto(root.getJSONArray(KEY_INVENTORY), state::addItem);
            readInto(root.getJSONArray(KEY_VISITED), state::markVisited);
            readInto(root.getJSONArray(KEY_FLAGS), state::setFlag);
            readInto(root.getJSONArray(KEY_LOGS), state::addLog);
            readInto(root.getJSONArray(KEY_PUZZLES), state::markSolved);
            readInto(root.getJSONArray(KEY_TRIGGERS), state::markTriggerFired);
            return Optional.of(state);
        } catch (Exception e) {
            terminal.print(Terminal.Voice.ERROR, config.msg(Config.MSG_LOAD_CORRUPT));
            return Optional.empty();
        }
    }

    private static void readInto(JSONArray array, java.util.function.Consumer<String> sink) {
        for (int i = 0; i < array.length(); i++) {
            sink.accept(array.getString(i));
        }
    }
}
