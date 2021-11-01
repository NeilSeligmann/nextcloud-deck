#!/usr/bin/env bash

if ./gradlew connectedDebugAndroidTest; then
  echo "connectedDebugAndroidTest succeeded" >&2
  echo "adb ls -all (1)"
  adb shell ls /
  adb shell ls /mnt/
  adb shell ls /mnt/sdcard
  adb shell ls -R /mnt/sdcard/screenshots
  echo "ls -all (1)"
  ls -all
  adb pull /sdcard/screenshots screenshots
  echo "adb ls -all (1.1)"
  adb shell ls /
  adb shell ls /mnt/
  adb shell ls /mnt/sdcard
  adb shell ls -R /sdcard/screenshots
  echo "ls -all (1.1)"
  ls -all
  adb exec-out /sdcard/screenshots -p >screenshots
  echo "adb ls -all (1.2)"
  adb shell ls /
  adb shell ls /mnt/
  adb shell ls /mnt/sdcard
  adb shell ls -R /sdcard/screenshots
  echo "ls -all (1.2)"
  ls -all
  exit 0
else
  echo "ls -all (2)"
  ls -all
  adb pull /sdcard/screenshots screenshots
  echo "ls -all (2.1)"
  ls -all
  adb exec-out /sdcard/screenshots -p >screenshots
  echo "ls -all (2.2)"
  ls -all
  exit 1
fi