package echobound;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class GameState {

    private String currentRoomId;
    private int wardTrust;
    private final List<String> inventory = new ArrayList<>();
    private final Set<String> visitedRooms = new HashSet<>();
    private final Set<String> flags = new HashSet<>();
    private final Set<String> discoveredLogs = new HashSet<>();
    private final Set<String> solvedPuzzles = new HashSet<>();
    private final Set<String> firedTriggers = new HashSet<>();

    public GameState(String startRoomId, int wardTrust) {
        this.currentRoomId = startRoomId;
        this.wardTrust = wardTrust;
        this.visitedRooms.add(startRoomId);
    }

    public String currentRoomId() {
        return currentRoomId;
    }

    public void moveTo(String roomId) {
        this.currentRoomId = roomId;
        this.visitedRooms.add(roomId);
    }

    public List<String> inventory() {
        return inventory;
    }

    public boolean hasItem(String itemId) {
        return inventory.contains(itemId);
    }

    public void addItem(String itemId) {
        inventory.add(itemId);
    }

    public boolean inventoryFull(int limit) {
        return inventory.size() >= limit;
    }

    public void markVisited(String roomId) {
        visitedRooms.add(roomId);
    }

    public Set<String> visitedRooms() {
        return visitedRooms;
    }

    public boolean hasVisited(String roomId) {
        return visitedRooms.contains(roomId);
    }

    public Set<String> flags() {
        return flags;
    }

    public boolean hasFlag(String flag) {
        return flags.contains(flag);
    }

    public void setFlag(String flag) {
        flags.add(flag);
    }

    public Set<String> discoveredLogs() {
        return discoveredLogs;
    }

    public boolean hasLog(String logId) {
        return discoveredLogs.contains(logId);
    }

    public void addLog(String logId) {
        discoveredLogs.add(logId);
    }

    public Set<String> solvedPuzzles() {
        return solvedPuzzles;
    }

    public boolean isSolved(String puzzleId) {
        return solvedPuzzles.contains(puzzleId);
    }

    public void markSolved(String puzzleId) {
        solvedPuzzles.add(puzzleId);
    }

    public Set<String> firedTriggers() {
        return firedTriggers;
    }

    public boolean hasFiredTrigger(String trigger) {
        return firedTriggers.contains(trigger);
    }

    public void markTriggerFired(String trigger) {
        firedTriggers.add(trigger);
    }

    public int wardTrust() {
        return wardTrust;
    }

    public void adjustTrust(int delta) {
        wardTrust += delta;
    }

    public void setWardTrust(int value) {
        this.wardTrust = value;
    }
}
