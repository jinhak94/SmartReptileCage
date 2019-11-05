import threading
import socket
import time
import ssl

import io
import socketserver
from threading import Condition
from http import server
import logging

MAX_USER = 10
dic = {}

class MainServer(threading.Thread,):
    def __init__(self):
        threading.Thread.__init__(self)
        self.IP = '127.0.0.1'
        self.Port = 8100
        self.MainSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.MainSocket.setsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF, 65536)

    def run(self):
        self.MainSocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        try:
            self.MainSocket.bind(('0.0.0.0', self.Port))
        except:
            exit()

        while True:
              totalData = bytes()
              jpgNum = 1
              while True:
                data, addr = self.MainSocket.recvfrom(65536)
                #print(len(data))
    
                if data[0] == 65:
                  jpgCnt = data[1]
                  name = data[4:data[3]+4]

                  #print(data[2], ', ', jpgNum)
                  #print(len(data))
                  if data[2] == jpgNum:
                    jpgNum = jpgNum + 1
                  else:
                    totalData = bytes()
                    jpgNum = 1
                    continue
                  
                  if data[-2] == 255 and data[-1] == 217:
                    totalData = totalData + data[data[3]+4:]

                    strName = str(name[:-1], 'utf-8')
                
                    if totalData[0] == 255 and totalData[1] == 216:
                        condition.acquire()
                        dic[strName] = totalData
                        condition.notify()
                        condition.release()
                        totalData = bytes()
                        jpgNum = 1
                    else:
                        totalData = bytes()
                        jpgNum = 1
                  else:
                    totalData = totalData + data[data[3]+4:]
                    continue
                    

class StreamingHandler(server.BaseHTTPRequestHandler):
    def do_GET(self):
        key = self.path[1:]

        if key in dic.keys():
            self.send_response(200)
            self.send_header('Age', 0)
            self.send_header('Cache-Control', 'no-cache, private')
            self.send_header('Pragma', 'no-cache')
            self.send_header('Content-Type', 'multipart/x-mixed-replace; boundary=FRAME')
            self.end_headers()
            try:
                while True:
                    condition.acquire()
                    condition.wait()
                    bins = dic[key]
                    #print(len(bins))
                    condition.release()
                    
                    #if not key in dic.keys():
                    #    print('Key is not available.')
                    #    return
                        
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
                    
condition = threading.Condition()
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
