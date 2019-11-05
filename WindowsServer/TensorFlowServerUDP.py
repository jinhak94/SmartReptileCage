import numpy as np
import os
import six.moves.urllib as urllib
import sys
import tarfile
import tensorflow as tf
import zipfile

from collections import defaultdict
from io import StringIO
import matplotlib
matplotlib.use('TkAgg')
from matplotlib import pyplot as plt
from PIL import Image

# This is needed since the notebook is stored in the object_detection folder.
sys.path.append("..")
from object_detection.utils import ops as utils_ops

#if tf.__version__ < '1.4.0':
#  raise ImportError('Please upgrade your tensorflow installation to v1.4.* or later!')

# This is needed to display the images.
# %matplotlib inline

from object_detection.utils import label_map_util
from object_detection.utils import visualization_utils as vis_util

import time
import cv2

import socketserver
import socket
import threading
import pymysql
import datetime

MAX_USER = 16

# What model to download.
#MODEL_NAME = 'ssd_mobilenet_v1_coco_2017_11_17'
#MODEL_FILE = MODEL_NAME + '.tar.gz'
#DOWNLOAD_BASE = 'http://download.tensorflow.org/models/object_detection/'

# Path to frozen detection graph. This is the actual model that is used for the object detection.
PATH_TO_CKPT = './export_dir/faster_rcnn_inception_v2_coco_2018_01_28/frozen_inference_graph.pb'

# List of the strings that is used to add correct label for each box.
PATH_TO_LABELS = './label_map.pbtxt'

NUM_CLASSES = 1

# opener = urllib.request.URLopener()
# opener.retrieve(DOWNLOAD_BASE + MODEL_FILE, MODEL_FILE)
# tar_file = tarfile.open(MODEL_FILE)
# for file in tar_file.getmembers():
#   file_name = os.path.basename(file.name)
#   if 'frozen_inference_graph.pb' in file_name:
#     tar_file.extract(file, os.getcwd())

detection_graph = tf.Graph()
with detection_graph.as_default():
  od_graph_def = tf.GraphDef()
  with tf.gfile.GFile(PATH_TO_CKPT, 'rb') as fid:
    serialized_graph = fid.read()
    od_graph_def.ParseFromString(serialized_graph)
    tf.import_graph_def(od_graph_def, name='')

label_map = label_map_util.load_labelmap(PATH_TO_LABELS)
categories = label_map_util.convert_label_map_to_categories(label_map, max_num_classes=NUM_CLASSES, use_display_name=True)
category_index = label_map_util.create_category_index(categories)

def load_image_into_numpy_array(image):
	(im_width, im_height) = image.size
	return np.array(image.getdata()).reshape(
		(im_height, im_width, 3)).astype(np.uint8)

# For the sake of simplicity we will use only 2 images:
# image1.jpg
# image2.jpg
# If you want to test the code with your images, just add path to the images to the TEST_IMAGE_PATHS.

# Size, in inches, of the output images.
IMAGE_SIZE = (12, 8)

route_list = []

def run_inference_for_single_image(image, sess, name):
  start = time.time()
	
  # Get handles to input and output tensors
  ops = tf.get_default_graph().get_operations()
  all_tensor_names = {output.name for op in ops for output in op.outputs}
  tensor_dict = {}
  for key in [
	  'num_detections', 'detection_boxes', 'detection_scores',
	  'detection_classes', 'detection_masks'
	  ]:
    tensor_name = key + ':0'
    if tensor_name in all_tensor_names:
      tensor_dict[key] = tf.get_default_graph().get_tensor_by_name(
      tensor_name)
  if 'detection_masks' in tensor_dict:
    # The following processing is only for single image
    detection_boxes = tf.squeeze(tensor_dict['detection_boxes'], [0])
    detection_masks = tf.squeeze(tensor_dict['detection_masks'], [0])
    # Reframe is required to translate mask from box coordinates to image coordinates and fit the image size.
    real_num_detection = tf.cast(tensor_dict['num_detections'][0], tf.int32)
    detection_boxes = tf.slice(detection_boxes, [0, 0], [real_num_detection, -1])
    detection_masks = tf.slice(detection_masks, [0, 0, 0], [real_num_detection, -1, -1])
    detection_masks_reframed = utils_ops.reframe_box_masks_to_image_masks(
      detection_masks, detection_boxes, image.shape[0], image.shape[1])
    detection_masks_reframed = tf.cast(
      tf.greater(detection_masks_reframed, 0.9), tf.uint8)
    # Follow the convention by adding back the batch dimension
    tensor_dict['detection_masks'] = tf.expand_dims(
      detection_masks_reframed, 0)
  image_tensor = tf.get_default_graph().get_tensor_by_name('image_tensor:0')
		
	
  # Run inference
  output_dict = sess.run(tensor_dict,
					    feed_dict={image_tensor: np.expand_dims(image, 0)})
		

  # all outputs are float32 numpy arrays, so convert types as appropriate
  output_dict['num_detections'] = int(output_dict['num_detections'][0])
  output_dict['detection_classes'] = output_dict[
	  'detection_classes'][0].astype(np.uint8)
  output_dict['detection_boxes'] = output_dict['detection_boxes'][0]
  output_dict['detection_scores'] = output_dict['detection_scores'][0]
  if 'detection_masks' in output_dict:
    output_dict['detection_masks'] = output_dict['detection_masks'][0]

  image_pil = Image.fromarray(np.uint8(image)).convert('RGB')
  im_width, im_height = image_pil.size

  found = False
  for item in range(len(output_dict['detection_boxes'])):
    if output_dict['detection_scores'][item] > 0.8:
      left = output_dict['detection_boxes'][item][1] * im_width
      right = output_dict['detection_boxes'][item][3] * im_width
      top = output_dict['detection_boxes'][item][0] * im_height
      bottom = output_dict['detection_boxes'][item][2] * im_height
      center_x = int((right + left) / 2)
      center_y = int((bottom + top) / 2)
      print("*************************************************************")
      print("[ ", name ," ] Lizard's point: ", center_x, ",", center_y)

      try:
        global route_list
        
        appended = str(center_x) + ',' + str(center_y)
        route_list.append(appended)
        route_str = ""
        if len(route_list) is 60:
          for route_item in route_list:
            if route_str is not "":
              route_str = route_str + "-"
            route_str = route_str + route_item
          #print(route_str)
          conn = pymysql.connect(host='127.0.0.1', port=3306, user='root', passwd='passwd', db='smartcage')
          cur = conn.cursor()
          command = "insert into route(id, route) values ('%s', '%s')" % (name, route_str)
          cur.execute(command)
          conn.commit()
          print("*************************************************************")
          print("Database send completed.")

          now = datetime.datetime.now()
          monthBefore = now - datetime.timedelta(days=7)
          nowDatetime = monthBefore.strftime('%Y-%m-%d %H:%M:%S')   
          command = "delete from route where date < '" + nowDatetime + "';"
          print("Time: ",nowDatetime)
          #print(command)
          cur.execute(command)
          conn.commit()

          cur.close()
          conn.close()
          
          del(route_list[:])
      except Exception as eee:
        print(eee)
      
      end = time.time() - start
      print("Running time: ",end)
      found = True
      break
  if found is False:
    print("No lizard is in that picture.")

  return output_dict



class TensorFlowServer(threading.Thread):
  def __init__(self):
    threading.Thread.__init__(self)
    self.IP = '127.0.0.1'
    self.Port = 8200
    self.MainSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    #self.MainSocket.setsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF, 65536)

  def run(self):
    #self.MainSocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    try:
      self.MainSocket.bind(('0.0.0.0', self.Port))
    except:
      exit()

    while True:
      graph = detection_graph
      with graph.as_default():
        with tf.Session() as sess:
          totalData = bytes()
          jpgNum = 1
          
          while True:
            data, addr = self.MainSocket.recvfrom(65536)

            if data[0] == 65:
              jpgCnt = data[1]
              name = data[4:data[3]+4]

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
                  file_path = 'C:\\' + strName + '.jpg'
                  f = open(file_path, 'wb')
                  f.write(totalData)
                  f.close()
                  image_np = cv2.imread(file_path)
                  image_np = cv2.cvtColor(image_np, cv2.COLOR_BGR2RGB)
                  output_dict = run_inference_for_single_image(image_np, sess, strName)
                else:
                  totalData = bytes()
                  jpgNum = 1

                totalData = bytes()
              else:
                totalData = totalData + data[data[3]+4:]
                continue



tensorFlowServer = TensorFlowServer()
tensorFlowServer.start()

tensorFlowServer.join()
