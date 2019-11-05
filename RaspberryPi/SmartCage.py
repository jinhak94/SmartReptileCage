'''
    Title: RaspiController
    Description: A classic-bluetooth based server which is able to set up Raspi setting such as Wi-Fi
    Date: 2019-02-13(Wed)
    Writer: Byungwook Kim
'''

import threading
import time
import wifi
import subprocess
import os
import bluetooth
from pbkdf2 import PBKDF2

reqSSID = ""
reqPassword = ""

class EnvironmentThread(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)

    def run(self):
        #startTime = time.time()
        while True:

            #endTime = time.time()
            #if (endTime - startTime) > 30:
            os.popen('sudo hciconfig hci0 piscan')
            print("*** Reset PISCAN mode.")

            popen = subprocess.Popen('ifconfig wlan0 | grep -w inet | awk \'{ print $2 }\'', stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
            (output, errors) = popen.communicate()
            servAddr = output[:-1]
            servAddr = str(servAddr, "UTF-8")
            if servAddr is not "":
                #command = 'sudo python3 /home/pi/Python/SmartCage/SimpleServer.py ' + servAddr + ' 20001 &'
                psresult = subprocess.check_output("ps -ef | grep python3 | grep -v grep", shell=True)
                psresult = str(psresult, "UTF-8")

                if 'DBThread.py' in psresult:
                    pass
                else:
                    command = 'sudo python3 /home/pi/Python/SmartCage/DBThread.py &'
                    try:
                        os.popen(command)
                    except:
                        pass
                    print("*** Reset Simple Server.")

                if 'FlaskClient.py' in psresult:
                    pass
                else:
                    command = 'sudo python3 /home/pi/Python/SmartCage/FlaskClient.py &'
                    try:
                        os.popen(command)
                    except:
                        pass

            command = "/sbin/ifup wlan0"
            try:
                proc = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
                (output, errors) = proc.communicate()
                print("*** Reset wlan0 Status.")
            except:
                pass

            time.sleep(30)
             #   startTime = time.time()

class BluetoothAcceptServer(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)
        self.server_socket = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
        self.port = 1
        self.server_socket.bind(("", self.port))
        self.server_socket.listen(1)
        result = os.popen('sudo hciconfig hci0 piscan').read()
        print("*** Scan activated.")
        command = "/sbin/ifup wlan0"
        proc = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
        (output, errors) = proc.communicate()

    def run(self):
        while True:
            client_socket, client_address = self.server_socket.accept()
            print("*** Accepted from: ", client_address)

            global reqSSID
            global reqPassword
            reqSSID = ""
            reqPassword = ""

            bluetoothServer = BluetoothServer(self.server_socket, client_socket, client_address)
            bluetoothServer.start()

class BluetoothServer(threading.Thread):
    def __init__(self, server_socket, client_socket, client_address):
        threading.Thread.__init__(self)
        self.reqSSID = ""
        self.reqPassword = ""
        self.server_socket = server_socket
        self.client_socket = client_socket
        self.client_address = client_address
        self.lastSend = ""
        self.runFlag = True

    def StopServer(self):
        self.runFlag = False

    def run(self):

        while self.runFlag == True:

            try:
                rcvBytes = self.client_socket.recv(1024)
                if len(rcvBytes) is not 0:
                    rcvStr = str(rcvBytes, "UTF-8")
                    serviceThread = ServiceThread(self.client_address, self.client_socket, self.client_address, rcvStr)
                    serviceThread.start()

            except:
                print("*** Disconnected from ", self.client_address)
                self.runFlag = False

class ServiceThread(threading.Thread):
    def __init__(self, server_socket, client_socket, client_address, msg):
        threading.Thread.__init__(self)
        self.server_socket = server_socket
        self.client_socket = client_socket
        self.client_address = client_address
        self.msg = msg

    def Search(self):
        wifilist = []

        cells = None
        counter = 0

        while cells is None:
            try:
                cells = wifi.Cell.all('wlan0')
            except:
                self.ActivateWlan()
            if cells is not None:
                break
            counter = counter + 1
            if counter > 30:
                break
            time.sleep(1)

        for cell in cells:
            wifilist.append(cell)
        return wifilist

    def FindFromSearchList(self,ssid):
        wifilist = self.Search()

        for cell in wifilist:
            if cell.ssid == ssid:
                return cell

        return False

    def FindFromSavedList(self,ssid):
        command = 'cat /etc/wpa_supplicant/wpa_supplicant.conf | grep ssid | grep ='
        proc = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
        (output, errors) = proc.communicate()
        curSSID = str(output, "UTF-8")
        curSSID = curSSID[7:-2]

        command = 'cat /etc/wpa_supplicant/wpa_supplicant.conf | grep psk | grep ='
        proc = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
        (output, errors) = proc.communicate()
        curPassword = str(output, "UTF-8")
        curPassword = curPassword[6:-2]

    def ActivateWlan(self):
        command = "/sbin/ifup wlan0"
        proc = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
        output, errors = proc.communicate()
        #print(output, errors)

    def DeactivateWlan(self):
        command = "/sbin/ifdown wlan0"
        proc = subprocess.Popen(command, shell=True)
        output, errors = proc.communicate()

    def CheckWlan(self):
        res = subprocess.check_output("iwconfig 2>&1 | grep ESSID", shell=True)
        resStr = str(res, "UTF-8")
        resSplit = resStr.split("ESSID:")
        wlanStatus = resSplit[1]
        if "off/any" in wlanStatus:
            return False 
        else:
            return True

    def WifiConnect(self, ssid, password=None):
        cell = self.FindFromSearchList(ssid)

        if cell:
            if cell.encrypted:
                if password is None:
                    print("*** Password Required!")
                    return False

                print("*** 1")
                self.DeactivateWlan()
                print("*** 2")
                self.AddToWpaSupplicant(ssid, password)
                print("*** 3")
                time.sleep(3)
                self.ActivateWlan()
                print("*** 4")

                counter = 0
                while counter < 180:
                    if self.CheckWlan() is True:
                        popen = subprocess.Popen('ifconfig wlan0 | grep -w inet | awk \'{ print $2 }\'', stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
                        (stdoutdata, stderrdata) = popen.communicate()
                        stdoutaddr = stdoutdata[:-1]
                        stdoutaddr = str(stdoutaddr, "UTF-8")
                        if stdoutaddr is "":
                            continue
                        print("*** Successfully connected Wi-Fi!")
                        print("*** Start New Simple Server.")
                        print("*** New Simpler Server IP: " + stdoutaddr)
                        #command = 'sudo python3 /home/pi/Python/SmartCage/SimpleServer.py ' + stdoutaddr + ' 20001 &'
                        #command = 'sudo python3 /home/pi/Python/SmartCage/DBThread.py &'
                        #os.popen(command)
                        psresult = subprocess.check_output("ps -ef | grep python3 | grep -v grep", shell=True)
                        psresult = str(psresult, "UTF-8")

                        if 'DBThread.py' in psresult:
                            pass
                        else:
                            command = 'sudo python3 /home/pi/Python/SmartCage/DBThread.py &'
                            try:
                                os.popen(command)
                            except:
                                pass

                        if 'FlaskServer.py' in psresult:
                            pass
                        else:
                            command = 'sudo python3 /home/pi/Python/SmartCage/FlaskServer.py &'
                            try:
                                os.popen(command)
                            except:
                                pass

                        return True

                counter = counter + 1
                time.sleep(1)

                print("*** Connection Failed.")
                return False

            else:
                # Modify required
                self.DeactivateWlan()
                self.AddToWpaSupplicant(ssid)
                time.sleep(3)
                self.ActivateWlan()

                counter = 0
                while counter < 180:
                    if self.CheckWlan() is True:
                        popen = subprocess.Popen('ifconfig wlan0 | grep -w inet | awk \'{ print $2 }\'', stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
                        (stdoutdata, stderrdata) = popen.communicate()
                        stdoutaddr = stdoutdata[:-1]
                        stdoutaddr = str(stdoutaddr, "UTF-8")
                        if stdoutaddr is "":
                            continue
                        print("*** Successfully connected Wi-Fi!")
                        print("*** Start New Simple Server.")
                        print("*** New Simpler Server IP: " + stdoutaddr)
                        #command = 'sudo python3 /home/pi/Python/SmartCage/SimpleServer.py ' + stdoutaddr + ' 20001 &'
                        #command = 'sudo python3 /home/pi/Python/SmartCage/DBThread.py &'
                        #os.popen(command)


                        return True

                print("*** Connection Failed.")
                return False

        return False

    def AddToWpaSupplicant(self, ssid, password=None):
        wpa = open('/etc/wpa_supplicant/wpa_supplicant.conf', 'w', encoding='utf8')
        contents = 'ctrl_interface=DIR=/var/run/wpa_supplicant GROUP=netdev\n'
        contents = contents + 'update_config=1\n'
        contents = contents + 'country=US\n\n'
        contents = contents + 'network={\n'
        contents = contents + '\tssid=' + '\"' + ssid + '\"\n'
        contents = contents + '\tkey_mgmt=WPA-PSK\n'
        contents = contents + '\tpsk=' + '\"' + password + '\"\n'
        contents = contents + '}'
        wpa.write(contents)
        wpa.close()

    def SendCommand(self, command):
        self.client_socket.send(bytes(command, "UTF-8"))
        lastSend = command
        command_head = command.split('~')
        command_head = command_head[0]
        if (command_head == "i") or (command_head == "o"):
            pass
        else:
            print("*** Respond to client ", self.client_address," : ", command)


    def run(self):
        rcvStr = self.msg

        global reqSSID
        global reqPassword
        if rcvStr != "I~E~E":
            print("*** Received from client ", self.client_address,  " : ", rcvStr)

        splits = rcvStr.split('~')
        if len(splits) is not 3:
            if splits[0] is not 'I':
                self.SendCommand("e~e~e")
                print("*** Error occured: ",rcvStr)
                return
        if splits[2] is not 'E':
            if splits[0] is not 'I':
                self.SendCommand("e~e~e")
                print("*** Error occured: ",rcvStr)
                return
        if splits[0] != 'I':
            self.SendCommand("o~e~e")

        if splits[0] == 'L':
            cells = None
            counter = 0

            self.ActivateWlan()
            while cells is None:
                try:
                    cells = wifi.Cell.all('wlan0')
                except:
                    pass
                if cells is not None:
                    break
                counter = counter + 1
                if counter > 30:
                    #print("*** Wi-Fi Module error. Please retry!")
                    #return
                    pass
                time.sleep(1)
            if cells is not None:
                for cell in cells:
                    wifiSSID = "l~"+str(cell.ssid)+"~e"
                    self.SendCommand(wifiSSID)
        elif splits[0] == 'U':
            splits[1] = splits[1] + '\n'
            f = open("/home/pi/Python/SmartCage/id.conf", 'w')
            f.write(splits[1])
            f.close()
            print("*** User syncronized : " + splits[1])

            output = subprocess.check_output("ps -ef | grep python3 | grep -v grep | grep FlaskServer | awk '{print $2}'", shell=True)
            output_str = str(output, 'utf-8')
            output_splits = output_str.split('\n')
            for item in output_splits:
                if item is '':
                    continue
                command = "sudo kill -9 " + item
                output_tmp = subprocess.check_output(command, shell=True)
                print("*** Killed FlaskServer PID " + item)

            output = subprocess.check_output("ps -ef | grep python3 | grep -v grep | grep DBThread | awk '{print $2}'", shell=True)
            output_str = str(output, 'utf-8')
            output_splits = output_str.split('\n')
            for item in output_splits:
                if item is '':
                    continue
                command = "sudo kill -9 " + item
                output_tmp = subprocess.check_output(command, shell=True)
                print("*** Killed DBThread PID " + item)

            #os.popen("sudo python3 /home/pi/Python/SmartCage/FlaskServer.py")
            #os.popen("sudo python3 /home/pi/Python/SmartCage/DBThread.py")

            time.sleep(1)

            psresult = subprocess.check_output("ps -ef | grep python3 | grep -v grep", shell=True)
            psresult = str(psresult, "UTF-8")

            if 'DBThread.py' in psresult:
                pass
            else:
                command = 'sudo python3 /home/pi/Python/SmartCage/DBThread.py &'
                try:
                    os.popen(command)
                except:
                    pass

            if 'FlaskServer.py' in psresult:
                pass
            else:
                command = 'sudo python3 /home/pi/Python/SmartCage/FlaskServer.py &'
                try:
                    os.popen(command)
                except:
                    pass

        elif splits[0] == 'S':
            reqSSID = splits[1]
        elif splits[0] == 'P':
            reqPassword = splits[1]  
        elif splits[0] == 'C':
            if reqSSID == "":
                print("*** Error: SSID required!")
                return
            if reqPassword == "":
                print("*** Error: Password required!")
                return
            tempSSID = reqSSID
            tempPassword = reqPassword
            reqSSID = ""
            reqPassword = ""

            conRes = self.WifiConnect(tempSSID, tempPassword)

            print("Connection Result: ", conRes)

            if conRes is True:
                output = subprocess.check_output("ps -ef | grep python3 | grep -v grep | grep FlaskServer | awk '{print $2}'", shell=True)
                output_str = str(output, 'utf-8')
                output_splits = output_str.split('\n')
                for item in output_splits:
                    if item is '':
                        continue
                    command = "sudo kill -9 " + item
                    output_tmp = subprocess.check_output(command, shell=True)
                    print("*** Killed FlaskServer PID " + item)

                output = subprocess.check_output("ps -ef | grep python3 | grep -v grep | grep DBThread | awk '{print $2}'", shell=True)
                output_str = str(output, 'utf-8')
                output_splits = output_str.split('\n')
                for item in output_splits:
                    if item is '':
                        continue
                    command = "sudo kill -9 " + item
                    output_tmp = subprocess.check_output(command, shell=True)
                    print("*** Killed DBThread PID " + item)

                #os.popen("sudo python3 /home/pi/Python/SmartCage/FlaskServer.py")
                #os.popen("sudo python3 /home/pi/Python/SmartCage/DBThread.py")

                time.sleep(1)

                psresult = subprocess.check_output("ps -ef | grep python3 | grep -v grep", shell=True)
                psresult = str(psresult, "UTF-8")

                if 'DBThread.py' in psresult:
                    pass
                else:
                    command = 'sudo python3 /home/pi/Python/SmartCage/DBThread.py &'
                    try:
                        os.popen(command)
                    except:
                        pass

                if 'FlaskServer.py' in psresult:
                    pass
                else:
                    command = 'sudo python3 /home/pi/Python/SmartCage/FlaskServer.py &'
                    try:
                        os.popen(command)
                    except:
                        pass
            # Connection information initialization
        elif splits[0] == 'I':
            connectionStatus = self.CheckWlan()
            if connectionStatus is True:
                comm = 'ifconfig wlan0 | grep -w inet | awk \'{ print $2 }\''
                result = os.popen(comm).read()
                self.SendCommand("i~"+result+"~e")
            else:
                self.SendCommand("i~f~e")
        elif splits[0] == 'E':
            self.SendCommand(lastSend)
            return
        elif splits[0] == 'B':
            self.SendBeacon()

accept_server = BluetoothAcceptServer()
accept_server.start()

env_thread = EnvironmentThread()
env_thread.start()
