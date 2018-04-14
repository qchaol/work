#!/usr/bin/python
#-*- coding=utf-8 -*-
import Image
import os
import sys


	
#多文件旋转	
def rotateAll(dirl,degree):	
	filenames = os.listdir(dirl)

	for imgs in filenames:
		print imgs
		img = Image.open(dirl+imgs)
		im=img.rotate(degree)
		im.save(dirl+imgs)
#单文件旋转		
def rotateTarget(filename,degree):
	img = Image.open(filename)
	im=img.rotate(degree)
	im.save(filename)

if len(sys.argv) == 1:
	print "example: rotateImg dir (90)"
	sys.exit(1)
	
dirs=sys.argv[1]
degree = 90
if len(sys.argv) > 2:
	degree = sys.argv[2]
	degree = float(degree)

if os.path.isfile(dirs):
	print "isfile"
	rotateTarget(dirs,degree)
else:
	print "isdir"
	rotateAll(dirs,degree)



