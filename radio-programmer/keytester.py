import serial
from time import sleep
BAUD = 19200
PORT = '/dev/ttyUSB0'
ser = serial.Serial(PORT, BAUD)

data = bytearray()
for i in range(13,16):
    ser.write([0xAA, 0x00, 0x10, 0x00])
    sleep(0.3)
    print(i, hex(i))
    data = [0xAA, 0x00, 0xd, 0x00]
    ser.write(data)
    sleep(0.3)