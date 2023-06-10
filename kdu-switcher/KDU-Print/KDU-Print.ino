#include <SoftwareSerial.h>
#define KDU_SER_TX 18
#define KDU_SER_RX 19
#define BAUDRATE 19200

// Simple function for arduino to print the messages from the radio

SoftwareSerial kdu(KDU_SER_RX, KDU_SER_TX);

byte message[32] = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

byte radio_1_bytes[]      = { 0x52, 0x61, 0x64, 0x69, 0x6f, 0x20, 0x31, 0x20, 0x20 }; //"Radio 1  "
byte radio_2_bytes[]      = { 0x52, 0x61, 0x64, 0x69, 0x6f, 0x20, 0x32, 0x20, 0x20 }; //"Radio 2  "
byte connecting[]   = { 0x53, 0x74, 0x61, 0x6e, 0x64, 0x62, 0x79, 0x2e, 0x2e }; //"Standby.."

byte start      = 0xBB; //Start of packet
byte freq_1[]   = { 0x31, 0x34, 0x35, 0x2e, 0x34, 0x32, 0x35, 0x30, 0x30 }; //145.42500
byte space      = 0x20;
byte freq_2[]   = { 0x34, 0x33, 0x33, 0x2e, 0x37, 0x30, 0x30, 0x30, 0x30 }; //433.70000
byte setting_1	= 0x00; // Frequency 1 Channel number
byte setting_2	= 0x00; // Frequency 2 Channel number
byte setting_3	= 0x04; // Channel Selector 0x04 = Channel 1, 0x05 = Channel 2
byte setting_4	= 0x3D; // Transmit power Bar
byte setting_5	= 0x55; // 0x50 > 0x56 Battery Bar
byte setting_6	= 0x03; // Electron(0x01), VULOS(0x02), Volume_control
byte setting_7	= 0x15; // MOI(0x15) Microphone Type
byte setting_8	= 0x22; // ES PT(0x22)
byte setting_9	= 0x16; // LOW(0x15), MED(0x16), High(0x17) Transmit Power
byte setting_10 = 0x15; // KEY(0x15), LOCK(0x16) Keyboard Lock
byte setting_11 = 0x01; // Keypad Light

//KDU Vars
byte kduBuf[4];

int switching_radio = 1;
int current_radio = 1;
int button_start;
unsigned long timeNow;

void create_message() {
  message[0]    = start;
  message[1]    = freq_1[0];
  message[2]    = freq_1[1];
  message[3]    = freq_1[2];
  message[4]    = freq_1[3];
  message[5]    = freq_1[4];
  message[6]    = freq_1[5];
  message[7]    = freq_1[6];
  message[8]    = freq_1[7];
  message[9]    = freq_1[8];
  message[10]   = space;
  message[11]   = freq_2[0];
  message[12]   = freq_2[1];
  message[13]   = freq_2[2];
  message[14]   = freq_2[3];
  message[15]   = freq_2[4];
  message[16]   = freq_2[5];
  message[17]   = freq_2[6];
  message[18]   = freq_2[7];
  message[19]   = freq_2[8];
  message[20]   = space;
  message[21]   = setting_1;
  message[22]   = setting_2;
  message[23]   = setting_3;
  message[24]   = setting_4;
  message[25]   = setting_5;
  message[26]   = setting_6;
  message[27]   = setting_7;
  message[28]   = setting_8;
  message[29]   = setting_9;
  message[30]   = setting_10;
  message[31]   = setting_11;
}

void printHex(uint8_t num) {
  char hexCar[2];

  sprintf(hexCar, "%02X", num);
  Serial.print(hexCar);
}

void display_radio_choice(){
  switch(current_radio){
    case 1:
      message[1]    = radio_2_bytes[0];
      message[2]    = radio_2_bytes[1];
      message[3]    = radio_2_bytes[2];
      message[4]    = radio_2_bytes[3];
      message[5]    = radio_2_bytes[4];
      message[6]    = radio_2_bytes[5];
      message[7]    = radio_2_bytes[6];
      message[8]    = radio_2_bytes[7];
      message[9]    = radio_2_bytes[8];
      message[11]   = connecting[0];
      message[12]   = connecting[1];
      message[13]   = connecting[2];
      message[14]   = connecting[3];
      message[15]   = connecting[4];
      message[16]   = connecting[5];
      message[17]   = connecting[6];
      message[18]   = connecting[7];
      message[19]   = connecting[8];

      timeNow = millis();
      while (millis() - timeNow < 1000){
        kdu.write(message, sizeof(message));
      }
      break;
    case 2:
      message[1]    = radio_1_bytes[0];
      message[2]    = radio_1_bytes[1];
      message[3]    = radio_1_bytes[2];
      message[4]    = radio_1_bytes[3];
      message[5]    = radio_1_bytes[4];
      message[6]    = radio_1_bytes[5];
      message[7]    = radio_1_bytes[6];
      message[8]    = radio_1_bytes[7];
      message[9]    = radio_1_bytes[8];
      message[11]   = connecting[0];
      message[12]   = connecting[1];
      message[13]   = connecting[2];
      message[14]   = connecting[3];
      message[15]   = connecting[4];
      message[16]   = connecting[5];
      message[17]   = connecting[6];
      message[18]   = connecting[7];
      message[19]   = connecting[8];

      timeNow = millis();
      while (millis() - timeNow < 1000){
        kdu.write(message, sizeof(message));
      }
      break;
  }
  create_message(); //TODO: Remove after proper signal from Radio

}

void switch_radio() { 
  switch(current_radio){
    case 1:
      Serial.println("Selecting Radio 2");
      display_radio_choice();
      current_radio = 2;
      break;      
    case 2:
      Serial.println("Selecting Radio 1");
      display_radio_choice();
      current_radio = 1;
      break;
  }
}

void setup() {
  create_message();
  kdu.begin(BAUDRATE);
  
  Serial.begin(9600);
}

void loop() {
  kdu.write(message, sizeof(message));
  kdu.readBytes(kduBuf, 4);

  if (kduBuf[0] == 0x01 && switching_radio == 0) {
    switch_radio();
    switching_radio = 1;
    for(int i=0; i<sizeof(kduBuf); i++){
      printHex(kduBuf[i]);
    }

    Serial.println();
    delay(500);
  } else if (kduBuf[0] == 0x00) {
    switching_radio = 0;
  }
  delay(50);
}
