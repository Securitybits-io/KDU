from pyrsistent import v
import serial
import binascii

BAUD = 19200
PORT = '/dev/ttyUSB0'
thing = ""
ser = serial.Serial(PORT, BAUD)
while ord(ser.read()) != int(0xbb):
    thing = ser.read(32)

getbinary = lambda x, n: format(x, 'b').zfill(n)

print('Start Byte: {}'.format(hex(thing[0])))
print('Channel 1: {}'.format(thing[1:10]))
print('Chan Split: {}'.format(hex(thing[10])))
print('Channel 2: {}'.format(thing[11:20]))
print('Unknown: {}'.format(hex(thing[20])))
print('Channel 1 Mode: {}'.format(hex(thing[21]))) #0x0: freq mode, 0x1>128:Selected Channel for #1
print('Channel 2 Mode: {}'.format(hex(thing[22]))) #0x0: freq mode, 0x1>128:Selected Channel for #2

# 0000 0000 - Pos 4
#         ^- Memoryslot 0 - Top, 1 - Bottom #MAYBE
print('Unknown: {}'.format(getbinary(thing[23], 8)))

# 0000 0000 - Pos 5
#
print('Unknown: {}'.format(getbinary(thing[24],8)))

# 0000 0000 - Pos 6
#        ^^- 10 - Rx, 01 - Tx(Maybe)
print('Rx/Tx: {}'.format(getbinary(thing[25],8)))

# 0000 0000 - Pos 7
#        ^^- 01 - Electron, 10 - VULOS/Potentio
# ^^^^ ^^  - Volume control
print('Volume control: {}'.format(getbinary(thing[26],8)))

# 0000 0000 - Pos 8
#  ^^- ---- - 10 - Dyn, 01 - CAP
print('CAP/Dyn Mic: {}'.format(getbinary(thing[27],8)))

# 0000 0000 - Pos 9
print('Unknown: {}'.format(hex(thing[28])))

# Power 
# 0000 0000 - Pos 10
#        ^^- 01 - Power Low, 10 - Power Middle, 11 - High
print('Power: {}'.format(getbinary(thing[29], 8))) #0x15 - Low, 0x16 - Middle, 0x17 - High

# Squelch and Lock is goverend by Binary data
# 0000 0000 - Pos 11
#        ^^- 01: Unlocked, 10: locked
# ^^^^ ^^  - Squelch Level
print('Squelch and Lock: {}'.format(getbinary(thing[30],8))) #0x05 - SQL0, 0x09 - SQL1, 0x0D - SQL2 -- This is also LOCK

# 0000 0000 - Pos 12
#
print('Unknown: {}'.format(hex(thing[31])))

for i in thing[20:]:
    print(hex(i), end='')
    print(' ', end='')
print()