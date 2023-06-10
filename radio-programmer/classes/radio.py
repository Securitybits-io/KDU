import serial
import yaml
import logging
import binascii

from time import sleep

BAUD = 19200
TIMING = 0.3
SHORT_TIMING = 0.2

KEYS = {
    "switch":   [0xAA, 0x00, 0x21, 0x00],
    "ptt":      [0xAA, 0x01, 0x00, 0x00],
    "volup":    [0xAA, 0x00, 0x22, 0x00],
    "voldown":  [0xAA, 0x00, 0x23, 0x00],
    "0":        [0xAA, 0x00, 0x0A, 0x00],
    "1":        [0xAA, 0x00, 0x01, 0x00],
    "2":        [0xAA, 0x00, 0x02, 0x00],
    "3":        [0xAA, 0x00, 0x03, 0x00],
    "4":        [0xAA, 0x00, 0x04, 0x00],
    "5":        [0xAA, 0x00, 0x05, 0x00],
    "6":        [0xAA, 0x00, 0x06, 0x00],
    "7":        [0xAA, 0x00, 0x07, 0x00],
    "8":        [0xAA, 0x00, 0x08, 0x00],
    "9":        [0xAA, 0x00, 0x09, 0x00],
    "clr":      [0xAA, 0x00, 0x10, 0x00],
    "ent":      [0xAA, 0x00, 0x0F, 0x00],
    "up":       [0xAA, 0x00, 0x31, 0x00],
    "down":     [0xAA, 0x00, 0x32, 0x00],
    "left":     [0xAA, 0x00, 0x0d, 0x00],
    "right":    [0xAA, 0x00, 0x0e, 0x00]
}


MAIN_MENU = {
    "R-CTC": 1,
    "R-DCSN": 2,
    "R-DCSI": 3,
    "R-MOD": 4,
    "T-CTC": 5,
    "T-DCSN": 6,
    "T-DCSI": 7,
    "T-DTM1": 8,
    "T-DTM2": 9,
    "POWER": 10,
    "W/NA": 11,
    "COMP": 12,
    "SRMR": 13,
    "SFT": 14,
    "OFFSET": 15,
    "STEP": 16,
    "CH-MEM": 17,
    "CH-DEL": 18,
    "LED-SW": 19,
    "BEEP": 20,
    "RING": 21,
    "BCL": 22,
    "TOT": 23,
    "TONE": 24,
    "DTM-TM": 25,
    "SQL": 26,
    "RPT": 27,
    "DTMF": 28,
    "ANI_ID": 29,
    "VOL-SW": 30,
    "SPEACKER": 31,
    "MIC_TYPE": 32,
    "RESET": 33
}


CHAN_MEM_CHARS = {
    "0": 0,
    "1": 1,
    "2": 2,
    "3": 3,
    "4": 4,
    "5": 5,
    "6": 6,
    "7": 7,
    "8": 8,
    "9": 9,
    "A": 10,
    "B": 11,
    "C": 12,
    "D": 13,
    "E": 14,
    "F": 15,
    "G": 16,
    "H": 17,
    "I": 18,
    "J": 19,
    "K": 20,
    "L": 21,
    "M": 22,
    "N": 23,
    "O": 24,
    "P": 25,
    "Q": 26,
    "R": 27,
    "S": 28,
    "T": 29,
    "U": 30,
    "V": 31,
    "W": 32,
    "X": 33,
    "Y": 34,
    "Z": 35,
    "*": 36,
    "#": 37,
    "-": 38,
    "_": 39
}


class radio:
    def __init__(self, eud, conf):
        self.eud = eud
        self.config_file = conf
        self.config = self.parse_config()
        return


    def connect(self):
        global ser
        print("[+] Connecting to radio")
        ser = serial.Serial(self.eud, BAUD)
        print("[+] Successful Connection to TRI 152")
        return


    def parse_config(self):
        with open(self.config_file) as file:
            conf = yaml.load(file, Loader=yaml.FullLoader)
        return conf


    def keypress(self, key):
        ser.write(KEYS[key])
        sleep(TIMING)
        return


    def getData(self):
        ser.flushInput()
        data = ser.read(64)
        raw = data[data.index(187):data.index(187)+32]
        if (len(raw) == 32):
            return raw
        return False


    def getChannelData(self):
        data = self.getData()
        if self.getChanSlot(data) == "A":
            return data[1:10]
        if self.getChanSlot(data) == "B":
            return data[11:20]
        return False


    def setFreq(self, freq):
        frequency = freq.replace(".","").ljust(8,"0")
        print("setting frequency "+freq)
        for c in frequency:
            self.keypress(c)
        return


    def getBit(self, data, bit):
        return (data & (1<<bit))


    def getChanSlot(self, data):
        if self.getBit(data[23], 0) == 0:
            return "A"
        elif self.getBit(data[23], 0) == 1: 
            return "B"
        return False


    def setChanSlot(self, slot):
        if self.getChanSlot(self.getData()) != slot:
            self.keypress("switch")
            return True
        return False


    def getChanMemSlot(self):
        data = self.getData()
        if (self.getChanSlot(data) == "A"):
            return data[21]
        if (self.getChanSlot(data) == "B"):
            return data[22]
        return False


    def set_option(self, entry, option):
        val = option
        if option == False:
            val = "OFF"
        elif option == True:
            val = "ON"

        print("Setting: {} to {}".format(entry, val))
        self.find_menu(entry)
        self.keypress("ent")
        while self.getChannelData().decode().strip() != val:
            self.keypress("up")
        self.keypress("ent")
        return


    def storeFrequency(self, pos, name, freq):
        self.keypress("clr")
        self.keypress("clr")

        # make sure to be in frequency mode
        while (self.getChanMemSlot() != 0 or self.getChannelData().decode().strip() == "NO-CCH"):
            self.keypress("ent")
            sleep(SHORT_TIMING)
            self.keypress("1")
            sleep(SHORT_TIMING)

        # sanitize and input frequency
        frequency = str(freq).replace(".","").ljust(8,"0")
        for number in frequency:
            self.keypress(number)

        # move menu to CH-MEM
        self.find_menu("CH-MEM")
        self.keypress("ent")

        # Move to Mem Slot
        wanted_slot = f"CH-{str(pos).zfill(3)}"
        
        while self.getChannelData().decode().strip() != wanted_slot:
            if pos <= 64:
                self.keypress("up")
            if pos >= 65:
                self.keypress("down")
        self.keypress("ent") #memorybank active
        
        # Enter the corresponding characters for the name
        # up/pre+ for a new character
        # (0) left/right (_) for selected character
        for char in str(name)[0:8]:
            self.keypress("up")
            if CHAN_MEM_CHARS[char] <= 20:    
                for i in range(0, CHAN_MEM_CHARS[char]):
                    self.keypress("left")
            if CHAN_MEM_CHARS[char] >= 21:
                for i in range(40, CHAN_MEM_CHARS[char], -1):
                    self.keypress("right")
                
            
        # Enter and exit to freq mode
        self.keypress("ent")
        return
    
    def find_menu(self, entry):
        for i in range(0,3):
            self.keypress("clr")
        self.keypress("ent")
        self.keypress("ent")
        while self.getChannelData().decode().strip() != entry.upper():
            menu_pos = self.getChannelData().decode().strip()
            current_pos = MAIN_MENU[menu_pos]
            wanted_pos = MAIN_MENU[entry.upper()]
            steps_up = 0
            steps_down = 0
            if current_pos < wanted_pos:
                steps_up = wanted_pos - current_pos
                steps_down = (len(MAIN_MENU) - wanted_pos) + current_pos
            if current_pos > wanted_pos:
                steps_up = (len(MAIN_MENU) - current_pos) + wanted_pos
                steps_down = current_pos - wanted_pos

            if steps_up > steps_down:
                for i in range(0, abs(steps_down)):
                    self.keypress("down")
            else:
                for i in range(0, abs(steps_up)):
                    self.keypress("up")


    def memSlotProgram(self, slot, config):
        
        print(f"[+] Programming Memory Bank {slot}")
        self.setChanSlot(slot)

        for setting, value in config.items():
            if setting == "Channels":
                continue
            self.set_option(setting, value)
  
        channels = config['Channels']
        
        print(f"[+] Programming Stored Frequencies for bank {slot}")
        for slot, value in channels.items():
            print(f"Slot: CH-{str(value['memslot']).zfill(3)} Freq: { str(value['freq']).ljust(9,'0') } Name: {value['name']}")
            self.storeFrequency(value['memslot'], value['name'], value['freq'] )
        return


    def program(self):
        if 'Global' in self.config:
            print("[+] Global Settings")
            for setting, value in self.config['Global'].items():
                self.set_option(setting, value)
            self.keypress("clr")
        
        if 'memslot1' in self.config:
            self.memSlotProgram("A", self.config['memslot1'])
        
        if 'memslot2' in self.config:
            self.memSlotProgram("B", self.config['memslot2'])

        print("[+] Programming Done, setting some defaults")
        self.setChanSlot("A")
    

