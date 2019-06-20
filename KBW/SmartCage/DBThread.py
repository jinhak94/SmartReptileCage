import threading
import time as tm
import datetime
import Adafruit_DHT
import subprocess
import pymysql
import os
import RPi.GPIO as GPIO

DHT11_sensor = Adafruit_DHT.DHT11
DHT11_pin = 2
SG90_pin = 18

GPIO.setmode(GPIO.BCM)
GPIO.setup(SG90_pin, GPIO.OUT)
p = GPIO.PWM(SG90_pin, 50)
p.start(0)

class DBThread(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)
        self.feedtimedif = 0

    def CheckWlan(self):
        res = subprocess.check_output("iwconfig 2>&1 | grep ESSID", shell=True)
        resStr = str(res, 'UTF-8')
        resSplit = resStr.split("ESSID:")
        wlanStatus = resSplit[1]
        if "off/any" in wlanStatus:
            return False
        else:
            return True

    def run(self):
        while True:
            try:
                if self.CheckWlan() is True:
                    f = open('/home/pi/Python/SmartCage/ip.conf', 'r')
                    line = f.readline()
                    f.close()

                    conn = pymysql.connect(host=line, port=3306, user='root', passwd='passwd', db='smartcage')
                    cur = conn.cursor()
                    #cur.execute("select * from userinfo")

                    #print(cur.description)

                    humi, temp = Adafruit_DHT.read_retry(DHT11_sensor, DHT11_pin)

                    humi_str = str(humi)
                    temp_str = str(temp)

                    lcdStr1 = '"T:' + temp_str + 'C ' + 'H:' + humi_str + '%"'
                    lcdStr2 = '"WiFi:ON"'
                    lcdCom = "python /home/pi/Python/SmartCage/LCD.py " + lcdStr1 + " " + lcdStr2
                    os.popen(lcdCom)

                    #now = datetime.datetime.now()
                    #nowDatetime = now.strftime('%Y-%m-%d %H:%M:%S')
                    nowDatetime = int(tm.time())

                    f = open('/home/pi/Python/SmartCage/id.conf', 'r')
                    line = f.readline()
                    f.close()

                    if line is "":
                        continue

                    line = line[:-1]

                    command = "select feedtime, feedleft from cage where id='%s';" % (line)
                    cur.execute(command)
                    for row in cur:
                        feedtime = row[0]
                        feedleft = row[1]
                    feedtime = int(feedtime)
                    feedleft = int(feedleft)
                    if feedtime != 0:
                        if self.feedtimedif == 0:
                            self.feedtimedif = tm.time()
                            feedleft = feedleft - 5
                            command = "update cage set feedleft=%d where id ='%s'" % (feedleft, line)
                            cur.execute(command)
                            conn.commit()
                        else:
                            now = tm.time()
                            timedif = now - self.feedtimedif
                            timedif = int(timedif)
                            feedleft = feedleft - timedif
                            command = "update cage set feedleft=%d where id='%s'" % (feedleft, line)
                            cur.execute(command)
                            conn.commit()
                            self.feedtimedif = tm.time()

                        if feedleft <= 0:
                            p.ChangeDutyCycle(9)
                            tm.sleep(1)
                            p.ChangeDutyCycle(1)
                            tm.sleep(1)
                            p.ChangeDutyCycle(9)
                            tm.sleep(1)
                            p.ChangeDutyCycle(1)
                            tm.sleep(1)
                            p.ChangeDutyCycle(9)
                            tm.sleep(1)
                            p.ChangeDutyCycle(1)

                            command = "update cage set feedleft=%d where id='%s'" % (feedtime, line)
                            cur.execute(command)
                            conn.commit()

                    command = "insert into cage(id, humi, temp, date) values ('%s', %s, %s, %s) on duplicate key update humi=%s, temp=%s, date=%s;" % (line, humi_str, temp_str, nowDatetime, humi_str, temp_str, nowDatetime);

                    if humi_str is not None and temp_str is not None:
                        cur.execute(command)
                        conn.commit()

                    cur.close()
                    conn.close()
                    tm.sleep(5)
                else:
                    lcdStr1 = '"T: ' + temp_str + 'C' + 'H: ' + humi_str + '%"'
                    lcdStr2 = '"WiFi:OFF"'
                    lcdCom = "python /home/pi/Python/SmartCage/LCD.py " + lcdStr1 + " " + lcdStr2
                    os.popen(lcdCom)
                    tm.sleep(5)
            except Exception as eee:
                print(eee)
                pass

thread = DBThread()
thread.start()
