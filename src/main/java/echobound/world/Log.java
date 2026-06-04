package echobound.world;

public record Log(
    String id,
    String title,
    String content,
    String corruptedContent,
    String repairPuzzle,
    String room) {

    public boolean isCorrupted() {
        return !repairPuzzle.isEmpty();
    }
}
