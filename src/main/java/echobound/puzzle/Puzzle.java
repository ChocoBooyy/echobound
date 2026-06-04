package echobound.puzzle;

import echobound.Parser;
import java.util.List;

public sealed interface Puzzle permits CipherPuzzle, LogicPuzzle, NarrativePuzzle {

    String id();

    String room();

    String target();

    String prompt();

    String successLine();

    String solvedFlag();

    List<String> answers();

    default boolean accepts(String input) {
        String candidate = Parser.normalize(input);
        for (String answer : answers()) {
            if (Parser.normalize(answer).equals(candidate)) {
                return true;
            }
        }
        return false;
    }
}
