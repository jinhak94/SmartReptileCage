import RPi.GPIO as GPIO
import LiquidCrystalPi
import time as time
import sys

argStr1 = sys.argv[1]
argStr2 = sys.argv[2]

GPIO.setmode(GPIO.BCM)
GPIO.setwarnings(False)

LCD = LiquidCrystalPi.LCD(25, 24, 21, 17, 20, 22)

LCD.begin(16, 2)

time.sleep(0.5)

LCD.home()
LCD.write(argStr1)
LCD.nextline()
LCD.write(argStr2)
