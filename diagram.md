```mermaid
%%{init: {"flowchart": {"defaultRenderer": "elk"}} }%%
graph TD;

IDLE --> |masterSwitchesOn| LV_ACTIVATION

LV_ACTIVATION --> |shutdownClosed && glvEnergized| TRACTIVE_ACTIVATION

TRACTIVE_ACTIVATION --> |brakePressed && driverButtonPressed| READY_TO_DRIVE

IDLE --> |batteryPercentage < 20| LOW_BATTERY

IDLE --> |20 <= batteryPercentage < 90 && pluggedIn| CONSTANT_CURRENT

IDLE --> |batteryPercentage > 90 && pluggedIn| CONSTANT_VOLTAGE

LOW_BATTERY --> |pluggedIn| PRE_CHARGING

PRE_CHARGING --> |batteryPercentage >= 20| CONSTANT_CURRENT

CONSTANT_CURRENT --> |batteryPercentage >= 90| CONSTANT_VOLTAGE

PRE_CHARGING --> |!pluggedIn| LOW_BATTERY

CONSTANT_CURRENT --> |!pluggedIn| IDLE

CONSTANT_VOLTAGE --> |!pluggedIn| IDLE

LOW_VOLTAGE_ONLY --> |!tractiveConnected| IDLE

FAULT