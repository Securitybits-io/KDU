#define RXD2  16
#define TXD2  17

#include "BluetoothSerial.h"

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif

#if !defined(CONFIG_BT_SPP_ENABLED)
#error Serial Bluetooth not available or not enabled. It is only available for the ESP32 chip.
#endif

const byte rawMessageLength = 128;
byte rawMessage[rawMessageLength];

const byte parsedMessageLength = 32;
byte message[parsedMessageLength];

const byte CHANNEL_SWITCH[4] = {0xAA, 0x00, 0x21, 0x00};

BluetoothSerial SerialBT;

void recieveMessage(){
  static int ndx = 0;
  
  while (Serial2.available() && ndx < sizeof(rawMessage)){
    char recievedChar;
    recievedChar = Serial2.read();    
    rawMessage[ndx] = recievedChar;
    ndx++;
  }
  
  ndx = 0;
  parseMessage();
  
  return;
}


void parseMessage(){
  delay(100);   //In order to make the buffer catch up and fill properly
  for (int rawPos = 0; rawPos < sizeof(rawMessage); rawPos++){
    if (int(rawMessage[rawPos]) == 187 && (rawPos+32) < sizeof(rawMessage)){
      /*
      Serial.print("Found message at pos: ");
      Serial.println(rawPos);
      */
      //Carve out the message from rawMessage
      for (int pos = 0; pos < 32; pos++){
        message[pos] = rawMessage[rawPos+pos];
      }
    }
  }
  //printMessage();
}


void printMessage(){ 
  for (int pPos = 0; pPos < sizeof(message); pPos++){
    Serial.print(char(message[pPos]));
  }
  Serial.println();
}



void setup(){
  Serial.begin(115200);
  Serial.println("Setting up the device");
  SerialBT.begin("TRI-152 KDU Radio");
  Serial2.begin(19200, SERIAL_8N1, RXD2, TXD2);
  Serial.println("Device configured, pair it with a bluetooth unit");
}


void loop(){
  recieveMessage();
  //SerialBT.write(message[1]);
  //SerialBT.write(13);
  if (SerialBT.available()) {
    if (SerialBT.read() == 99){
      Serial2.write(CHANNEL_SWITCH, 4);
    }
  }
}
