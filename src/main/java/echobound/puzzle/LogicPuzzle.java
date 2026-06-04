package echobound.puzzle;

import echobound.Parser;
import java.util.List;

public record LogicPuzzle(
    String id,
    String room,
    String target,
    String prompt,
    String successLine,
    String solvedFlag,
    List<String> answers) implements Puzzle {

    public LogicPuzzle {
        answers = List.copyOf(answers);
    }

    @Override
    public boolean accepts(String input) {
        String candidate = canonical(input);
        for (String answer : answers) {
            if (canonical(answer).equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static String canonical(String value) {
        return Parser.normalize(value.replace("(", " ").replace(")", " "));
    }
}
