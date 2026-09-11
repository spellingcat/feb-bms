public class CANUtils {
    public double getPercentage() {
        return 0;
    }

    public boolean pluggedIn() {
        return false;
    }

    public boolean discharging() {
        return false;
    }

    public boolean voltageOutOfRange() {
        return false;
    }

    public boolean overcurrent() {
        return false;
    }

    public boolean overheating() {
        return false;
    }

    public boolean missedMeasurement() {
        return false;
    }

    public boolean fault() {
        // must monitor for:
        // Voltage values outside the permitted range  EV.7.4.2  
        // Voltage sense Overcurrent Protection device(s) blown or tripped 
        // Temperature values outside the permitted range  EV.7.5.2  
        // Missing or interrupted voltage or temperature measurements 
        // A fault in the BMS 
        return missedMeasurement() || overheating() || overcurrent() || voltageOutOfRange() || discharging() || pluggedIn();
    }

    public boolean brakePressed() {
        return false;
    }

    public boolean driverButtonPressed() {
        return false;
    }

    public boolean shutdownClosed() {
        return false;
    }

    public boolean glvEnergized() {
        return false;
    }
}
