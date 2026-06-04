package echobound;

import echobound.config.Config;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public final class Terminal {

    public enum Voice { ROOM, LOG, WARD, ERROR, SYSTEM }

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_WHITE = "\u001B[37m";
    private static final String ANSI_BRIGHT_WHITE = "\u001B[97m";
    private static final String ANSI_DIM_YELLOW = "\u001B[2;33m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_RED = "\u001B[31m";

    private static final String PREFIX_NONE = "";
    private static final String PREFIX_LOG = "[LOG] ";
    private static final String PREFIX_WARD = "WARD > ";
    private static final String PREFIX_ERROR = "[ERR] ";
    private static final String PREFIX_SYSTEM = ">> ";

    private static final String PROMPT = "> ";
    private static final char CARRIAGE_RETURN = '\r';
    private static final char NEWLINE = '\n';

    private final PrintStream out;
    private final InputStream in;
    private final int delayMs;

    public Terminal(Config config) {
        this.out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        this.in = System.in;
        this.delayMs = config.typewriterDelayMs();
    }

    public void print(Voice voice, String text) {
        out.print(colorFor(voice));
        typewrite(prefixFor(voice) + text);
        out.print(ANSI_RESET);
        out.println();
    }

    public void blank() {
        out.println();
    }

    public void prompt() {
        out.print(ANSI_BRIGHT_WHITE + PROMPT + ANSI_RESET);
        out.flush();
    }

    public String readLine() {
        StringBuilder builder = new StringBuilder();
        try {
            int read = in.read();
            if (read == -1) {
                return null;
            }
            while (read != -1) {
                char c = (char) read;
                if (c == NEWLINE) {
                    break;
                }
                if (c != CARRIAGE_RETURN) {
                    builder.append(c);
                }
                read = in.read();
            }
        } catch (IOException e) {
            return null;
        }
        return builder.toString();
    }

    private void typewrite(String text) {
        boolean skip = false;
        for (int i = 0; i < text.length(); i++) {
            out.print(text.charAt(i));
            out.flush();
            if (!skip && skipRequested()) {
                skip = true;
            }
            if (!skip && delayMs > 0) {
                sleep();
            }
        }
        if (skip) {
            drainPending();
        }
    }

    private boolean skipRequested() {
        try {
            return in.available() > 0;
        } catch (IOException e) {
            return false;
        }
    }

    private void drainPending() {
        try {
            while (in.available() > 0) {
                in.read();
            }
        } catch (IOException e) {
            return;
        }
    }

    private void sleep() {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String colorFor(Voice voice) {
        return switch (voice) {
            case ROOM -> ANSI_WHITE;
            case LOG -> ANSI_DIM_YELLOW;
            case WARD -> ANSI_CYAN;
            case ERROR -> ANSI_RED;
            case SYSTEM -> ANSI_BRIGHT_WHITE;
        };
    }

    private String prefixFor(Voice voice) {
        return switch (voice) {
            case ROOM -> PREFIX_NONE;
            case LOG -> PREFIX_LOG;
            case WARD -> PREFIX_WARD;
            case ERROR -> PREFIX_ERROR;
            case SYSTEM -> PREFIX_SYSTEM;
        };
    }
}
