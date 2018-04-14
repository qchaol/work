#!/bin/sh

echo 0 | sudo update-alternatives --config java

read num



echo $num | sudo update-alternatives --config java


echo $num | sudo update-alternatives --config javap



echo $num | sudo update-alternatives --config javac



java -version
