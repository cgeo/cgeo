### General
- Cambiado: Introducción a la navegación inferior para acceder directamente a las pantallas de c:geo's más usadas, reemplazando la vieja pantalla principal

### Mapa
- Corregido: Al cargar archivos GPX que contienen múltiples pistas las muestran como separadas, sin conexión
- Cambiado: Activar automáticamente la visualización de la pista al cargar un archivo de pista GPX
- New: Allow displaying several tracks at once
- New: D/T symbols for cache icons (optional)
- New: Option to check for missing routing data for current viewport
- New: Theme legend for Elevate, Elements and Freizeitkarte themes
- Fix: Reenable routing with external BRouter app in version 1.6.3
- Fix: Avoid map duplication by map downloader in certain conditions

### Lista de cachés
- Nuevo: opción para seleccionar los siguientes 20 cachés
- Nuevo: Resumen de atributos (ver Gestión de cachés => Resumen de atributos)
- Nuevo: Añadir importación de listas de marcadores (solo para GC premium)
- Nuevo: Invertir orden al hacer mantener pulsado la barra de ordenación
- Change: Also perform automatic sorting by distance for lists containing cache series with more than 50 caches (up to 500)
- Fix: Use a shorter timeout for fast scrolling mechanism for less interference with other layout elements

### Detalles del caché
- Nuevo: Pasar las coordenadas del caché actual al geochecker (si es soportado por el geochecker)
- New: Colored attribute icons (following attribute groups)
- Fix: Problem opening pictures from gallery tab in external apps on some Samsung devices
- Fix: Missing log count (website change)

### Otro
- Nuevo: Cargar rápidamente los geocódigos del texto del portapapeles en la búsqueda de la pantalla de inicio
- New: Added support for user-defined log templates
- Nuevo: Hacer Ajustes => Ver Configuración filtrable
- New: Enable search in preferences
- New: Added GC Wizard to useful apps list
- New: Attributes filter: Allow selecting from which connectors attributes are shown
- New: Option to limit distance in nearby search (see Settings => Services)
- Change: Removed barcode scanner from useful apps list and from mainscreen
- Change: Removed BRouter from useful apps list (you can still use both external and internal navigation)
- Fix: Avoid repeated update checks for maps/routing tiles with interval=0
- Fix: Optimize support to autofill passwords from external password store apps in settings
- Fix: Enable tooltips for systems running on Android below version 8
- Refactored settings to meet current Android specifications
- Updated MapsWithMe API

