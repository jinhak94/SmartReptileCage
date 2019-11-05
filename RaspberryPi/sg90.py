import RPi.GPIO as GPIO
import time

pin = 18

GPIO.setmode(GPIO.BCM)
GPIO.setup(pin, GPIO.OUT)
GPIO.output(pin, True)
p = GPIO.PWM(pin, 50)
cnt = 0

'''
try:
    while True:
        p.ChangeDutyCycle(1)
        print("angle : 1")
        time.sleep(1)
        p.ChangeDutyCycle(5)
        print("angle : 5")
        time.sleep(1)
        p.ChangeDutyCycle(8)
        print("angle : 8")
        time.sleep(1)

except KeyboardInterrupt:
    p.stop()
'''


try:
    p.start(12.5)
    p.ChangeDutyCycle(0)

    while True:
        deg = input('Degree: ')
        deg = float(deg)
        print("Trans: ", deg)

        #p.ChangeDutyCycle(deg)
        p.ChangeDutyCycle(deg)
        time.sleep(0.6)

        p.ChangeDutyCycle(12.5)
        time.sleep(1)

        p.ChangeDutyCycle(0)

        #p.stop()
except KeyboardInterrupt:
    p.stop()


GPIO.cleanup()
