#include "dbele_motor.h"
#include <SoftwareSerial.h>
SoftwareSerial BTserial(12, 13); // RX | TX

const char FORWARD = 'F';
const char BACKWARD = 'B';
const char LEFT = 'L';
const char RIGHT = 'R';
const char NEUTRAL = 'N';

char determine_direction(String directions) {
    int index = directions.indexOf(":");
    //acer
    //String up_down = directions.substring(0, index);
    String left_right = directions.substring(0, index);
    //acer
    //String left_right = directions.substring(index+1);
    String up_down = directions.substring(index+1);
    double u_d = atof(up_down.c_str());
    double l_r = atof(left_right.c_str());

    if (u_d > 3) {
      return FORWARD;
    } else if (u_d < -3) {
      return BACKWARD;
    } else if (l_r < -3) {//else if (l_r > 3) {
      return RIGHT;
    } else if (l_r > 3) {//else if (l_r < -3){
      return LEFT;
    } else {
      return NEUTRAL;
    }
    
}

String report = "";

// Motor A
int enA = 9;
int in1 = 8;
int in2 = 7;

// Motor B
int enB = 3;
int in3 = 5;
int in4 = 4;

int trig = 10;
int echo = 11;

DB_Motor m(enA, in1, in2, enB, in3, in4);

void setup_proximity_sensor() {
    pinMode(trig, OUTPUT);
    pinMode(echo, INPUT);
}

void setup() {

  setup_proximity_sensor();

  Serial.begin(9600);
  BTserial.begin(9600);
}

const unsigned long trg_high_time = 500;
unsigned long trg_time;
const byte STATE_IDLE = 1;
const byte STATE_UP = 2;
int current_state=STATE_IDLE;

void handle_proximity_sensor() { 
  switch (current_state) {
    case STATE_IDLE:
        digitalWrite(trig, HIGH);
        trg_time = millis();
        current_state = STATE_UP;
      break;
      
    case STATE_UP:
      if (millis() - trg_time >= trg_high_time) {
        digitalWrite(trig, LOW);
        current_state = STATE_IDLE;
        sendDistance();
      }
      break;
  }
}

int duration;
int distance;

void sendDistance() {
    duration = pulseIn(echo, HIGH);
    distance = (duration/2) / 29.1; // into cm
    writeBluetooth();
}

void writeBluetooth() {
    //Serial.print(distance);
    //Serial.print("cm\n");

    BTserial.print(distance);
    //BTserial.print("cm");
    BTserial.print("\n");
}

int MIN_ALLOWED_DISTANCE = 45;

void handle_bluetooth() {
    if (BTserial.available()) {  
      int data = BTserial.read();

      if ((char)data == '\n') {
          //Serial.println(report);
          char dir = determine_direction(report);
          Serial.println(dir);
          switch(dir){
            case FORWARD:
                if (distance > MIN_ALLOWED_DISTANCE) {
                m.drive_forward(255);
                } else {
                   m.stop();
                }
                break;
            case BACKWARD:
                m.drive_backward(255);
                break;
            case RIGHT:
                m.turn_right(192);
                break;
            case LEFT:
                m.turn_left(192);
                break;
            case NEUTRAL:
                m.stop();
                break;
          }
          report = "";
      } else {
          if ((char)data != '\r') {
             report += (char)data;
          }
      }
    }
}


// the loop function runs over and over again forever
void loop() {
  
  handle_proximity_sensor();
  handle_bluetooth();
}





