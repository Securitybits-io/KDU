
# KDU

Repo for the KDU Projects

![KDU](https://github.com/Securitybits-io/KDU/assets/25975089/e2ad2413-d2a0-4c04-8caf-76e5abb62b39)


## PRC-152

Baudrate: 19200

Colors:

- Red      -> +5v
- Black    -> GND
- Yellow   -> Rx
- Green    -> Tx

# Directories

### 152-traffic-decoder
A couple helper scripts for decoding the traffic between the radio an KDU

### ble

A future project where the goal was for Bluetooth control over the radio instead of a physical cable, with support to several radios.

### docs

Some documentation and datasheets

### KDU-Switcher

Some early code aimed at decoding and interacting with an Arduino UNO

### Phone

Contains some sub dirs:

#### KeyDisplayUnit

The actual Android App that controls the radio

#### PCB

Versions of the PCB that attaches onto the radio
The v1.0 is modeled to fit the original TRI 152 KDU Connector Housing

### Radio-Programmer

A small bonus python i whipped up that essentially takes a yaml formatted cfg file and a Serial port to the radio which automatically programs the radio for you.
