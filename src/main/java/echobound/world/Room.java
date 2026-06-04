package echobound.world;

import java.util.List;

public record Room(
    String id,
    String name,
    String description,
    List<Exit> exits,
    List<String> itemIds,
    List<String> logIds,
    List<String> puzzleIds) {

    public Room {
        exits = List.copyOf(exits);
        itemIds = List.copyOf(itemIds);
        logIds = List.copyOf(logIds);
        puzzleIds = List.copyOf(puzzleIds);
    }
}
