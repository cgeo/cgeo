Debido a las políticas de Play Store hemos actualizado el nivel de la API de Android esta versión de los objetivos de c:geo. Esto podría producir efectos secundarios no deseados, especialmente en las versiones más recientes de Android. Si experimenta algún problema con esta versión de c:geo, por favor informe ya sea en [GitHub](https://github.com/cgeo/cgeo) o por correo electrónico a [support@cgeo.org](mailto:support@cgeo.org)

### Mapa
- Nuevo: Optimización del calculo de rutas de cachés
- Nuevo: Activar el modo live (en vivo) mantiene visibles los waypoints del objetivo actual
- Nuevo: Un toque largo en la línea de navegación abre el mapa de terreno (UnifiedMap)
- Nuevo: Mostrar waypoints generados en el mapa

### Detalles del caché
- Nuevo: Detectar caracteres adicionales en fórmulas: –, ⋅, ×
- Nuevo: Se preservará la hora y día de los registros propios al actualizar un caché
- Nuevo: Opcional mini vista de la brújula (ver configuración => detalles del caché => Mostrar dirección en los detalles del caché)
- New: Show owners' logs on "friends/own" tab
- Change: "Friends/own" tab shows log counts for that tab instead of global counters
- Change: Improved header in variable and waypoint tabs
- Fix: Two "delete log" items shown
- Fix: c:geo crashing in cache details when rotating screen
- Change: More compact layout for "adding new waypoint"
- New: Option to load images for geocaching.com caches in "unchanged" size
- New: Variables view can be filtered

### Wherigo player
- New: Offline translation for Wherigos
- New: Improved button handling

### General
- New: Share option after logging a cache
- Change: Do not show "needs maintenance" or "needs archived" options for own caches
- Fix: Restoring a backup may duplicate track files in internal storage and subsequent backups
- Change: Removed references to Twitter
- New: Delete orphaned trackfiles on clean up and restore backup
- New: Warning on trying to add too many caches to a bookmark list
- New: Watch/unwatch list functions

