### Aviso de deprecación de mapas "antiguos" & UnifiedMap hoja de ruta
c:geo tiene una implementación de mapa completamente nueva llamada "UnifiedMap" desde un tiempo, que reemplazará en última instancia las viejas implementaciones de Google Maps y Mapsforge (OpenStreetMap). Este es un aviso de deprecación para informarle acerca de la nueva hoja de ruta.

UnifiedMap se publicó hace aproximadamente un año. Todavía soporta Google Maps y OpenStreetMap (online + offline), pero de una manera técnica completamente reelaborada y con muchas nuevas características emocionantes que los "antiguos" mapas no soportan, algunas de las cuales son
- Mapa de rotación para mapas basados en OpenStreetMap (online + offline)
- Popup de cluster para Google Maps
- Ocultar fuentes de mapa que no necesitas
- Gráfico de reconocimiento de rutas y pistas
- Cambiar entre listas directamente desde el mapa
- "Modo de conducción" para mapas basados en OpenStreetMap

UnfiedMap ha demostrado ser estable desde hace algún tiempo, por lo que eliminaremos las implementaciones de mapas antiguas para reducir los esfuerzos de mantenimiento de c:geo.

Hoja de Ruta:
- Los mapas "Antiguos" están ahora en modo de deprecación - ya no arreglaremos errores para ellos.
- UnifiedMap será la elección por defecto para todos los usuarios en otoño de 2025.
- Las implementaciones de mapas "Antiguos" se eliminarán en la primavera de 2026.

Hasta entonces, puede cambiar entre las diferentes implementaciones en ajustes => fuentes de mapas.

### UnifiedMap
- Nuevo: Mostrar perímetros de respuesta para etapas de Adventure Labs (UnifiedMap) - habilite "Círculos" en los ajustes rápidos del mapa para mostrarlos
- Nuevo: Opción para establecer círculos con radio individual para puntos adicionales (opción de menú contextual "perímetro de respuesta")
- Corregido: La vista del mapa no se actualiza al eliminar la caché de la lista actual
- Corregido: Número de caché en el selector de lista no actualizaba al cambiar el contenido de la lista
- Cambio: Se mantendrá la vista actual al mapear una lista, si todos los cachés entran en la vista actual
- Nuevo: Sigue mi ubicación en el gráfico de elevación (UnifiedMap)
- Nuevo: Activar las acciones "mover a" / "copiar a" para "mostrar como lista"
- Nuevo: Soporte para el tema Elevate Winter en el descargador de mapas
- Nuevo: Sombreado de relieve adaptativo, modo opcional de alta calidad (UnifiedMap Mapsforge)
- Nuevo: Rediseño del diálogo de ajustes rápidos de rutas/pistas
- Nuevo: Pulsación larga en el icono de selección de mapa para seleccionar el proveedor de teselas anterior (UnifiedMap)
- Nuevo: Permite configurar el nombre de los mapas sin conexión en el archivo complementario (UnifiedMap)
- Nuevo: Pulsación larga en "botón activar en vivo" para cargar cachés sin conexión
- Nuevo: Sombreado de relieve sin conexión para UnifiedMap (variante VTM)
- Nuevo: Soporte para mapas en segundo plano (UnifiedMap)
- Corregido: Iconos compactos que no regresan a su tamaño grande al acercarse de modo automático (UnifiedMap)
- Nuevo: Acciones con pulsaciones largas en la hoja de información del caché: código GC, título del caché, coordenadas, notas personales/pista
- Cambio: Cambia el toque largo en la hoja de información del caché para el selector de emojis por un toque corto para resolver la colisión

### Detalles del caché
- Nuevo: Traducción sin conexión del texto de los listados y registros (experimental)
- Nuevo: Opción para compartir cachés con datos de usuario (coordenadas, notas personales)
- Corregido: Servicio de voz interrumpido al rotar la pantalla
- Corregido: Detalles del caché: Listas de cachés no actualizadas después de tocar en el nombre de la lista y eliminando ese caché de esa lista
- Corregido: La nota del usuario se pierde al actualizar un Adventure Lab
- Cambio: Los marcadores de posición relacionados con fecha de registro usarán la fecha elegida en lugar de la fecha actual
- Nuevo: Las entradas de registro largas se colapsarán por defecto

### Ejecutador de Wherigos
- Nuevo: Comprobación de credenciales faltantes integrada en el Ejecutador de Wherigos
- Cambio: Eliminado informe de error Wherigo (ya que los errores están en su mayoría relacionados con cartuchos, necesitan ser corregidos por el propietario de cartucho)
- Nuevo: Posibilidad de navegar a una zona usando la brújula
- Nuevo: Posibilidad de copiar las coordenadas del centro de zona al portapapeles
- Nuevo: Establecer el centro de zona como objetivo al abrir el mapa (para obtener información de ruta e información de distancia hacia él)
- Nuevo: Soporte para abrir archivos locales de Wherigo
- Cambio: Ya no se reconocen pulsaciones largas en una zona del mapa. Esto permite a los usuarios hacer otras cosas en el área de la zona del mapa disponible con toques largos, por ejemplo: crear un caché definido por el usuario
- Nuevo: Mostrar advertencia si wherigo.com informa de que falta el EULA (lo que conduce a una descarga fallida de cartucho)

### General
- Nuevo: Página de búsqueda rediseñada
- Nuevo: Filtro de recuento de inventario
- Nuevo: Soporte para coordenadas en formato DD, DDDDDD
- Nuevo: Mostrar el último nombre de filtro usado en el filtro de diálogo
- Nuevo: Calculadora de coordenadas: Función para reemplazar "x" con el símbolo de multiplicación
- Corregido: Altitud incorrecta (no usando la media sobre el nivel del mar)
- Corregido: El ajuste del límite de distancia cercano no funcionaba correctamente para valores pequeños
- Corregido: Ordenar las listas de cachés por distancia descendente no funcionaba correctamente
- Corregido: Cachés Adventure Lab excluidos en el filtro D/T incluso con "incluir incierto" activo
- Corregido: Problemas de color con los iconos de menú en modo claro
- Nuevo: Añadir "Eliminar eventos pasados" a la lista "todos"
- Nuevo: Mostrar conector para "cachés definidos por el usuario" como activos en el filtro de origen
- Nuevo: Exporte de GPX: exportación de registros / rastreables ahora es opcional
- Nuevo: Botón añadido para eliminar plantillas de registro
- Corregido: Importar archivo de mapa local obtiene un nombre aleatorio para el mapa
- Corregido: El descargador de mapas ofrece archivos rotos (0 bytes) para descargar
- Nuevo: Se añadieron mapeados para algunos tipos de cachés OC faltantes
- Nuevo: Mover listas "usadas recientemente" en el diálogo de selección a la parte superior pulsando el botón "usado recientemente"
- Nuevo: Compartir lista de geocódigos de la lista de cachés
- Cambio: "Navegación (coche)" etc. usará el parámetro "q=" en lugar del parámetro "ll=" obsoleto
