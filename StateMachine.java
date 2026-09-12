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

        LOW_BATTERY, // ??
        LOW_VOLTAGE_ONLY, // EV.4.4.1.b

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
    
    private static State state = State.IDLE;
    private CANUtils canUtils;
    // private State prevState;

    public StateMachine() {
        canUtils = new CANUtils();
        addTransitions();
        addCommands();
    }

    private void addTransitions() {
        // activation sequence
        bindTransition(State.IDLE, State.LV_ACTIVATION, () -> canUtils.masterSwitchesOn());
        bindTransition(State.LV_ACTIVATION, State.TRACTIVE_ACTIVATION, () -> canUtils.shutdownClosed() && canUtils.glvEnergized());
        //TODO add way to bail out of tractive and LV activation
        bindTransition(State.TRACTIVE_ACTIVATION, State.READY_TO_DRIVE, () -> canUtils.brakePressed() && canUtils.driverButtonPressed());
        // i don't know how to get out of ready to drive

        bindTransition(State.IDLE, State.LOW_BATTERY, () -> canUtils.getPercentage() < 20);
        
        // no IDLE -> PRE_CHARGING because it'll go to LOW_BATTERY first
        bindTransition(State.IDLE, State.CONSTANT_CURRENT, () -> canUtils.getPercentage() >= 20 && canUtils.getPercentage() < 90 && canUtils.pluggedIn());
        bindTransition(State.IDLE, State.CONSTANT_VOLTAGE, () -> canUtils.getPercentage() >= 90 && canUtils.pluggedIn());

        // full charging sequence
        bindTransition(State.LOW_BATTERY, State.PRE_CHARGING, () -> canUtils.pluggedIn());
        bindTransition(State.PRE_CHARGING, State.CONSTANT_CURRENT, () -> (canUtils.getPercentage() >= 20)); // add some debounce to filter noise
        bindTransition(State.CONSTANT_CURRENT, State.CONSTANT_VOLTAGE, () -> (canUtils.getPercentage() >= 90));
        
        // exit charging early
        bindTransition(State.PRE_CHARGING, State.LOW_BATTERY, () -> !canUtils.pluggedIn());
        bindTransition(State.CONSTANT_CURRENT, State.IDLE, () -> !canUtils.pluggedIn());
        bindTransition(State.CONSTANT_VOLTAGE, State.IDLE, () -> !canUtils.pluggedIn());

        bindTransition(State.FAULT, () -> canUtils.fault()); // i think this state is "terminal"- says you need to reset everything to get out of this state

        bindTransition(State.LOW_VOLTAGE_ONLY, () -> !canUtils.tractiveDisconnected()); // i don't really know how this state works
        bindTransition(State.LOW_VOLTAGE_ONLY, State.IDLE, () -> canUtils.tractiveDisconnected());
    }

    private void addCommands() {
        // i'm not super sure of all the things that need to happen during these states but I've listed what I can find from the rulebook
        bindCommands(State.IDLE);

        bindCommands(State.LV_ACTIVATION, () -> activateLV());
        bindCommands(State.TRACTIVE_ACTIVATION, () -> activateTractive());
        bindCommands(State.READY_TO_DRIVE, () -> makeSound(), () -> drive()); // should be reading and responding to driver inputs like steering, pedals. Also has to make a sound

        bindCommands(State.PRE_CHARGING, () -> preCharge()); // first phase of charging from 0-20%
        bindCommands(State.CONSTANT_CURRENT, () -> chargeConstantCurrent()); // second phase of charging from 20-90%
        bindCommands(State.CONSTANT_VOLTAGE, () -> chargeConstantVoltage()); // final phase of charging from 90-100%. Closes second IR

        bindCommands(State.LOW_BATTERY); // battery is below 20% (or some threshold) and is not plugged in- car should not be running as to not damage the battery
        bindCommands(State.LOW_VOLTAGE_ONLY); // EV.4.4.1.b - when tractive battery is removed. I imagine it would just not call anything that requires it

        bindCommands(State.FAULT, () -> openShutdownCircuit(), () -> enableIndicatorLights());
    }

    private void preCharge() {}

    private void chargeConstantCurrent() {}

    private void chargeConstantVoltage() {}

    private void activateLV() {}

    private void activateTractive() {}

    private void drive() {}

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