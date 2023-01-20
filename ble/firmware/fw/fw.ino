#define RXD2  16
#define TXD2  17

const byte rawMessageLength = 128;
byte rawMessage[rawMessageLength];

const byte parsedMessageLength = 32;
byte message[parsedMessageLength];


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
      
      Serial.print("Found message at pos: ");
      Serial.println(rawPos);
      
      //Carve out the message from rawMessage
      for (int pos = 0; pos < 32; pos++){
        message[pos] = rawMessage[rawPos+pos];
      }
    }
  }
  printMessage();
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

  Serial2.begin(19200, SERIAL_8N1, RXD2, TXD2);
}


void loop(){
  recieveMessage();
}
