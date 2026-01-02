Due to Play Store policies we have updated the Android API level this version of c:geo targets + we have changed some of the screen layout routines. Esto podría producir efectos secundarios no deseados, especialmente en las versiones más recientes de Android. Si experimenta algún problema con esta versión de c:geo, por favor informe ya sea en [GitHub](https://github.com/cgeo/cgeo) o por correo electrónico a [support@cgeo.org](mailto:support@cgeo.org)

### UnifiedMap
- Nuevo: Optimización del calculo de rutas de cachés
- Nuevo: Activar el modo live (en vivo) mantiene visibles los waypoints del objetivo actual
- Nuevo: Un toque largo en la línea de navegación abre el mapa de terreno (UnifiedMap)
- Nuevo: Mostrar waypoints generados en el mapa
- New: Download caches ordered by distance
- Fix: Doubling of individual route items
- New: Support for Motorider theme (VTM only)
- New: Support for transparent background display of offline maps (VTM only)
- New: NoMap tile provider (don't show map, just caches etc.)
- Change: Max distance to connect points on history track lowered to 500m (configurable)

### Detalles del caché
- Nuevo: Detectar caracteres adicionales en fórmulas: –, ⋅, ×
- Nuevo: Se preservará la hora y día de los registros propios al actualizar un caché
- Nuevo: Opcional mini vista de la brújula (ver configuración => detalles del caché => Mostrar dirección en los detalles del caché)
- Nuevo: Se mostrarán los registros de los propietarios en la pestaña "amigos/propios"
- Cambio: La pestaña "amigos/propios" muestra el contador de registros para esa pestaña en lugar de los contadores globales
- Cambio: Cabecera mejorada en las pestañas de variables y waypoints
- Corregido: Se mostrarán dos elementos de "borrar registro"
- Corregido: La aplicación de c:geo ya no se detiene en los detalles del caché al girar la pantalla
- Cambio: Diseño más compacto para "añadir nuevo waypoint"
- Nuevo: Opción de cargar imágenes para cachés de geocaching.com en tamaño "sin cambios"
- Nuevo: La vista de variables puede ser filtrada
- New: Visualize calculated coordinates overflow in waypoint list
- New: Menu entry in waypoint list to mark certain waypoint types as visited
- New: Placeholders for trackable logging (geocache name, geocache code, user)
- Change: Removed the link to outdated WhereYouGo player. Integrated Wherigo player is now default for Wherigos.
- Fix: Missing quick toggle in guided mode of waypoint calculator

### Ejecutador de Wherigos
- Nuevo: Traducción sin conexión para Wherigos
- Nuevo: Manejo de botones mejorado
- New: Status auto-save
- New: Option to create shortcout to Wherigo player on your mobile's home screen

### General
- Nuevo: Opción de compartir después de registrar un caché
- Cambio: No mostrar las opciones "Necesita atención del propietario" o "Necesita atención del revisor" para cachés propios
- Corregido: Restaurar una copia de seguridad puede duplicar archivos de almacenamiento interno y copias de seguridad posteriores
- Cambio: Referencias eliminadas a Twitter
- Nuevo: Borrar archivos huérfanos al limpiar y restaurar la copia de seguridad
- Nuevo: Advertencia al intentar añadir demasiados cachés a una lista de marcadores
- Nuevo: Opción de añadir/no añadir una lista a seguimiento
- New: Offer offline translation with Google Translate or DeepL apps (if installed)
- New: Delete items from search history
- Change: Remove GCVote (service discontinued)
- New: Colored toolbar on cache details pages
- New: Select multiple bookmark lists / pocket queries to download
- New: Preview bookmark lists
- Change: Increase minimum required Android version to Android 8
- New: Default quick buttons for new installations
- Fix: Titles in range input dialogs cut off
- Fix: Notification for nightly update points to regular APK even for FOSS variant
- New: "Ignore year" option for date filters
