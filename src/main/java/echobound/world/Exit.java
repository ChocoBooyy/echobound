package echobound.world;

import java.util.Set;

public record Exit(String direction, String targetRoomId, String requiredFlag, String lockedMessage) {

    public boolean isOpen(Set<String> flags) {
        return requiredFlag.isEmpty() || flags.contains(requiredFlag);
    }
}
