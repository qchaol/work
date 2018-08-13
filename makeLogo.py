#!/usr/bin/python
#-*- coding:utf-8 -*-
import sys
import os
import shutil
import Image


#converse to bmp
def converseBmp(src):      
	extension=os.path.splitext(src)[1]
	im=Image.open(src)
	if extension==".png":
		bg = Image.new("RGB", im.size, (255,255,255))
		bg.paste(im,im)
		bg.save("ll.jpg")
        	im=Image.open("ll.jpg")
		im.save('ll.bmp')	
	else:
		im.save('ll.bmp')



#move picture
def movePic(argv):
	#tail 
	tailu="_uboot"
	tailk="_kernel"
	li=[]
	for x in range(len(argv)):
		print isspr		
		if isspr != 3 :			
			li.append(argv[x]+tailu)
		li.append(argv[x]+tailk)		
	for i in range(len(li)):
		shutil.copyfile("ll.bmp", os.getcwd()+"/"+li[i].split('_')[0]+"/"+li[i]+".bmp")

#exec sh
def execScript(argv):	
	if os.path.exists("local_make_logo_bin"):		
		print argv
		os.system('./local_make_logo_bin '+argv)
	elif isspr==3:
		print "update_sprd_multi " + argv
		os.system('./update_sprd_multi '+argv)
	else:		
		print argv.replace(" ","_")
		os.system('./update_multi '+argv)


#del temp picture
def removeTempFile():
	if os.path.exists("ll.bmp"): 
		os.remove("ll.bmp")
	if os.path.exists("ll.jpg"):
		os.remove("ll.jpg")

#select project
def selectArgv():
	global isspr
	isspr = 0
	flat = raw_input("please input a number: \n1 zh960 \n2 k960 \n3 b960\n4 a708 a706\n5 w801\n6 zh960a\n")
	 
	if flat == "1":
		return "wxganl wxxganl wuxganl"
	if flat == "2":
		return "wsvganl wxganl wxga"
	if flat =="3":
		isspr = 3
		return "wxganl wxxganl wxga"
	if flat =="4":
		isspr = 3
		return "wsvga wsvganl wxga"
	if flat == "5":
		return "wxganl wuxganl"
	if flat == "6":
		return "wxganl wxxganl wuxganl wuxgafhd wxga wuxga"
		
	print "wrong input,bye!!"	
	sys.exit(1)
	
if __name__ =="__main__":
	if len(sys.argv) > 1:
		converseBmp(sys.argv[1])	
	else:
		print """#########################################################
		\nexample:\n    python makeLogo.py l.jpg\n#########################################################\n"""
		sys.exit(1)
	select =selectArgv()	
	movePic(select.split(" "))
	execScript(select)
	removeTempFile()
