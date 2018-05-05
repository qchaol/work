#!/usr/bin/python
#-*- coding:utf-8 -*-
import shutil
import os


#select project
def selectFile():
	flat = raw_input("please input a number: \n1 makeLogo \n2 createCustom \n3 \n4  \n5 \n")
	if flat == "1":
		shutil.copyfile("/home/allen/source/tools/makeLogo/makeLogo.py",os.path.getcwd())
	if flat == "2":
		return "wsvganl wxganl wxga"
	if flat =="3":
		isspr = 3
		return "wxganl wxxganl wxga"
	if flat =="4":
		isspr = 3
		return "wsvga_wsvganl_wxga"
	if flat == "5":
		return "wxganl wuxganl"
		
		
def main():
	selectFile()
		
		
if __name__=="__main__":
	main()
