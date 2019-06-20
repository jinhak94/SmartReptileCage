import threading
import socket
import time

import io
import socketserver
from threading import Condition
from http import server
import logging

MAX_USER = 10
dic = {}

class MainServer(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)
        self.IP = '127.0.0.1'
        self.Port = 8100
        self.MainSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.MainSocket.setsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF, 65536)

    def run(self):
        self.MainSocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        try:
            self.MainSocket.bind(('0.0.0.0', self.Port))
        except:
            exit()
        self.MainSocket.listen(MAX_USER)

        while True:
            Connection, Address = self.MainSocket.accept()
            Service = ServiceServer(Connection, Address)
            Service.start()

class ServiceServer(threading.Thread):
    def __init__(self, Connection, Address):
        threading.Thread.__init__(self)
        self.Connection = Connection
        self.Address = Address
        self.Name = None
        self.Data = None

    def run(self):
        AddrStr = str(self.Address)
        print("Client " + AddrStr + " is connected.")
        
        buffer = bytes(65536)

        checkBuffer = bytes(65536)
        checkCount = 0

        while True:
            try:
                recv = self.Connection.recv(65536)

                #print(len(recv))

                if checkBuffer == recv:
                    checkCount = checkCount + 1
                    if checkCount > 30:
                        print("Client " + AddrStr + " is out of check!")
                        return
                else:
                    checkCount = 0
                    checkBuffer = recv

                #print(len(recv))

                if recv is 0:
                    print("Client " + AddrStr + "is disconnected.")
                    return

                if recv[0] is 255 and recv[1] is 216 and self.Name is not None:
                    dic[self.Name] = recv
                elif len(recv) < 30:
                    isChar = True
                    for item in recv:
                        if not (item > 0x00 and item < 0x7F):
                            isChar = False

                    if isChar is True:
                        recvStr = recv.decode('UTF-8') 
                        self.Name = recvStr
                        print("New Client is " + self.Name)
                            
            except Exception as e:
                print(e)
                #return

class StreamingHandler(server.BaseHTTPRequestHandler):
    def do_GET(self):
        key = self.path[1:] + '\n'

        if key in dic.keys():
            self.send_response(200)
            self.send_header('Age', 0)
            self.send_header('Cache-Control', 'no-cache, private')
            self.send_header('Pragma', 'no-cache')
            self.send_header('Content-Type', 'multipart/x-mixed-replace; boundary=FRAME')
            self.end_headers()
            try:
                while True:
                    bins = dic[key]
                    
                    if not key in dic.keys():
                        return
                    self.wfile.write(b'--FRAME\r\n')
                    self.send_header('Content-Type', 'image/jpeg')
                    self.send_header('Content-Length', len(bins))
                    self.end_headers()
                    
                    self.wfile.write(bins)
                    #print(dic[key])
                    #print(len(bins))
                    #time.sleep(30)
                    self.wfile.write(b'\r\n')
            except Exception as e:
                logging.warning('Removed Streaming Client %s, %s', self.client_address, str(e))
        else:
            self.send_error(404)
            self.end_headers()
            
class StreamingServer(socketserver.ThreadingMixIn, server.HTTPServer):
    allow_reuse_address = True
    daemon_threads = True
                    

mainServer = MainServer()
mainServer.start()

try:
    address = ('', 8120)
    server = StreamingServer(address, StreamingHandler)
    server.serve_forever()
finally:
    pass

#while True:
#    pass
