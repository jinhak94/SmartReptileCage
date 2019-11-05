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
        self.MainSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.MainSocket.setsockopt(socket.SOL_SOCKET, socket.SO_SNDBUF, 65536)
        self.runFlag = True

    def stop(self):
        self.runFlag = False

    def run(self):
        global output

        if (self.ID is "") or (self.IP is ""):
            return

        print("Waiting for TF Server...")
        
        try:
            self.MainSocket.connect((self.IP, self.Port))
        except Exception as ee:
            return
        print("TF Server Connected: ", self.IP[:-1], ", ", self.Port)
        self.MainSocket.send(bytes(self.ID, "UTF-8"))
        time.sleep(2)

        while self.runFlag:
            try:
                if output.frame is not None:
                    with output.condition:
                        output.condition.wait()
                        frame = output.frame
                        self.MainSocket.send(frame)
            except:
                return
            time.sleep(5)

class TCPServer(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)
        #self.IP = '192.168.0.7'
        f = open('/home/pi/Python/SmartCage/ip.conf', 'r')
        line = f.readline()
        f.close()
        self.IP = line 
        self.Port = 8100
        self.MainSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.MainSocket.setsockopt(socket.SOL_SOCKET, socket.SO_SNDBUF, 65536)
        self.runFlag = True

    def stop(self):
        self.runFlag = False

    def run(self):
        global output

        f = open('/home/pi/Python/SmartCage/id.conf', 'r')
        line = f.readline()

        if line is "":
            return
        
        print("Waiting for Flask Server Connection...")

        try:
            self.MainSocket.connect((self.IP, self.Port))
        except Exception as ee:
            #print("Failed to connect Flask Server!")
            return
        print("Connected! : " + str(self.IP) + ", " + str(self.Port))

        self.MainSocket.send(bytes(line, "UTF-8"))
        
        time.sleep(2)

        while self.runFlag:
            try:
                '''
                self.MainSocket.send(bytes(line, "UTF-8"))
                with output.condition:
                    output.condition.wait()
                    frame = output.frame
                    if output.frame is not None:
                        self.MainSocket.send(output.frame)
                '''

        #try:
                if output.frame is not None:
                    with output.condition:
                        output.condition.wait()
                        frame = output.frame
                        self.MainSocket.send(frame)
            except:
                return
                #print(len(frame))
                #time.sleep(10)
        #except KeyboardInterrupt:
        #    self.MainSocket.close()
        #    print("Disconnected!")
        #    exit(0)

with picamera.PiCamera(resolution='640x480', framerate=10) as camera:
    output = StreamingOutput()
    camera.start_recording(output, format='mjpeg')
    tcpServer = TCPServer()
    tcpServer.start()
    tensorFlowClient = TensorFlowClient()
    tensorFlowClient.start()

    tcpServer.join()
    tensorFlowClient.join()
