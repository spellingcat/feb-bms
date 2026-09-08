import java.util.function.BooleanSupplier;

public class StateMachine {
    public enum State {
        //https://www.grepow.com/blog/what-are-the-3-stages-of-lithium-battery-charging.html
        PRE_CHARGING, // 0-??% 20?
        CONSTANT_CURRENT, // ??-80% 
        CONSTANT_VOLTAGE, // 80-100%
        IDLE,
        DISCHARGING,
        LOW_BATTERY; // ??

        public final BooleanSupplier trigger;

        private State() {
            trigger = (() -> state == this);
        }

        public BooleanSupplier getTrigger() {
            return trigger;
        }
    }
    
    private static State state;
    // private State prevState;

    public StateMachine() {
        addTransitions();
    }

    private void addTransitions() {
        // normal charging sequence
        bindTransition(State.LOW_BATTERY, State.PRE_CHARGING, () -> pluggedIn());
        bindTransition(State.PRE_CHARGING, State.CONSTANT_CURRENT, () -> (getPercentage() > 20)); // add some debounce to filter noise
        bindTransition(State.CONSTANT_CURRENT, State.CONSTANT_VOLTAGE, () -> (getPercentage() > 80));
        
        // exit charging early
        bindTransition(State.PRE_CHARGING, State.LOW_BATTERY, () -> !pluggedIn());
        bindTransition(State.CONSTANT_CURRENT, State.IDLE, () -> !pluggedIn());
        bindTransition(State.CONSTANT_VOLTAGE, State.IDLE, () -> !pluggedIn());

        bindTransition(State.IDLE, State.DISCHARGING, () -> discharging());
    }

    private void bindTransition(State start, State end, BooleanSupplier transition) {
        if (transition.getAsBoolean() && start.getTrigger().getAsBoolean()) changeStateTo(end);
    }

    private void changeStateTo(State nextState) {
        // this.prevState = state;
        state = nextState;
    }

    private double getPercentage() {
        // presumably this comes from CAN???
        return 0;
    }

    private boolean pluggedIn() {
        // presumably this comes from CAN???
        return false;
    }

    private boolean discharging() {
        // presumably this comes from CAN???
        return false;
    }
}