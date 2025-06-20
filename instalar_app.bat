@echo off
echo ========================================
echo    INSTALADOR DE APP MQTT - MyPaws
echo ========================================
echo.

echo Verificando dispositivos conectados...
adb devices

echo.
echo ========================================
echo INSTRUCCIONES PARA INSTALAR:
echo ========================================
echo.
echo 1. Conecta tu dispositivo Android por USB
echo 2. Activa la depuracion USB en tu dispositivo:
echo    - Ve a Ajustes ^> Acerca del telefono
echo    - Toca 7 veces en "Numero de compilacion"
echo    - Ve a Ajustes ^> Opciones de desarrollador
echo    - Activa "Depuracion USB"
echo.
echo 3. Cuando conectes el dispositivo, acepta la autorizacion
echo    que aparecera en la pantalla
echo.
echo 4. Ejecuta este script nuevamente
echo.
echo ========================================
echo.

if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo APK encontrado: app-debug.apk
    echo Tamanio: 
    dir "app\build\outputs\apk\debug\app-debug.apk" | find "app-debug.apk"
    echo.
    echo Para instalar manualmente, ejecuta:
    echo adb install -r app\build\outputs\apk\debug\app-debug.apk
    echo.
) else (
    echo ERROR: No se encontro el APK
    echo Ejecuta: gradlew assembleDebug
    echo.
)

echo Presiona cualquier tecla para continuar...
pause > nul 