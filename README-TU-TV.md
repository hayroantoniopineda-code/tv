# Tu Google TV Streamer

## Configuración actual

- CleanYourTV está instalado como aplicación normal. Google Home sigue como Home predeterminado para conservar Cast.
- Hay cuatro espacios en el dock. Entra a Biblioteca de aplicaciones con el botón de cuadrícula. Pulsa OK para agregar o quitar una app de Inicio. El menú de reordenamiento permite cambiar el orden con el control.
- Las vistas previas son opcionales y consultan canales locales de Android TV al volver a Inicio. Solo aparecen cuando la app publica recomendaciones; algunas IPTV y servicios no lo hacen. No descarga imágenes ni mantiene sondeos en segundo plano.
- Los dos relojes usan formato de 12 horas con AM/PM: el principal sigue la zona horaria del sistema y el secundario muestra Honduras. Para que la hora local cambie al viajar, deja activada la zona horaria automática en Android.
- Se agregó el pronóstico de cinco días para tu zona. Cuando aceptes la ubicación aproximada, Android puede actualizarla usando redes Wi-Fi disponibles; al cambiar de zona, el pronóstico se actualiza al volver a Inicio. Si no se da permiso o no hay ubicación, usa el centro aproximado del ZIP 30092. No lee ni envía el nombre o contraseña del Wi-Fi.
- El pronóstico usa Open-Meteo y envía solo coordenadas aproximadas para consultar temperaturas y condiciones. Se guarda una copia pequeña; actualiza como máximo cada tres horas, solo con Home abierto. La ubicación se consulta de nuevo al regresar después de 20 minutos. La escena del clima dibuja sol, nubes, lluvia, nieve o tormenta con formas vectoriales propias a unos 15 fotogramas por segundo; se detiene al salir de Home. No usa videos, imágenes, desenfoque ni una capa de hardware separada. El fondo cambia de tono por hora y condición del tiempo.
- La escena animada se muestra junto a los dos relojes; por la noche representa la luna y estrellas. El resumen indica si parece buen momento para caminar y el pronóstico de cinco días se pliega al explorar aplicaciones para dejar más espacio a la cuadrícula.
- Después de cinco minutos sin usar Home se muestra el salvapantallas. Mantiene los relojes local y de Honduras y el clima en un panel discreto; el fondo aprovecha el papel tapiz activo y su rotación configurada, o cambia cada 90 segundos entre paisajes vectoriales oscuros si no hay fondos propios. Los paisajes no usan fotos ni memoria de bitmap, y la animación se pausa al salir del salvapantallas.
- Se deshabilitaron cinco servicios reversibles de comentarios, registro remoto y publicidad del Google TV Streamer. No se desinstaló ninguna app.

## Proton VPN

El launcher está configurado para mostrar Proton VPN con el paquete Android ch.protonvpn.android. En esta sesión Android no encontró Proton VPN instalada, así que el botón dedicado no aparece aún. Si la instalas desde Google Play, el botón podrá abrirla. La conexión y la selección del servidor se hacen dentro de Proton VPN; configura allí Quick Connect para conservar tu servidor favorito.

## Abrir y personalizar

Google Home sigue predeterminado. Abre CleanYourTV desde Apps en Google TV. Usa el control para moverte por las apps. En Configuración puedes cambiar el fondo y el formato del reloj.

## Reinstalar sin reemplazar Google Home

En PowerShell, con JDK 17, Android SDK y una conexión ADB inalámbrica cifrada autorizada:

  $env:TV_ADB_BINARY = 'C:\ruta\a\adb.exe'
  $env:JAVA_HOME = 'C:\ruta\a\jdk-17'
  $env:ANDROID_HOME = 'C:\ruta\al\android-sdk'
  .\launcher\install-preserving-home.ps1 -TvHost '<ip-del-TV>:<puerto-ADB-inalambrico>'

El script compila, revisa el límite de 589824 bytes e instala el APK como app. No cambia Home predeterminado ni deshabilita Google Home.

Si ya existe un APK compilado en launcher/app/build/outputs/apk/release/app-release.apk, añade -NoBuild para instalarlo sin compilar.

## Restaurar los servicios

Activa temporalmente la depuración inalámbrica del TV y empareja el PC si Android solicita un código nuevo. Luego ejecuta en PowerShell:

  $env:TV_ADB_BINARY = 'C:\ruta\a\adb.exe'
  .\tools\restore.ps1 examples\google-tv-streamer-4k-kirkwood\debloat-keep-cast.txt -TvHost '<ip-del-TV>:<puerto-ADB-inalambrico>'

La lista de recuperación se guarda en %USERPROFILE%\.cleanyourtv\. Las copias privadas del estado inicial están en backup y las capturas en shots. No compartas esos directorios: pueden contener identificadores del dispositivo y datos de configuración.

Al finalizar una sesión ADB, apaga Depuración inalámbrica y Depuración USB en Opciones para desarrolladores y verifica que los puertos estén cerrados.
