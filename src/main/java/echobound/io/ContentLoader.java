package echobound.io;

import echobound.puzzle.CipherPuzzle;
import echobound.puzzle.LogicPuzzle;
import echobound.puzzle.NarrativePuzzle;
import echobound.puzzle.Puzzle;
import echobound.ward.WardResponse;
import echobound.world.Exit;
import echobound.world.Item;
import echobound.world.Log;
import echobound.world.Room;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public final class ContentLoader {

    public record Ending(
        String id,
        int priority,
        int minLogs,
        int minPuzzles,
        int minTrust,
        String title,
        List<String> text) {}

    private static final String ROOMS_RESOURCE = "/rooms.json";
    private static final String ITEMS_RESOURCE = "/items.json";
    private static final String LOGS_RESOURCE = "/logs.json";
    private static final String PUZZLES_RESOURCE = "/puzzles.json";
    private static final String WARD_RESOURCE = "/ward.json";
    private static final String ENDINGS_RESOURCE = "/endings.json";

    private static final String KEY_START = "start";
    private static final String KEY_ROOMS = "rooms";
    private static final String KEY_ITEMS = "items";
    private static final String KEY_LOGS = "logs";
    private static final String KEY_PUZZLES = "puzzles";
    private static final String KEY_RESPONSES = "responses";
    private static final String KEY_ENDINGS = "endings";
    private static final String KEY_TRUST_START = "trustStart";
    private static final String KEY_TRIGGER_ROOM = "triggerRoom";
    private static final String KEY_TRIGGER_TARGET = "triggerTarget";

    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_EXITS = "exits";
    private static final String KEY_DIRECTION = "direction";
    private static final String KEY_TARGET = "target";
    private static final String KEY_REQUIRED_FLAG = "requiredFlag";
    private static final String KEY_LOCKED_MESSAGE = "lockedMessage";
    private static final String KEY_START_ROOM = "startRoom";
    private static final String KEY_TITLE = "title";
    private static final String KEY_CONTENT = "content";
    private static final String KEY_CORRUPTED = "corruptedContent";
    private static final String KEY_REPAIR_PUZZLE = "repairPuzzle";
    private static final String KEY_ROOM = "room";
    private static final String KEY_TYPE = "type";
    private static final String KEY_PROMPT = "prompt";
    private static final String KEY_SUCCESS_LINE = "successLine";
    private static final String KEY_SOLVED_FLAG = "solvedFlag";
    private static final String KEY_ANSWERS = "answers";
    private static final String KEY_CIPHER_TEXT = "cipherText";
    private static final String KEY_CIPHER_KEY = "key";
    private static final String KEY_REQUIRED_LOGS = "requiredLogs";
    private static final String KEY_TRIGGER = "trigger";
    private static final String KEY_LINE = "line";
    private static final String KEY_TRUST_DELTA = "trustDelta";
    private static final String KEY_PRIORITY = "priority";
    private static final String KEY_MIN_LOGS = "minLogs";
    private static final String KEY_MIN_PUZZLES = "minPuzzles";
    private static final String KEY_MIN_TRUST = "minTrust";
    private static final String KEY_TEXT = "text";

    private static final String TYPE_CIPHER = "cipher";
    private static final String TYPE_LOGIC = "logic";
    private static final String TYPE_NARRATIVE = "narrative";

    private final Map<String, Room> rooms;
    private final Map<String, Item> items;
    private final Map<String, Log> logs;
    private final Map<String, Puzzle> puzzles;
    private final List<WardResponse> wardResponses;
    private final List<Ending> endings;
    private final String startRoomId;
    private final int wardTrustStart;
    private final String endingTriggerRoom;
    private final String endingTriggerTarget;

    public ContentLoader() {
        JSONObject roomsRoot = read(ROOMS_RESOURCE);
        this.rooms = parseRooms(roomsRoot.getJSONArray(KEY_ROOMS));
        this.startRoomId = roomsRoot.getString(KEY_START);
        this.items = parseItems(read(ITEMS_RESOURCE).getJSONArray(KEY_ITEMS));
        this.logs = parseLogs(read(LOGS_RESOURCE).getJSONArray(KEY_LOGS));
        this.puzzles = parsePuzzles(read(PUZZLES_RESOURCE).getJSONArray(KEY_PUZZLES));
        JSONObject wardRoot = read(WARD_RESOURCE);
        this.wardResponses = parseWard(wardRoot.getJSONArray(KEY_RESPONSES));
        this.wardTrustStart = wardRoot.optInt(KEY_TRUST_START, 0);
        JSONObject endingsRoot = read(ENDINGS_RESOURCE);
        this.endings = parseEndings(endingsRoot.getJSONArray(KEY_ENDINGS));
        this.endingTriggerRoom = endingsRoot.getString(KEY_TRIGGER_ROOM);
        this.endingTriggerTarget = endingsRoot.getString(KEY_TRIGGER_TARGET);
    }

    public Map<String, Room> rooms() {
        return rooms;
    }

    public Room room(String id) {
        return rooms.get(id);
    }

    public Item item(String id) {
        return items.get(id);
    }

    public Log log(String id) {
        return logs.get(id);
    }

    public Puzzle puzzle(String id) {
        return puzzles.get(id);
    }

    public List<WardResponse> wardResponses() {
        return wardResponses;
    }

    public List<Ending> endings() {
        return endings;
    }

    public String startRoomId() {
        return startRoomId;
    }

    public int wardTrustStart() {
        return wardTrustStart;
    }

    public String endingTriggerRoom() {
        return endingTriggerRoom;
    }

    public String endingTriggerTarget() {
        return endingTriggerTarget;
    }

    private static Map<String, Room> parseRooms(JSONArray array) {
        Map<String, Room> map = new LinkedHashMap<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            Room room = new Room(
                object.getString(KEY_ID),
                object.getString(KEY_NAME),
                object.getString(KEY_DESCRIPTION),
                parseExits(object.getJSONArray(KEY_EXITS)),
                stringList(object.getJSONArray(KEY_ITEMS)),
                stringList(object.getJSONArray(KEY_LOGS)),
                stringList(object.getJSONArray(KEY_PUZZLES)));
            map.put(room.id(), room);
        }
        return map;
    }

    private static List<Exit> parseExits(JSONArray array) {
        List<Exit> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            list.add(new Exit(
                object.getString(KEY_DIRECTION),
                object.getString(KEY_TARGET),
                object.getString(KEY_REQUIRED_FLAG),
                object.getString(KEY_LOCKED_MESSAGE)));
        }
        return list;
    }

    private static Map<String, Item> parseItems(JSONArray array) {
        Map<String, Item> map = new LinkedHashMap<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            Item item = new Item(
                object.getString(KEY_ID),
                object.getString(KEY_NAME),
                object.getString(KEY_DESCRIPTION),
                object.getString(KEY_START_ROOM));
            map.put(item.id(), item);
        }
        return map;
    }

    private static Map<String, Log> parseLogs(JSONArray array) {
        Map<String, Log> map = new LinkedHashMap<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            Log log = new Log(
                object.getString(KEY_ID),
                object.getString(KEY_TITLE),
                object.getString(KEY_CONTENT),
                object.getString(KEY_CORRUPTED),
                object.getString(KEY_REPAIR_PUZZLE),
                object.getString(KEY_ROOM));
            map.put(log.id(), log);
        }
        return map;
    }

    private static Map<String, Puzzle> parsePuzzles(JSONArray array) {
        Map<String, Puzzle> map = new LinkedHashMap<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            Puzzle puzzle = switch (object.getString(KEY_TYPE)) {
                case TYPE_CIPHER -> new CipherPuzzle(
                    object.getString(KEY_ID),
                    object.getString(KEY_ROOM),
                    object.getString(KEY_TARGET),
                    object.getString(KEY_PROMPT),
                    object.getString(KEY_SUCCESS_LINE),
                    object.getString(KEY_SOLVED_FLAG),
                    object.getString(KEY_CIPHER_TEXT),
                    object.getString(KEY_CIPHER_KEY),
                    stringList(object.getJSONArray(KEY_ANSWERS)));
                case TYPE_LOGIC -> new LogicPuzzle(
                    object.getString(KEY_ID),
                    object.getString(KEY_ROOM),
                    object.getString(KEY_TARGET),
                    object.getString(KEY_PROMPT),
                    object.getString(KEY_SUCCESS_LINE),
                    object.getString(KEY_SOLVED_FLAG),
                    stringList(object.getJSONArray(KEY_ANSWERS)));
                case TYPE_NARRATIVE -> new NarrativePuzzle(
                    object.getString(KEY_ID),
                    object.getString(KEY_ROOM),
                    object.getString(KEY_TARGET),
                    object.getString(KEY_PROMPT),
                    object.getString(KEY_SUCCESS_LINE),
                    object.getString(KEY_SOLVED_FLAG),
                    stringList(object.getJSONArray(KEY_ANSWERS)),
                    stringList(object.getJSONArray(KEY_REQUIRED_LOGS)));
                default -> throw new IllegalStateException("unknown puzzle type: " + object.getString(KEY_TYPE));
            };
            map.put(puzzle.id(), puzzle);
        }
        return map;
    }

    private static List<WardResponse> parseWard(JSONArray array) {
        List<WardResponse> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            list.add(new WardResponse(
                object.getString(KEY_TRIGGER),
                object.getString(KEY_LINE),
                object.getInt(KEY_TRUST_DELTA)));
        }
        return list;
    }

    private static List<Ending> parseEndings(JSONArray array) {
        List<Ending> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            list.add(new Ending(
                object.getString(KEY_ID),
                object.getInt(KEY_PRIORITY),
                object.getInt(KEY_MIN_LOGS),
                object.getInt(KEY_MIN_PUZZLES),
                object.getInt(KEY_MIN_TRUST),
                object.getString(KEY_TITLE),
                stringList(object.getJSONArray(KEY_TEXT))));
        }
        list.sort((left, right) -> Integer.compare(left.priority(), right.priority()));
        return list;
    }

    private static List<String> stringList(JSONArray array) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(array.getString(i));
        }
        return list;
    }

    private static JSONObject read(String resource) {
        try (InputStream in = ContentLoader.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("missing resource: " + resource);
            }
            String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return new JSONObject(new JSONTokener(body));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("failed to read resource: " + resource, e);
        }
    }
}
