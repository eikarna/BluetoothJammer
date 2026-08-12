# BluetoothJammer — Edición Mejorada (v1.4)

Herramienta de **investigación educativa** sobre seguridad Bluetooth para Android (Kotlin / Material 3).

Versión mejorada del proyecto original [eikarna/BluetoothJammer](https://github.com/eikarna/BluetoothJammer), con detección real de dispositivos cercanos, clasificación de altavoces, múltiples técnicas de ataque seleccionables y motor de coroutines.

---

## ⚠️ DISCLAIMER LEGAL

> **ESTA APLICACIÓN ES SOLO PARA FINES EDUCATIVOS Y DE INVESTIGACIÓN.**
>
> - Úsala **ÚNICAMENTE con dispositivos de tu propiedad** y dentro de tu propio entorno.
> - **Interferir, atacar o degradar dispositivos que no te pertenecen es ilegal** en la mayoría de jurisdicciones (FCC en EE. UU., normativa europea, legislación local, etc.) y puede constituir un delito.
> - La aplicación incluye un **aviso obligatorio al abrirse** y recordatorios al iniciar cualquier ataque.
> - El desarrollador, colaboradores y mantenedores **no se hacen responsables del uso indebido** de esta herramienta. El uso correcto o incorrecto es responsabilidad exclusiva del usuario final.
> - Esta herramienta **no** es un jammer de radiofrecuencia: un teléfono no puede emitir RF arbitraria. Implementa técnicas de denegación de servicio a nivel de protocolo Bluetooth (L2CAP/RFCOMM, GATT, SDP, advertising BLE) dentro de los límites del SDK de Android.
> - Esta versión es **EXCLUSIVAMENTE para uso experimental, educativo y privado**. No está pensada para su uso fuera de un entorno controlado de investigación.

---

## 📜 Licencia

Este fork se publica bajo **licencia MIT** (ver [LICENSE](LICENSE)), pero con una salvedad legal importante:

- El repositorio original ([eikarna/BluetoothJammer](https://github.com/eikarna/BluetoothJammer)) **no tiene licencia**, por lo que su autor conserva todos los derechos sobre el código original.
- La licencia MIT cubre **solo las modificaciones y el código nuevo** aportados por este fork (manuti), tal y como se detalla en el propio fichero LICENSE.
- El autor original no ha otorgado permiso explícito para esta publicación; la intención de este fork es contribuir al estudio educativo de la seguridad Bluetooth con atribución clara al trabajo original.

**Responsabilidades legales:** este software se proporciona "TAL CUAL", sin garantías de ningún tipo. El autor de este fork **no se hace responsable** de ningún daño, pérdida o consecuencia legal derivada del uso indebido de esta herramienta. Es responsabilidad exclusiva del usuario final conocer y respetar la legislación local (FCC, normativa europea, etc.) y usar la aplicación únicamente con dispositivos de su propiedad y con fines educativos/privados.

---

## Novedades de la versión 1.4

- **RFCOMM Channel Flood**: barre los canales RFCOMM 1-30 usando la API oculta `createInsecureRfcommSocket(int)` vía reflexión (técnica clásica de SPP; puede ser bloqueada por la *hidden API enforcement* en Android 9+ — se registra en el log).
- **Modo bombardeo**: ciclos rápidos conectar → enviar ráfaga → cerrar (en vez de mantener el socket), para agotar recursos por saturación de ciclos.
- **Tamaño de payload configurable** (bytes): 0 = automático (maxTransmitPacketSize o 600), p. ej. 990 B.
- **Combo ampliado**: ahora combina L2CAP + **RFCOMM channel** + GATT + Pairing + SDP (5 capas).
- Ideas adoptadas de [hackeringtrue/bluetooth2jam](https://github.com/hackeringtrue/bluetooth2jam) (barrido de canales, bombardeo, payload 990 B).

## Novedades de la versión 1.3

- **Ciclo de ráfaga/pausa (TX/Sleep) con jitter**: controla los ataques en ráfagas (p. ej. 10 s activo / 5 s pausa) con temporizadores pseudoaleatorios — concepto portado del **Jammer TX del PortaPack Mayhem** (firmware SDR).
- **Selector de patrón de payload** (análogo a los tipos de señal del PortaPack): ruido aleatorio, patrón fijo (A-Z), sierra (0-255) u ondulado (chirp).
- **Resumen de sesión**: al detener se imprime en el log el tiempo, los eventos CONN/DATA/RETRY y el número de objetivos.

## Novedades de la versión 1.2

- **AttackManager centralizado**: registro de ataques por objetivo y **stop global** (patrón portado del fork [PIXELQUADRO07/BluetoothJammer](https://github.com/PIXELQUADRO07/BluetoothJammer)). Detiene todo incluso si la Activity murió.
- **Multi-target**: mantén pulsado un dispositivo para añadirlo a la selección y pulsa **Atacar (N)** para lanzar el mismo tipo de ataque contra varios objetivos a la vez.
- **Logs estructurados por categoría**: `[THREAD] [CONN] [DATA] [RETRY] [PAIR] [GATT] [SDP] [ADV] [SPOOF] [COMBO]` — permite medir tasas de conexión/envío/reintento en sesiones multi-objetivo.
- **Tests del gestor**: `AttackManagerTest` (4 casos) cubre registro multi-objetivo y stop global.

## Novedades de la versión 1.1

- **7 tipos de ataque seleccionables** (antes 5): se añaden **Profile Spoofing** y **Combo en capas**.
- **Ataque en capas (Combo)**: L2CAP + GATT + Pairing + SDP simultáneos sobre el mismo objetivo con un único Start/Stop.
- **Profile Spoofing**: cicla UUIDs de perfiles conocidos (A2DP, HID, HFP, OPP, SPP, PBAP) presentándose como cada uno; sirve de sonda de servicios y satura canales.
- **Control de tasa + jitter**: nuevo campo **Delay (ms)** para limitar la velocidad de cada ataque, con retrasos pseudo-aleatorios (anti-patrón periódico).
- **Pairing spam**: inunda al objetivo de solicitudes de emparejamiento (BR/EDR; `createBond(TRANSPORT_LE)` es API oculta, así que solo clásico).
- **Fingerprinting de dispositivo**: fabricante por **OUI** (primeros 3 bytes de la MAC) y **servicios soportados** (SDP para clásico, ScanRecord para BLE) mostrados en la lista y en la ficha del dispositivo.
- **Logging por worker**: cada worker/hilo reporta sus intentos, conexiones y contadores.

## Novedades de la versión 1.0

- **Detección real de dispositivos cercanos** (antes solo listaba los ya emparejados).
- **Clasificador de altavoces** con 3 señales combinadas (clase Bluetooth, appearance BLE, nombre).
- **5 técnicas de ataque seleccionables** tras elegir el objetivo.
- **Botón Stop funcional**: los ataques se detienen de verdad (antes requería forzar el cierre de la app).
- **Aviso educativo obligatorio** al abrir la aplicación.
- **UI renovada**: botón Escanear, filtro "Solo altavoces", línea de estado, badges 🔊, RSSI y tipo de dispositivo.
- **Correcciones**: buffer de 0 bytes en el flood L2CAP (no escribía nada en RFCOMM), imports correctos del paquete `android.bluetooth.le`, permisos y compatibilidad API 24–34.

---

## Funcionalidades

### 1. Detección de dispositivos (`ScanNearbyDevices`)

Combina **tres fuentes** en una sola lista deduplicada por dirección MAC:

- **Emparejados** (`bondedDevices`) — siempre visibles como ancla.
- **Discovery clásico** (`startDiscovery` + `ACTION_FOUND`) — captura nombre, clase de dispositivo y RSSI.
- **Escaneo BLE** (`BluetoothLeScanner`, modo low-latency) — captura nombre, RSSI, *appearance* y servicios del `ScanRecord`.

Además, cada dispositivo se enriquece con:

- **Fabricante (OUI)**: los 3 primeros bytes de la MAC se comparan contra una tabla local (Apple, Samsung, MediaTek, Broadcom, Qualcomm, Intel, Google, Huawei, Xiaomi…). Tabla **parcial** — puede fallar con direcciones aleatorias.
- **Servicios soportados**: SDP (`fetchUuidsWithSdp`, una sonda por dispositivo) para clásico y `ScanRecord.serviceUuids` para BLE, traducidos a nombres legibles (A2DP, HID, HFP, GATT…).

Los resultados se ordenan con los altavoces primero y luego por cercanía (RSSI), y se entregan por eventos.

### 2. Clasificador de altavoces (`SpeakerClassifier`)

Determina si un dispositivo es probablemente un altavoz, combinando señales de mayor a menor fiabilidad:

| Señal | Fuente | Confianza |
|---|---|---|
| Clase Bluetooth | `BluetoothClass` major Audio/Video + minor (Altavoz, Hi-Fi, Pantalla+altavoz) | Alta |
| Clase Bluetooth | Audio portátil / Audio de coche | Media |
| Appearance BLE | Campo GAP `0x0017` (Generic Speaker) | Alta |
| Nombre | Heurística de keywords (JBL, Sonos, soundbar, echo, …) | Media |

Los auriculares quedan **excluidos explícitamente**. La UI muestra un badge 🔊 con la razón de la clasificación ("Clase BT", "BLE", "Nombre: …").

### 3. Técnicas de ataque (`AttackType` / interfaz `BluetoothAttack`)

Se eligen con un selector (Spinner) una vez seleccionado el objetivo. **Threads** = intensidad (workers/concurrencia); **Delay (ms)** = pausa entre ráfagas (0 = máxima velocidad, con jitter aleatorio).

| Tipo | Capa | Descripción | Intensidad |
|---|---|---|---|
| **L2CAP Flood (clásico)** | RFCOMM/L2CAP | Abre sockets RFCOMM con UUIDs aleatorios y satura el socket conectado. | 1–64 workers |
| **RFCOMM Channel Flood** | RFCOMM | Barre canales RFCOMM 1-30 vía reflexión (API oculta) y satura los sockets. | 1–30 workers |
| **GATT Flood (BLE)** | GATT | Llena la tabla de conexiones GATT del periférico (la mayoría solo acepta unas pocas). | 4–64 conexiones paralelas |
| **Pairing Flood** | Bonding | Inunda al objetivo de solicitudes de emparejamiento (BR/EDR). | 1–3 workers (el stack serializa) |
| **SDP Query Storm** | SDP | Satura el servidor SDP con consultas de servicios repetidas. | 1–16 consultas concurrentes |
| **Advertising Flood (BLE)** | Advertising | Contamina el canal de anuncios BLE con UUIDs aleatorios (requiere soporte de advertising BLE). | n/a |
| **Profile Spoofing** | RFCOMM | Cicla UUIDs de perfiles conocidos (A2DP, HID, HFP, OPP, SPP, PBAP) probando conexión como cada uno. | 1–9 workers |
| **Combo** | Todas | L2CAP + GATT + Pairing + SDP simultáneos coordinados bajo un único Start/Stop. | según capa |

Todos los ataques:

- Corren sobre **coroutines** en `Dispatchers.IO` con su propio flag de ejecución.
- Reportan progreso por **worker** al log (activable/desactivable con el switch Log).
- **Se detienen limpiamente** con el botón Stop: cancelan su scope y cierran sockets/conexiones.

### 4. Seguridad y UX

- **Diálogo de aviso educativo** obligatorio (no cancelable) al abrir la app.
- **Toast recordatorio** ("úsalo solo con dispositivos de tu propiedad") al iniciar cada ataque.
- Log con marcas de tiempo (`Logger`) y límite de 100 líneas.

---

## Lo que NO es posible sin root (documentado)

Limitaciones técnicas del SDK de Android (API pública) que esta app **no implementa ni puede implementar** sin root o hardware externo:

- **Jamming de radiofrecuencia** (ruido en 2.4 GHz): requiere SDR (HackRF/ESP32); el módem del teléfono no expone RF arbitraria.
- **Desautenticación (deauth) BR/EDR**: es un paquete de nivel de enlace (LMP_detach); la API pública no expone LMP y `createL2capSocket` está oculto/bloqueado por SELinux.
- **L2CAP a PSMs arbitrarios / UUIDs malformados**: `createInsecureRfcommSocketToServiceRecord(UUID)` valida el UUID; solo RFCOMM (PSM 0x03) es público y no se pueden enviar tramas malformadas.
- **Exploits de CVEs (BlueFrag CVE-2020-0022, CVE-2024-43763, MediaTek, etc.)**: requieren tramas L2CAP/HCI crudas (no emitibles por la API pública) y targets con stacks antiguos sin parchear; los RCE no son material de prueba de estrés educativa y quedan fuera del alcance.
- **Suplantación de MAC** en advertising BLE: el controlador fija la dirección (puede ser aleatoria, pero no elegible).
- **Variación de firma de paquetes a nivel de stack**: los headers los controla el stack; solo se varía payload/UUID y temporización (jitter).

---

## Requisitos

- **Android**: minSdk 24 (Android 7.0), targetSdk 34.
- **Permisos** (declarados en el manifest): `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`.
- **Compilación**: JDK 17, Android SDK platform 34 + build-tools 34.0.0, Gradle 8.7 (wrapper incluido), AGP 8.6.0, Kotlin 1.9.0.

### Compilar

```bash
./gradlew :app:assembleDebug
# APK de salida:
#   app/build/outputs/apk/debug/app-debug.apk
```

### Tests unitarios

```bash
./gradlew :app:testDebugUnitTest
# 22 tests (clasificador + metadatos + manager + payload + ejemplo)
```

### Nota para ARM64 (Termux / aarch64)

AGP 8.6 distribuye `aapt2` compilado solo para **x86-64**, que no ejecuta en ARM64. Para compilar en un dispositivo ARM64:

1. Instala el paquete nativo de Termux: `pkg install aapt2`
2. Añade en `~/.gradle/gradle.properties`:

```properties
android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
```

3. El shebang de `gradlew` (`#!/usr/bin/env sh`) no resuelve en Termux; ejecútalo con `sh gradlew`.

---

## Instalación

1. Compila o descarga el APK debug (`app/build/outputs/apk/debug/app-debug.apk`).
2. Copia el APK a tu dispositivo (p. ej. `~/storage/downloads/` en Termux).
3. Ábrelo con el gestor de archivos y permite "instalar aplicaciones desconocidas".
4. Al abrir la app: acepta el aviso educativo y concede los permisos de Bluetooth/ubicación cuando se soliciten.

---

## Estructura del proyecto

```
app/src/main/java/com/eikarna/bluetoothjammer/
├── MainActivity.kt            # Detección, lista de dispositivos, aviso educativo
├── AttackActivity.kt          # Selección de objetivo, tipo de ataque, Threads/Delay y Start/Stop
├── api/
│   ├── BluetoothAttack.kt     # Interfaz común + enum AttackType (selector + fábrica)
│   ├── AttackManager.kt      # Registro por objetivo + stop global (multi-target)
│   ├── ScanNearbyDevices.kt   # Motor de escaneo (emparejados + clásico + BLE) + sonda SDP
│   ├── SpeakerClassifier.kt   # Clasificador de altavoces (clase BT/appearance/nombre)
│   ├── DeviceMetadata.kt      # OUI→fabricante y UUID→perfil (tablas locales)
│   ├── AttackTiming.kt        # Control de tasa con jitter (jitterDelay)
│   ├── AttackDevices.kt       # L2capFloodAttack (flood RFCOMM/L2CAP)
│   ├── FloodSupport.kt        # Bucle de flood compartido (payload/tamaño/rate)
│   ├── RfcommChannelFloodAttack.kt # Barrido de canales RFCOMM 1-30 (reflexión)
│   ├── GattFloodAttack.kt     # Flood de conexiones GATT (BLE)
│   ├── PairingFloodAttack.kt  # Flood de emparejamiento clásico + BLE
│   ├── SdpFloodAttack.kt      # Tormenta de consultas SDP
│   ├── AdvertiseFloodAttack.kt# Flood de advertising BLE
│   ├── ProfileSpoofAttack.kt  # Spoofing de perfiles (A2DP, HID, HFP…)
│   └── ComboAttack.kt         # Ataque en capas (L2CAP+GATT+Pairing+SDP)
├── util/Logger.kt             # Log con timestamp
└── ui/theme/                  # Tema Material 3
```

---

## Limitaciones conocidas

- **Identificación de altavoces** depende de lo que publique cada fabricante (clase/appearance); los dispositivos que no publican metadata solo se detectan por heurística de nombre.
- **Fabricante (OUI)** usa una tabla parcial; con direcciones MAC aleatorias (comunes en BLE) el resultado puede ser nulo o incorrecto.
- **Advertising Flood** requiere que el hardware soporte advertising BLE (`isMultipleAdvertisementSupported`) y falla si el radio está ocupado.
- **Profile Spoofing** no es una suplantación real: los stacks modernos validan el protocolo de cada perfil; el efecto es saturación de canales y sondeo de servicios.
- **No es un jammer de RF** y no puede emitir tramas crudas (ver sección "Lo que NO es posible sin root").
- La detección y la efectividad dependen del hardware del teléfono (antena, alcance).

---

## Créditos

- Repositorio original: [eikarna/BluetoothJammer](https://github.com/eikarna/BluetoothJammer)
- Fork con ideas adoptadas: [PIXELQUADRO07/BluetoothJammer](https://github.com/PIXELQUADRO07/BluetoothJammer) (AttackManager, logs estructurados)
- Conceptos de jamming RF: [PortaPack Mayhem — Jammer TX](https://github.com/portapack-mayhem/mayhem-firmware/wiki/Jammer) (duty cycle TX/Sleep + jitter, tipos de señal)
- Ideas no-root adoptadas: [hackeringtrue/bluetooth2jam](https://github.com/hackeringtrue/bluetooth2jam) (barrido de canales RFCOMM, bombardeo connect/disconnect, payload 990 B)
- Inspiración y asistencia de desarrollo: ChatGPT-4o (repo original) y herramientas de desarrollo asistido (esta edición).

---

*Este proyecto se publica con fines educativos. Respeta la privacidad y la propiedad de los demás.*
