package echobound.puzzle;

import java.util.List;

public record CipherPuzzle(
    String id,
    String room,
    String target,
    String prompt,
    String successLine,
    String solvedFlag,
    String cipherText,
    String key,
    List<String> answers) implements Puzzle {

    public CipherPuzzle {
        answers = List.copyOf(answers);
    }
}
