import io
import picamera
import logging
import socketserver
import socket
from threading import Condition
import threading
from http import server
import time

PAGE="""\
<html>
<head>
<title>picamera MJPEG streaming demo</title>
</head>
<body>
<h1>PiCamera MJPEG Streaming Demo</h1>
<img src="stream.mjpg" width="640" height="480" />
</body>
</html>
"""

class StreamingOutput(object):
    def __init__(self):
        self.frame = None
        self.buffer = io.BytesIO()
        self.condition = Condition()

    def write(self, buf):
        if buf.startswith(b'\xff\xd8'):
            self.buffer.truncate()
            with self.condition:
                self.frame = self.buffer.getvalue()
                self.condition.notify_all()
            self.buffer.seek(0)
        return self.buffer.write(buf)

class TensorFlowClient(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)

        f = open('/home/pi/Python/SmartCage/ip.conf', 'r')
        line = f.readline()
        self.IP = line
        f.close()
        f = open('/home/pi/Python/SmartCage/id.conf', 'r')
        line = f.readline()
        self.ID = line
        f.close()
        self.Port = 8200
        self.MainSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        #self.MainSocket.setsockopt(socket.SOL_SOCKET, socket.SO_SNDBUF, 65536)
        self.runFlag = True

    def stop(self):
        self.runFlag = False

    def run(self):
        global output

        if (self.ID is "") or (self.IP is ""):
            return

        print("Waiting for TF Server...")

        while self.runFlag:
            try:
                if output.frame is not None:
                    with output.condition:
                        output.condition.wait()
                        jpg = output.frame
                        jpgLen = len(jpg)
                        jpgCnt = int(jpgLen / 65000) + 1
                        jpgNum = 1
                        jpgStart = 0
                        jpgEnd = 0

                        while jpgLen > 0:
                            frame = bytes([65, jpgCnt, jpgNum, len(self.ID)]) + bytes(self.ID, 'utf-8')
                            jpgNum = jpgNum + 1
                            if jpgLen > 65000:
                                jpgEnd = jpgEnd + 65000
                                jpgLen = jpgLen - 65000
                            else:
                                jpgEnd = jpgEnd + jpgLen
                                jpgLen = 0
                            frame = frame + jpg[jpgStart:jpgEnd]
                            jpgStart = jpgStart + 65000
                            self.MainSocket.sendto(frame, (self.IP, self.Port))
            except Exception as eee:
                print(eee)
                return
            time.sleep(5)

class UDPServer(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)
        #self.IP = '192.168.0.7'
        f = open('/home/pi/Python/SmartCage/ip.conf', 'r')
        line = f.readline()
        f.close()
        self.IP = line 
        f = open('/home/pi/Python/SmartCage/id.conf', 'r')
        line = f.readline()
        f.close()
        self.ID = line 
        self.Port = 8100
        self.MainSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.MainSocket.setsockopt(socket.SOL_SOCKET, socket.SO_SNDBUF, 65536)
        self.runFlag = True

    def stop(self):
        self.runFlag = False

    def run(self):
        global output

        f = open('/home/pi/Python/SmartCage/id.conf', 'r')
        line = f.readline()
        
        print("Waiting for Flask Server Connection...")

        while self.runFlag:
            try:
                if output.frame is not None:
                    with output.condition:
                        output.condition.wait()
                        jpg = output.frame
                        jpgLen = len(jpg)
                        jpgCnt = int(jpgLen / 65000) + 1
                        jpgNum = 1
                        jpgStart = 0
                        jpgEnd = 0

                        while jpgLen > 0:
                            frame = bytes([65, jpgCnt, jpgNum, len(self.ID)]) + bytes(self.ID, 'utf-8')
                            jpgNum = jpgNum + 1
                            if jpgLen > 65000:
                                jpgEnd = jpgEnd + 65000
                                jpgLen = jpgLen - 65000
                            else:
                                jpgEnd = jpgEnd + jpgLen
                                jpgLen = 0
                            frame = frame + jpg[jpgStart:jpgEnd]
                            jpgStart = jpgStart + 65000
                            self.MainSocket.sendto(frame, (self.IP, self.Port))
            except Exception as eee:
                print(eee)

with picamera.PiCamera(resolution='1024x768', framerate=25) as camera:
    #camera.rotation = 270
    output = StreamingOutput()
    camera.start_recording(output, format='mjpeg')
    udpServer = UDPServer()
    udpServer.start()
    tensorFlowClient = TensorFlowClient()
    tensorFlowClient.start()

    udpServer.join()
    tensorFlowClient.join()
