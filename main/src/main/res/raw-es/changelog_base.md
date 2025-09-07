Due to Play Store policies we have updated the Android API level this version of c:geo targets + we have changed some of the screen layout routines. Esto podría producir efectos secundarios no deseados, especialmente en las versiones más recientes de Android. Si experimenta algún problema con esta versión de c:geo, por favor informe ya sea en [GitHub](https://github.com/cgeo/cgeo) o por correo electrónico a [support@cgeo.org](mailto:support@cgeo.org)

### UnifiedMap
- Nuevo: Optimización del calculo de rutas de cachés
- Nuevo: Activar el modo live (en vivo) mantiene visibles los waypoints del objetivo actual
- Nuevo: Un toque largo en la línea de navegación abre el mapa de terreno (UnifiedMap)
- Nuevo: Mostrar waypoints generados en el mapa

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

### Ejecutador de Wherigos
- Nuevo: Traducción sin conexión para Wherigos
- Nuevo: Manejo de botones mejorado

### General
- Nuevo: Opción de compartir después de registrar un caché
- Cambio: No mostrar las opciones "Necesita atención del propietario" o "Necesita atención del revisor" para cachés propios
- Corregido: Restaurar una copia de seguridad puede duplicar archivos de almacenamiento interno y copias de seguridad posteriores
- Cambio: Referencias eliminadas a Twitter
- Nuevo: Borrar archivos huérfanos al limpiar y restaurar la copia de seguridad
- Nuevo: Advertencia al intentar añadir demasiados cachés a una lista de marcadores
- Nuevo: Opción de añadir/no añadir una lista a seguimiento
- New: Offer offline translation with Google Translate or DeepL apps (if installed)
