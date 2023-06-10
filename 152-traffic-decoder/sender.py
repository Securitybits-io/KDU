from pyrsistent import v
import serial

BAUD = 19200
PORT = '/dev/ttyUSB0'
ser = serial.Serial(PORT, BAUD)

data = bytearray()
start   = b'\xbb'
freqA   = b'\x48\x65\x6c\x69\x78\x50\x57\x4e\x20'
divider = b'\x20' # Divider Channel A
freqB   = b'\x46\x75\x63\x6b\x20\x59\x65\x61\x68'
config = [
    '00100000',     # Pos 1 Divider Channel B
    '00001110',     # Pos 2 Channel Mode 0x0>0x7F (1)
    '00000000',     # Pos 3 Channel Mode 0x0>0x7F (128)
    '00101001',     # Pos 4 Top Transmission Bar, Frequency Slot Select
    '00111101',     # Pos 5 Bottom Transmission Bar, Cycle reverse Frequency
    '00000001',     # Pos 6 Rx/Tx Symbol, Battery Status
    '00000100',     # Pos 7 Volume Control and Volume Control type
    '00100101',     # Pos 8 Mic Type, CT, R-CTC
    '00100110',     # Pos 9 ES, AM, PT
    '00010111',     # Pos 10 Frequency Shift, Radio Mode (TRF/RPT), Power
    '00011100',     # Pos 11 Squelch, Lock and Unlocked
    '00000001'      # Pos 12 Keypad Light
    ]
end = b'\x01'

data += start
data += freqA
data += divider
data += freqB

for binary in config:
    byte = int(binary,2)
    data.append(byte)

data += end

while(True):
    ser.write(data)