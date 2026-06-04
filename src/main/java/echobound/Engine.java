package echobound;

import echobound.config.Config;
import echobound.io.ContentLoader;
import echobound.io.SaveManager;
import echobound.puzzle.CipherPuzzle;
import echobound.puzzle.LogicPuzzle;
import echobound.puzzle.NarrativePuzzle;
import echobound.puzzle.Puzzle;
import echobound.ward.Ward;
import echobound.world.Exit;
import echobound.world.Item;
import echobound.world.Log;
import echobound.world.Room;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class Engine {

    private static final String VERB_GO = "go";
    private static final String VERB_TAKE = "take";
    private static final String VERB_USE = "use";
    private static final String VERB_READ = "read";
    private static final String VERB_EXAMINE = "examine";
    private static final String VERB_INVENTORY = "inventory";
    private static final String VERB_MAP = "map";
    private static final String VERB_HELP = "help";
    private static final String VERB_SAVE = "save";
    private static final String VERB_LOAD = "load";
    private static final String VERB_QUIT = "quit";
    private static final String VERB_NONE = "";

    private static final String TRIGGER_START = "start";
    private static final String TRIGGER_ENTER = "enter:";
    private static final String TRIGGER_READ = "read:";
    private static final String TRIGGER_SOLVE = "solve:";
    private static final String TRIGGER_TAKE = "take:";

    private static final String LIST_SEPARATOR = ", ";
    private static final String SPACE = " ";

    private final Config config;
    private final Terminal terminal;
    private final ContentLoader loader;
    private final Parser parser;
    private final Ward ward;
    private final SaveManager saveManager;

    private GameState state;
    private boolean running;

    public Engine() {
        this.config = Config.load();
        this.terminal = new Terminal(config);
        this.loader = new ContentLoader();
        this.parser = new Parser(verbs());
        this.ward = new Ward(loader.wardResponses());
        this.saveManager = new SaveManager(terminal, config);
        this.state = new GameState(loader.startRoomId(), loader.wardTrustStart());
    }

    public void run() {
        running = true;
        for (String line : config.text(Config.TEXT_INTRO)) {
            terminal.print(Terminal.Voice.ROOM, line);
        }
        terminal.blank();
        fireWard(TRIGGER_START);
        describeRoom();
        loop();
    }

    private void loop() {
        while (running) {
            terminal.blank();
            terminal.prompt();
            String raw = terminal.readLine();
            if (raw == null) {
                running = false;
                return;
            }
            dispatch(parser.parse(raw));
            reportDebug();
        }
    }

    private void dispatch(Command command) {
        switch (command.verb()) {
            case VERB_GO -> move(command.noun());
            case VERB_TAKE -> take(command.noun());
            case VERB_USE -> use(command.noun());
            case VERB_READ -> read(command.noun());
            case VERB_EXAMINE -> examine(command.noun());
            case VERB_INVENTORY -> showInventory();
            case VERB_MAP -> showMap();
            case VERB_HELP -> showHelp();
            case VERB_SAVE -> saveManager.save(state);
            case VERB_LOAD -> loadGame();
            case VERB_QUIT -> running = false;
            case VERB_NONE -> { }
            default -> terminal.print(Terminal.Voice.ERROR, config.msg(Config.MSG_UNKNOWN));
        }
    }

    private void move(String direction) {
        Exit exit = findExit(direction);
        if (exit == null) {
            terminal.print(Terminal.Voice.ERROR, config.msg(Config.MSG_NO_EXIT));
            return;
        }
        if (!exit.isOpen(state.flags())) {
            String locked = exit.lockedMessage().isEmpty()
                ? config.msg(Config.MSG_LOCKED_EXIT)
                : exit.lockedMessage();
            terminal.print(Terminal.Voice.ERROR, locked);
            return;
        }
        state.moveTo(exit.targetRoomId());
        describeRoom();
        fireWard(TRIGGER_ENTER + exit.targetRoomId());
    }

    private void take(String noun) {
        Item item = matchInList(noun, itemsInRoom());
        if (item == null) {
            terminal.print(Terminal.Voice.ERROR, config.msg(Config.MSG_NOTHING_HERE));
            return;
        }
        if (state.inventoryFull(config.inventoryLimit())) {
            terminal.print(Terminal.Voice.ERROR, config.msg(Config.MSG_INVENTORY_FULL));
            return;
        }
        state.addItem(item.id());
        terminal.print(Terminal.Voice.SYSTEM, String.format(config.msg(Config.MSG_TAKEN), item.name()));
        fireWard(TRIGGER_TAKE + item.id());
    }

    private void use(String noun) {
        if (isEndingTrigger(noun)) {
            triggerEnding();
            return;
        }
        Puzzle puzzle = puzzleInRoom(noun);
        if (puzzle != null) {
            attemptPuzzle(puzzle);
            return;
        }
        Item item = matchInList(noun, inventoryItems());
        if (item != null) {
            terminal.print(Terminal.Voice.ROOM, String.format(config.msg(Config.MSG_USE_ITEM), item.name(), item.description()));
            return;
        }
        terminal.print(Terminal.Voice.ERROR, config.msg(Config.MSG_NOTHING_HERE));
    }

    private void read(String noun) {
        Log log = findLog(noun);
        if (log == null) {
            terminal.print(Terminal.Voice.ERROR, config.msg(Config.MSG_NOTHING_HERE));
            return;
        }
        if (log.isCorrupted() && !state.isSolved(log.repairPuzzle())) {
            terminal.print(Terminal.Voice.LOG, log.title());
            terminal.print(Terminal.Voice.LOG, log.corruptedContent());
            terminal.print(Terminal.Voice.SYSTEM, config.msg(Config.MSG_CORRUPTED_TAG));
            return;
        }
        terminal.print(Terminal.Voice.LOG, log.title());
        terminal.print(Terminal.Voice.LOG, log.content());
        if (!state.hasLog(log.id())) {
            state.addLog(log.id());
            fireWard(TRIGGER_READ + log.id());
        }
    }

    private void examine(String noun) {
        if (isEndingTrigger(noun)) {
            terminal.print(Terminal.Voice.SYSTEM, config.msg(Config.MSG_ENDING_HINT));
            return;
        }
        Puzzle puzzle = puzzleInRoom(noun);
        if (puzzle != null) {
            attemptPuzzle(puzzle);
            return;
        }
        Item item = matchInList(noun, examinableItems());
        if (item != null) {
            terminal.print(Terminal.Voice.ROOM, item.description());
            return;
        }
        Log log = findLog(noun);
        if (log != null) {
            terminal.print(Terminal.Voice.ROOM, log.title());
            return;
        }
        terminal.print(Terminal.Voice.ROOM, config.msg(Config.MSG_NOTHING_HERE));
    }

    private void attemptPuzzle(Puzzle puzzle) {
        if (state.isSolved(puzzle.id())) {
            terminal.print(Terminal.Voice.SYSTEM, config.msg(Config.MSG_ALREADY_SOLVED));
            return;
        }
        if (puzzle instanceof NarrativePuzzle narrative && !logsRead(narrative.requiredLogs())) {
            terminal.print(Terminal.Voice.SYSTEM, config.msg(Config.MSG_NARRATIVE_BLOCKED));
            return;
        }
        terminal.print(Terminal.Voice.SYSTEM, puzzle.prompt());
        terminal.print(Terminal.Voice.SYSTEM, hintFor(puzzle));
        terminal.prompt();
        String answer = terminal.readLine();
        if (answer == null) {
            running = false;
            return;
        }
        if (puzzle.accepts(answer)) {
            state.markSolved(puzzle.id());
            state.setFlag(puzzle.solvedFlag());
            terminal.print(Terminal.Voice.SYSTEM, puzzle.successLine());
            fireWard(TRIGGER_SOLVE + puzzle.id());
        } else {
            terminal.print(Terminal.Voice.ERROR, config.msg(Config.MSG_PUZZLE_REJECT));
        }
    }

    private String hintFor(Puzzle puzzle) {
        return switch (puzzle) {
            case CipherPuzzle cipher -> String.format(config.msg(Config.MSG_HINT_CIPHER), cipher.cipherText());
            case LogicPuzzle ignored -> config.msg(Config.MSG_HINT_LOGIC);
            case NarrativePuzzle ignored -> config.msg(Config.MSG_HINT_NARRATIVE);
        };
    }

    private void triggerEnding() {
        ContentLoader.Ending chosen = selectEnding();
        terminal.blank();
        terminal.print(Terminal.Voice.SYSTEM, chosen.title());
        for (String line : chosen.text()) {
            terminal.print(Terminal.Voice.ROOM, line);
        }
        running = false;
    }

    private ContentLoader.Ending selectEnding() {
        int logs = state.discoveredLogs().size();
        int puzzles = state.solvedPuzzles().size();
        int trust = state.wardTrust();
        ContentLoader.Ending fallback = null;
        for (ContentLoader.Ending ending : loader.endings()) {
            fallback = ending;
            if (logs >= ending.minLogs() && puzzles >= ending.minPuzzles() && trust >= ending.minTrust()) {
                return ending;
            }
        }
        return fallback;
    }

    private void showInventory() {
        List<Item> items = inventoryItems();
        if (items.isEmpty()) {
            terminal.print(Terminal.Voice.ROOM, config.msg(Config.MSG_INVENTORY_EMPTY));
            return;
        }
        List<String> names = new ArrayList<>();
        for (Item item : items) {
            names.add(item.name());
        }
        terminal.print(Terminal.Voice.ROOM, config.msg(Config.MSG_INVENTORY_LABEL) + SPACE + String.join(LIST_SEPARATOR, names));
    }

    private void showMap() {
        terminal.print(Terminal.Voice.SYSTEM, config.msg(Config.MSG_MAP_HEADER));
        for (String roomId : state.visitedRooms()) {
            Room room = loader.room(roomId);
            terminal.print(Terminal.Voice.ROOM, room.name() + SPACE + exitSummary(room));
        }
    }

    private String exitSummary(Room room) {
        List<String> parts = new ArrayList<>();
        for (Exit exit : room.exits()) {
            if (exit.isOpen(state.flags())) {
                parts.add(exit.direction());
            } else {
                parts.add(exit.direction() + SPACE + config.msg(Config.MSG_MAP_LOCKED));
            }
        }
        return config.msg(Config.MSG_EXITS_LABEL) + SPACE + String.join(LIST_SEPARATOR, parts);
    }

    private void showHelp() {
        for (String line : config.text(Config.TEXT_HELP)) {
            terminal.print(Terminal.Voice.ROOM, line);
        }
    }

    private void loadGame() {
        saveManager.load().ifPresent(loaded -> {
            this.state = loaded;
            terminal.print(Terminal.Voice.SYSTEM, config.msg(Config.MSG_LOAD_OK));
            describeRoom();
        });
    }

    private void describeRoom() {
        Room room = currentRoom();
        terminal.print(Terminal.Voice.SYSTEM, room.name());
        terminal.print(Terminal.Voice.ROOM, room.description());
        terminal.print(Terminal.Voice.ROOM, exitSummary(room));
        List<Item> items = itemsInRoom();
        if (!items.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (Item item : items) {
                names.add(item.name());
            }
            terminal.print(Terminal.Voice.ROOM, config.msg(Config.MSG_ITEMS_LABEL) + SPACE + String.join(LIST_SEPARATOR, names));
        }
        if (!room.logIds().isEmpty()) {
            List<String> titles = new ArrayList<>();
            for (String logId : room.logIds()) {
                titles.add(loader.log(logId).title());
            }
            terminal.print(Terminal.Voice.ROOM, config.msg(Config.MSG_LOGS_LABEL) + SPACE + String.join(LIST_SEPARATOR, titles));
        }
    }

    private void reportDebug() {
        if (config.debug()) {
            terminal.print(Terminal.Voice.SYSTEM, String.format(config.msg(Config.MSG_DEBUG_TRUST), state.wardTrust()));
        }
    }

    private void fireWard(String trigger) {
        ward.reactTo(trigger, state).ifPresent(line -> terminal.print(Terminal.Voice.WARD, line));
    }

    private Room currentRoom() {
        return loader.room(state.currentRoomId());
    }

    private Exit findExit(String direction) {
        for (Exit exit : currentRoom().exits()) {
            if (exit.direction().equals(direction)) {
                return exit;
            }
        }
        return null;
    }

    private boolean isEndingTrigger(String noun) {
        return state.currentRoomId().equals(loader.endingTriggerRoom())
            && matches(noun, loader.endingTriggerTarget());
    }

    private Puzzle puzzleInRoom(String noun) {
        for (String puzzleId : currentRoom().puzzleIds()) {
            Puzzle puzzle = loader.puzzle(puzzleId);
            if (matches(noun, puzzle.target(), puzzle.id())) {
                return puzzle;
            }
        }
        return null;
    }

    private Log findLog(String noun) {
        List<String> candidates = new ArrayList<>(currentRoom().logIds());
        for (String logId : state.discoveredLogs()) {
            if (!candidates.contains(logId)) {
                candidates.add(logId);
            }
        }
        for (String logId : candidates) {
            Log log = loader.log(logId);
            if (matches(noun, log.id(), log.title())) {
                return log;
            }
        }
        return null;
    }

    private List<Item> itemsInRoom() {
        List<Item> items = new ArrayList<>();
        for (String itemId : currentRoom().itemIds()) {
            if (!state.hasItem(itemId)) {
                items.add(loader.item(itemId));
            }
        }
        return items;
    }

    private List<Item> inventoryItems() {
        List<Item> items = new ArrayList<>();
        for (String itemId : state.inventory()) {
            items.add(loader.item(itemId));
        }
        return items;
    }

    private List<Item> examinableItems() {
        List<Item> items = new ArrayList<>(itemsInRoom());
        items.addAll(inventoryItems());
        return items;
    }

    private Item matchInList(String noun, List<Item> items) {
        for (Item item : items) {
            if (matches(noun, item.id(), item.name())) {
                return item;
            }
        }
        return null;
    }

    private boolean logsRead(List<String> logIds) {
        for (String logId : logIds) {
            if (!state.hasLog(logId)) {
                return false;
            }
        }
        return true;
    }

    private boolean matches(String noun, String... fields) {
        if (noun.isEmpty()) {
            return false;
        }
        for (String field : fields) {
            String normalized = Parser.normalize(field);
            if (normalized.equals(noun) || normalized.contains(noun)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> verbs() {
        return Set.of(VERB_GO, VERB_TAKE, VERB_USE, VERB_READ, VERB_EXAMINE,
            VERB_INVENTORY, VERB_MAP, VERB_HELP, VERB_SAVE, VERB_LOAD, VERB_QUIT);
    }
}
