adb root
java -jar signapk.jar platform.x509.pem platform.pk8 ../bin/SkinManager.apk SkinManager.apk

adb remount

adb push ./SkinManager.apk vendor/app/

adb reboot