#!/usr/bin/python
#-*- coding=utf-8 -*-
import Image
import os
import sys

#缩放图片	
def zoomAll(dirl,x,y):	
	filenames = os.listdir(dirl)

	for imgs in filenames:
		print imgs
		img = Image.open(dirl+imgs)
		im=img.resize((x,y),Image.ANTIALIAS)
		im.save(dirl+imgs)
		
if len(sys.argv) == 1:
	print "example: rotateImg dir (90)"
	sys.exit(1)
	
dirs=sys.argv[1]
x = int(sys.argv[2])
y = int(sys.argv[3])

if os.path.isfile(dirs):
	print "isfile"
	rotateTarget(dirs,degree)
else:
	print "isdir"
	zoomAll(dirs,x,y)
