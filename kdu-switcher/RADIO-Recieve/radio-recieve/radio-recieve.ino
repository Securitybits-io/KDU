#include <SoftwareSerial.h>

#define RADIO_SER_TX 16
#define RADIO_SER_RX 17
#define KDU_SER_TX 18
#define KDU_SER_RX 19
#define BAUDRATE 19200

SoftwareSerial radio(RADIO_SER_RX, RADIO_SER_TX);
SoftwareSerial kdu(KDU_SER_RX, KDU_SER_TX);

byte radioBuf[31] = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

void printHex(uint8_t num) {
  char hexCar[2];
  sprintf(hexCar, "%02X", num);
  Serial.print(hexCar);
}

void setup() {
  radio.begin(BAUDRATE);
  //kdu.begin(BAUDRATE);
  Serial.begin(9600);
}

void loop() {
  //radio.listen();
  if (radio.read() == 0xBB){
    radio.readBytes(radioBuf, 31);
    for(int i=0; i<sizeof(radioBuf); i++){
      printHex(radioBuf[i]);
    }
    Serial.println();
  }
  
  /*if (radio.read() == 0xBB){
    for(int i = 1; i<32; i++){
      radioBuf[i] = radio.read();
    }
    
    for(int i=0; i<sizeof(radioBuf); i++){
        printHex(radioBuf[i]);
      }
    Serial.println();
    
    if (radioBuf[10] == 0x20 && radioBuf[20] == 0x20 ) {
      Serial.println("Writing KDU");
      radio.stopListening();
      kdu.listen();
      kdu.write(radioBuf, sizeof(radioBuf));
      kdu.stopListening();      
    }
    
  }*/
}
