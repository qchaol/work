#!/bin/sh 
adb shell dumpsys window windows | grep -E 'mCurrentFocus|mFocusedApp' --color=always
