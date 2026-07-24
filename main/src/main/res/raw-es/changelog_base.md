### Notas de la actualización

**De borde a borde**

Debido a las políticas de Play Store hemos actualizado la versión de la API de Android en esta versión de c:geo targets + hemos cambiado algunas de las rutas de diseño de pantalla. Esto podría producir efectos secundarios no deseados, especialmente en las versiones más recientes de Android. Si experimenta algún problema con esta versión de c:geo, por favor informe ya sea en [GitHub](https://github.com/cgeo/cgeo) o por correo electrónico a [support@cgeo.org](mailto:support@cgeo.org)

**Mapas antiguos**

Como se anunció con las actualizaciones del 2025.07.17 y 2025.12.01, finalmente hemos eliminado los mapas antiguos. Cambiarás al nuevo UnifiedMap automáticamente y no deberías notar diferencias excepto un par de nuevas opciones, algunas de las cuales son
- Mapa de rotación para mapas basados en OpenStreetMap (online + offline)
- Popup de cluster para Google Maps
- Ocultar fuentes de mapa que no necesitas
- Gráfico de reconocimiento de rutas y pistas
- Cambiar entre listas directamente desde el mapa
- "Modo de conducción" para mapas basados en OpenStreetMap
- Mantén pulsado en los tracks/rutas individuales para ver más opciones

### UnifiedMap
- Nuevo: Optimización del calculo de rutas de cachés
- Nuevo: Activar el modo live (en vivo) mantiene visibles los waypoints del objetivo actual
- Nuevo: Un toque largo en la línea de navegación abre el mapa de terreno (UnifiedMap)
- Nuevo: Mostrar waypoints generados en el mapa
- Nuevo: Descargar cachés ordenados por distancia
- Corregido: Duplicación de elementos de ruta individuales
- Nuevo: Soporte para el tema Motorider (solo VTM)
- Nuevo: proveedor de baldosas NoMap (no mostrar mapa, solo cachés etc.)
- Cambio: Distancia máxima para conectar puntos en el historial bajados a 500m (configurable)
- Nuevo: Ahora se permite importar archivos KML como rutas (ej: Itinerario de rastreables)
- Nuevo: Ahora se puede definir el icono del caché aunque aún no esté almacenado
- Nuevo: Caja de información para los gráficos de elevación mostrando distancia restante en subida y en descenso
- Nuevo: Mostrar coordenadas de los puntos de referencia en el pop-up
- Corregido: Los ajustes de búsqueda rápida del mapa podrán mostrar los botones "1"/"2" para perfiles de enrutamiento vacíos después de cambiar de idioma
- Nuevo: Cálculo de los datos de elevación faltantes en rutas importadas (Si se descargan los datos de elevación)
- Corregido: El descargador de teselas ya no para bajo ciertas condiciones (OpenStreetMap mapas online)
- Nuevo: Marcadores de cachés condicionales
- Nuevo: Mostrar pista de navegación (flecha + distancia)

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
- Nuevo: Visualiza las coordenadas calculadas que exceden los límites en la lista de puntos de referencia
- Nuevo: Opción de marcar los puntos de referencia como visitados en la lista de puntos de referencia
- Nuevo: Elementos de relleno para registro de rastreables (Nombre, GC, usuario)
- Cambio: Eliminado el enlace al ejecutador desfasado de WhereYouGo. El ejecutador de wherigos integrado se usará por defecto.
- Corregido: El botón faltante de activación/desactivación del modo guiado en el calculador de puntos de referencia
- Nuevo: Agregadas funciones con soporte de intervalo: sum/suma, min/mínimo, max/máximo, cnt/cuenta, med/media, multiplicar/produto/prod
- Corregido: El incorrecto manejo del estado de DNF para las plataformas de opencaching
- Nuevo: Borrar registro sin conexión después de fusionar con el log en línea
- Nuevo: Mostrar confirmación cuando se eliminan cachés con registros sin conexión
- Nuevo: Mostrar confirmación cuando se borran todos los cachés de la lista "Todos"
- Nuevo: Se permite el formato Markdown para el texto de la descripción en cachés definidos por el usuario
- Cambio: Almacena el caché antes de añadir la imagen del usuario
- Corregido: Fallo en las imágenes que cargan directamente incrustadas en la descripción
- Nuevo: Muestra tus propios favoritos en la vista del registro (Geocaching.com + registros sin conexión)

### Ejecutador de Wherigos
- Nuevo: Traducción sin conexión para Wherigos
- Nuevo: Manejo de botones mejorado
- Nuevo: Estado del autoguardado
- Nuevo: Opción de crear un acceso directo al ejecutador de wherigos en la pantalla principal del teléfono

### General
- Nuevo: Opción de compartir después de registrar un caché
- Cambio: No mostrar las opciones "Necesita atención del propietario" o "Necesita atención del revisor" para cachés propios
- Corregido: Restaurar una copia de seguridad puede duplicar archivos de almacenamiento interno y copias de seguridad posteriores
- Cambio: Referencias eliminadas a Twitter
- Nuevo: Borrar archivos huérfanos al limpiar y restaurar la copia de seguridad
- Nuevo: Advertencia al intentar añadir demasiados cachés a una lista de marcadores
- Nuevo: Opción de añadir/no añadir una lista a seguimiento
- Nuevo: Ofrecer traducción sin conexión con las aplicaciones Google Translate o DeepL (si están instaladas)
- Nuevo: Borrar elementos del historial de búsqueda
- Cambio: GCVote eliminado (servicio interrumpido)
- Nuevo: Barra de herramientas coloreada la página de detalles del caché
- Nuevo: Selecciona múltiples listas de marcadores / pocket queries para descargar
- Nuevo: Previsualiza listas de marcadores
- Cambio: Incremento de la versión mínima requerida de Android a Android 8
- Nuevo: Botones rápidos predeterminados para nuevas instalaciones
- Corregido: Títulos cortados en el diálogo de inserción de datos
- Corregido: Notificación de actualización de la versión nocturna de la variante FOSS apuntaba al APK normal
- Nuevo: Opción "Ignorar año" para los filtros de fechas
- Nuevo: URI remoto ahora es pulsable en las descargas pendientes
- Cambio: Uso de los ajustes del sistema como tema predeterminado en nuevas instalaciones
- Novo: Exportación GPX: Añadidas anotaciones GSAK "Lat/LonBeforeCorrect" en la exportación de puntos adicionales originales
- Nuevo: Mostrar barra para deshacer cuando se borran cachés en una lista desde un mapa
- Corregido: Detención en el filtro de porcentaje de favoritos
- Nuevo: Ahora es más fácil utilizar listas simples como listas principales
- Cambio: Uso de la zona horaria local (de dispositivo, no de evento) para las entradas del calendario (en lugar de UTC)
- Corregido: Algunos textos ignoraban el cambio de idioma
- Corregir: "Usar ajustes imperiales" no inicializaba correctamente en instalaciones nuevas
- Cambio: Bergamot módulo de traducción sin conexión de código abierto reemplazando al traductor de Google ML Kit de código cerrado
- Cambio: Nuevo selector de emojis
