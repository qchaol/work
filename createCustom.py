# -*- coding: utf-8 -*-

import sys
import os
import shutil
import xlwt
import collections

dir = os.getcwd()

cf = "custom_feature/feature/"
vf = "version_feature/feature/"

keys = ['custom','appRemoveable','appUnremoveable','priv-app','blackAppConfig','logoBin','wallpapers','bootanimation_2','bootanimation_1','bootanimation','machineAttribute', 'shutanimation','bootaudio','apnsConf','blackAppIconConfig','custpackConf','fatImg','shutaudio','configs','bakres','gms','sdcardZip']

dicts={}

def updateCode():
	print "update custom_feature ..."
	os.system("cd %s && git pull" % cf)
	print "update version_feature ..."
	os.system("cd %s && git pull" % vf)
	
def createFeatureDir(cn):
	index = cn.find('-')
	if(index == -1):
		customname = cn
		customnum = cn
	else:
		customname = cn[0:index]
		customnum = cn[index+1:]
	
	os.system('cd %s && mkdir -p %s/%s && touch %s/%s/readme' % (vf,customname,customnum,customname,customnum))	
	#os.system('mkdir -p %sapp/%s' % (cf,cn))
	#os.system('mkdir -p %sbakres/%s' % (cf,cn))
	

def mvFileToCustomFeature(direct,cn):
	filenames = os.listdir(direct)
	print filenames
	for name in filenames:
		if os.path.isfile(direct+name):
			if os.path.splitext(name)[1] ==".jpg" or os.path.splitext(name)[1] ==".png":
				shutil.copyfile(direct+name,cf+"wallpapers/"+cn+os.path.splitext(name)[1])
				dicts['wallpapers'] = cn+os.path.splitext(name)[1]
			if os.path.splitext(name)[1] ==".zip":
				if "_1" in os.path.splitext(name)[0]:
					shutil.copyfile(direct+name,cf+"bootanimation/"+cn+"_1"+os.path.splitext(name)[1])
					dicts['bootanimation_1'] = cn+"_1"+os.path.splitext(name)[1]
				if "_2" in os.path.splitext(name)[0]:
					shutil.copyfile(direct+name,cf+"bootanimation/"+cn+"_2"+os.path.splitext(name)[1])
					dicts['bootanimation_2'] = cn+"_2"+os.path.splitext(name)[1]
				else:
					shutil.copyfile(direct+name,cf+"bootanimation/"+cn+os.path.splitext(name)[1])
					dicts['bootanimation'] = cn+os.path.splitext(name)[1]
			if os.path.splitext(name)[1] ==".mp3":
				shutil.copyfile(direct+name,cf+"bootaudio/"+cn+os.path.splitext(name)[1])
				dicts['bootaudio'] = cn+os.path.splitext(name)[1]
			if os.path.splitext(name)[1] ==".bin" or os.path.splitext(name)[1] ==".bmp":
				shutil.copyfile(direct+name,cf+"logoBin/"+cn+os.path.splitext(name)[1])
				dicts['logoBin'] = cn+os.path.splitext(name)[1]
			if os.path.splitext(name)[1] ==".txt":
				shutil.copyfile(direct+name,cf+"custpackConf/"+name)
				dicts['custpackConf'] = name
			if os.path.splitext(name)[1] ==".xml":
				shutil.copyfile(direct+name,cf+"machineAttribute/"+cn+os.path.splitext(name)[1])
				dicts['machineAttribute'] = cn+os.path.splitext(name)[1]
		if os.path.isdir(direct+name):
			if name =="bakres":
				shutil.copytree(direct+name,cf+"bakres/"+cn)
				dicts['bakres'] = cn
			if name =="apk":
				print name
				shutil.copytree(direct+name,cf+"app/"+cn)
				dicts['appUnremoveable'] = cn
			if name =="apkr":
				print name
				shutil.copytree(direct+name,cf+"app/"+cn+"remove")
				dicts['appRemoveable'] = cn+"remove"
				

def writeToExcel():
	book = xlwt.Workbook(encoding='utf-8')
	sheet1 = book.add_sheet('Sheet 1')
	for i,val in enumerate(keys):		
		if dicts.has_key(keys[i]):
#			print dicts[keys[i]]
			sheet1.write(1,i,dicts[keys[i]])
	book.save('temp.xls') 
	

def getFileSize(filePath):
    size = 0
    for root, dirs, files in os.walk(filePath):
        for f in files:
            size += os.path.getsize(os.path.join(root, f))
            #print(f)
    size = size / 1024.0 /1024.0
    if size > 300:
       dicts['custpackConf'] = "600M.txt"
    if size > 600:
       dicts['custpackConf'] = "800M.txt"
    	
	

def main():	
	if len(sys.argv) == 1 :
		print "请输入客户名称 路径"
		sys.exit(1)		
	customName = sys.argv[1]
	
	updateCode()
	createFeatureDir(customName)
	dicts['custom'] = customName
	if len(sys.argv)>2:
		direct = sys.argv[2]
		getFileSize(direct)
		mvFileToCustomFeature(direct,customName)
	writeToExcel()


if __name__ == '__main__':
	main()
