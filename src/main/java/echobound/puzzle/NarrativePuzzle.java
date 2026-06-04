package echobound.puzzle;

import java.util.List;

public record NarrativePuzzle(
    String id,
    String room,
    String target,
    String prompt,
    String successLine,
    String solvedFlag,
    List<String> answers,
    List<String> requiredLogs) implements Puzzle {

    public NarrativePuzzle {
        answers = List.copyOf(answers);
        requiredLogs = List.copyOf(requiredLogs);
    }
}
