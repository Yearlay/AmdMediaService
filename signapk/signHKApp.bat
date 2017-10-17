java -jar signapk.jar platform.x509.pem platform.pk8 ../bin/AMTMediaService.apk MediaService.apk
adb remount
adb push ./MediaService.apk vendor/app/
adb reboot