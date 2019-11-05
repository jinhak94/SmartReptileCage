import threading
import socket
import time
from datetime import date, time, datetime
import sys
import Adafruit_DHT
import subprocess
import pymysql

MAX_USER = 10
SERVER_IP = sys.argv[1]
SERVER_PORT = int(sys.argv[2])

DHT11_sensor = Adafruit_DHT.DHT11
DHT11_pin = 2

class SimpleServer(threading.Thread):
    def __init__(self, IP, Port):
        threading.Thread.__init__(self)
        self.runFlag = True
        self.IP = IP
        self.Port = Port
        self.MainSocket = socket.socket()

    def stopServer(self):
        self.runFlag = False
        
    def run(self):
        self.MainSocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        try:
            self.MainSocket.bind((self.IP, self.Port))
        except:
            exit()
        self.MainSocket.listen(MAX_USER)
        
        while self.runFlag == True:
            Connection, Address = self.MainSocket.accept()
            Service = SimpleServerService(Connection, Address)
            Service.start()
            now = datetime.now()
            print("\n* [Server] New client ",Address," is Accepted!" , "\n- Time: " , str(now))
            
class DBThread(threading.Thread):
    def __init__(self):
        self.temp = temp
        self.humi = humi

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
        if self.CheckWlan() is True:
            f = open('/home/pi/Python/SmartCage/ip.conf', 'r')
            line = f.readline()
            f.close()

            conn = pymysql.connect(host=line, port=3306, user='root', passwd='passwd', db='smartcage')
            cur = conn.cursor()
            cur.execute("select * from userinfo")

            print(cur.description)

            try:
                humi, temp = Adafruit_DHT.read_retry(DHT11_sensor, DHT11_pin)
            finally:
                print(humi, temp)

            cur.close()
            conn.close()

class SimpleServerService(threading.Thread):
    def __init__(self, Connection, Address):
        threading.Thread.__init__(self)
        self.runFlag = True
        self.Connection = Connection
        self.Address = Address

    def getInformation(self):
        try:
            humidity, temperature = Adafruit_DHT.read_retry(DHT11_sensor, DHT11_pin)
        finally:
            return temperature, humidity

    def getTemperature(self):
        try:
            humidity, temperature = Adafruit_DHT.read_retry(DHT11_sensor, DHT11_pin)
        finally:
            return temperature

    def getHumidity(self):
        try:
            humidity, temperature = Adafruit_DHT.read_retry(DHT11_sensor, DHT11_pin)
        finally:
            return humidity 
    def run(self):
        buffer = bytes(128)
        
        while self.runFlag == True:
            try:
                buffer = self.Connection.recv(128)
                buffer_str = buffer.decode("UTF-8")
                #buffer_str = buffer_str[:(len(buffer_str) - 1)]
                now = datetime.now()

                if(buffer_str == ""):
                    self.runFlag = False
                    print("\n* [Server] Client ",self.Address," is diconnected!" , "\n- Time: " , str(now))
                    continue

                print("\n* [Server]",buffer_str, "Received!" , "\n- Client: ", self.Address, "\n- Time: " , str(now))
                if(buffer_str == "GET"):
                    self.Connection.send(bytes("Response to GET!", "UTF-8"))
                elif(buffer_str == "INFORMATION\n"):
                    temperature, humidity = self.getInformation()
                    if temperature is not None:
                        if humidity is not None:
                            data = "i:"+str(temperature)+","+str(humidity)+":e"
                            self.Connection.send(bytes(data, "UTf-8"))
                        else:
                            self.Connection.send(bytes("ERROR", "UTF-8"))
                    else:
                        self.Connection.send(bytes("ERROR", "UTF-8"))
                elif(buffer_str == "TEMPERATURE\n"):
                    temperature = self.getTemperature()
                    if temperature is not None:
                        data = "t:"+str(temperature)
                        self.Connection.send(bytes(data, "UTf-8"))
                    else:
                        self.Connection.send(bytes("ERROR", "UTF-8"))
                elif(buffer_str == "HUMIDITY\n"):
                    humidity = self.getHumidity()
                    if humidity is not None:
                        data = "h:"+str(humidity)
                        self.Connection.send(bytes(data, "UTF-8"))
                    else:
                        self.Connection.send(bytes("ERROR", "UTF-8"))
                else:
                    self.Connection.send(bytes("Nothing!", "UTF-8"))
            except:
                continue
            
myserver = SimpleServer(SERVER_IP, SERVER_PORT)
myserver.start()
