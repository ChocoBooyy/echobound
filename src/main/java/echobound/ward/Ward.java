package echobound.ward;

import echobound.GameState;
import java.util.List;
import java.util.Optional;

public final class Ward {

    private final List<WardResponse> responses;

    public Ward(List<WardResponse> responses) {
        this.responses = List.copyOf(responses);
    }

    public Optional<String> reactTo(String trigger, GameState state) {
        for (WardResponse response : responses) {
            if (response.trigger().equals(trigger)) {
                if (!state.hasFiredTrigger(trigger)) {
                    state.markTriggerFired(trigger);
                    state.adjustTrust(response.trustDelta());
                }
                return Optional.of(response.line());
            }
        }
        return Optional.empty();
    }
}
