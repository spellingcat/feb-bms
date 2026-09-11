import java.util.function.BooleanSupplier;
public class StateMachine {
    public enum State {
        IDLE,
        
        //activation sequence
        LV_ACTIVATION,
        TRACTIVE_ACTIVATION,
        READY_TO_DRIVE,

        //https://www.grepow.com/blog/what-are-the-3-stages-of-lithium-battery-charging.html
        PRE_CHARGING, // 0-??% 20?
        CONSTANT_CURRENT, // ??-90% 
        CONSTANT_VOLTAGE, // 90-100%

        DISCHARGING,
        LOW_BATTERY, // ??
        LOW_VOLTAGE, // EV.4.4.1.b

        SHUTDOWN,
        FAULT

        ;

        public final BooleanSupplier trigger;

        private State() {
            trigger = (() -> state == this);
        }

        public BooleanSupplier getTrigger() {
            return trigger;
        }
    }
    
    private static State state;
    private CANUtils canUtils;
    // private State prevState;

    public StateMachine() {
        canUtils = new CANUtils();
        addTransitions();
        addCommands();
    }

    private void addTransitions() {
        // normal charging sequence
        bindTransition(State.LOW_BATTERY, State.PRE_CHARGING, () -> canUtils.pluggedIn());
        bindTransition(State.PRE_CHARGING, State.CONSTANT_CURRENT, () -> (canUtils.getPercentage() > 20)); // add some debounce to filter noise
        bindTransition(State.CONSTANT_CURRENT, State.CONSTANT_VOLTAGE, () -> (canUtils.getPercentage() > 80));
        
        // exit charging early
        bindTransition(State.PRE_CHARGING, State.LOW_BATTERY, () -> !canUtils.pluggedIn());
        bindTransition(State.CONSTANT_CURRENT, State.IDLE, () -> !canUtils.pluggedIn());
        bindTransition(State.CONSTANT_VOLTAGE, State.IDLE, () -> !canUtils.pluggedIn());

        bindTransition(State.IDLE, State.DISCHARGING, () -> canUtils.discharging());

        bindTransition(State.LV_ACTIVATION, State.TRACTIVE_ACTIVATION, () -> canUtils.shutdownClosed() && canUtils.glvEnergized());
        bindTransition(State.TRACTIVE_ACTIVATION, State.READY_TO_DRIVE, () -> canUtils.brakePressed() && canUtils.driverButtonPressed());

        bindTransition(State.FAULT, () -> canUtils.fault());
    }

    private void addCommands() {
        bindCommands(State.IDLE);
        bindCommands(State.LV_ACTIVATION);
        bindCommands(State.TRACTIVE_ACTIVATION);
        bindCommands(State.READY_TO_DRIVE, () -> makeSound());

        bindCommands(State.PRE_CHARGING); // 0-??% 20?
        bindCommands(State.CONSTANT_CURRENT); // ??-90% 
        bindCommands(State.CONSTANT_VOLTAGE); // 90-100%

        bindCommands(State.DISCHARGING);
        bindCommands(State.LOW_BATTERY); // ??
        bindCommands(State.LOW_VOLTAGE); // EV.4.4.1.b
        bindCommands(State.SHUTDOWN);
        bindCommands(State.FAULT, () -> openShutdownCircuit(), () -> enableIndicatorLights());
    }

    private void makeSound() {}

    private void openShutdownCircuit() {}

    private void enableIndicatorLights() {}

	private void bindTransition(State start, State end, BooleanSupplier transition) {
        if (transition.getAsBoolean() && start.getTrigger().getAsBoolean()) changeStateTo(end);
    }

    private void bindTransition(State end, BooleanSupplier transition) {
        if (transition.getAsBoolean()) changeStateTo(end);

    }

    private void changeStateTo(State nextState) {
        // this.prevState = state;
        state = nextState;
    }

    private void bindCommands(State state, Runnable... functions) {
        // idk how the event loop works but 
        if (state.getTrigger().getAsBoolean()) {
            for (Runnable f : functions) {
                Thread t = new Thread(f);
                t.start();
            }
        }
    }

}