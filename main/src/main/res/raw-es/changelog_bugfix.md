##
¡Es hora de actualizar! Si todavía estás usando Android 7 o anteriores, ¡esta podría ser la última actualización de c:geo para ti! Con nuestra próxima versión de c:geo dejaremos de dar soporte a Android 5-7 para reducir nuestra carga de mantenimiento y poder actualizar algunos componentes externos utilizados por c:geo que estábamos retrasando. Seguiremos soportando Android 8 hasta Android 16 (y versiones más recientes cuando sean publicadas), un espacio de más de ocho años de historia de Android.

- Corregido: Retraso en la apertura en la ventana de Cache/punto de referencia en algunos dispositivos
- Corregido: Editar la descripción del caché no soportaba copiar y pegar
- Corregido: Algunos fallos y "app no responde"
- Corregido: Fallo al eliminar el registro de un rastreable (cambio del sitio web)

##
- Corregido: Borrado de imágenes de registro roto (cambio en el sitio web)
- Cambio: Unificados los botones de carga de rutas y de rutas individuales
- Corregido: Atributos de caché no detectados correctamente bajo ciertas condiciones
- Corregido: Registrar cachés (Cambio en el sitio web)
- Corregido: Registrar rastreables (Cambio en el sitio web)

##
- Corregido: Importaciónes de Pocket Query rotas (cambio en el sitio web)

##
- Corregido: Detención al acceder a rutas
- Corregido: Detención en la página de puntos de referencia
- Cambio: Buscar por "caches propios" empieza con filtros reseteados
- Corregido: Las etapas de lab no guardadas perdían "visitado" al refrescar
- Corregido: Solicitud recurrente a actualizaciones de la fuente de teselas
- Corregido: Localización aleatoria al mapear una lista (Google Maps)

##
- Corregido: Detención en la hoja de información de cachés
- Corregido: Los cartuchos de Wherigo no se podían descargar (cambio en el sitio web)

##
 - Cambio: Los archivos Wherigo no se pueden descargar actualmente. Mostrar instrucciones de mitigación
 - Corregido: La razón al borrar registros no imponía el límite de caracteres
 - Nuevo: Registro extendido de fallos en el gestor de descargas
 - Corregido: La hoja de información de un Waypoint podía volverse demasiado larga, volviendo los botones inalcanzables
 - Corregido: Parte de la información de ubicación se truncaba
 - Corregido: La ruta interna no funcionaba, solo se mostraba la línea recta
 - Corregido: Errores al crear una carpeta

Nota: Si está utilizando el enrutado interno, necesitará ejecutar el siguiente paso una vez instalada esta versión: Ir a la pantalla de inicio de c:geo, abrir "Administrar datos sin conexión" - "Actualizar datos de enrutamiento" y dejar que c:geo instale los archivos actualizados. (Razón: La estructura del archivo de datos de enrutamiento BRouter ha cambiado y todos los archivos de datos de enrutamiento deben cumplir con la misma versión.)

##
- Corregido: Fallo al extraer la ubicación del caché para ciertos idiomas del sitio web
- Corregido: Fallo al abrir rastreables desde lista de seguimiento
- Corregido: El teclado bloqueaba en ocasiones la selección de la lista
- Corregido: Proveedor de teselas definido por el usuario no soportaba parámetros adicionales en la URL
- Corregido: Fallo de carga en el Inventario / Rastreables de un caché
- Cambio: Actualizado el agente de usuario interno para abordar algunos problemas de descarga
- Corregido: Ver detalles de un rastreable lo eliminaba del inventario del caché

##
- Corregido: Diálogo de descarga de traducción sin conexión se mostraba en las instalaciones que no permitían usarlo
- Corregido: Cambio de formato de coordenadas en un caché / hoja de información de waypoint
- Corregido: Fecha del registro cortada en la lista de registros (dependiendo del formato de la fecha y tamaño de la fuente)
- Corregido: Horas del evento no detectadas en ciertas condiciones
- Corregido: Enlaces en la descripción no pulsables bajo algunas condiciones
- Corregido: Las acciones de registro de los rastreables se mezclaban en ocasiones

##
- Cambio: El número máximo de rastreables visitando por registro de caché se reduce a 100 (por petición de geocaching.com para reducir la carga de su servidor causada por amantes extremos de los rastreables)
- Corregido: Posibles excepciones de seguridad cuando el usuario no ha concedido ciertos permisos (por ejemplo: notificaciones)
- Corregido: Círculos de cachés incompletos en niveles de zoom bajos (solo VTM)
- Corregido: Error al recargar waypoints en ciertas condiciones de carga
- Corregido: El filtro de fecha de eventos no funcionaba bajo ciertas condiciones
- Corregido: Límite máximo de líneas de registro no funcionaba de forma fiable en la configuración "ilimitada"
- Corregido: Detención al abrir el mapa bajo ciertas condiciones
- Corregido: No se mostrará el mapa si wherigo no tiene zonas visibles
- Corregido: Detención en la pestaña de imágenes de detalles del caché bajo ciertas condiciones
- Corregido: Búsquedas de mapas con coordenadas no válidas
- Corregido: Algunas traducciones no respetaban la configuración de idioma interna de c:geo

##
- Cambio: UnifiedMap establecido como mapa predeterminado para todo el mundo (como parte de nuestro mapa de ruta a UnifiedMap) Puedes volver a cambiar en "ajustes" - "fuentes del mapa" por el momento. La eliminación de los mapas de legado está planeada para la primavera de 2026 en nuestras actualizaciones habituales.
- Corregido: La casilla de verificación de favoritos se restablecía al volver al reabrir la pantalla de registro sin conexión
- Corregido: El cuadro de entrada del radio del perímetro de respuesta muestra el número decimal
- Corregido: La sincronización de notas personales no funciona
- Cambio: Nuevo icono para la importación de rutas GPX en los ajustes de mapa/configuración rápida de ruta

##
- Corregido: Los valores negativos en el gráfico de elevación no se escalaban
- Corregido: Las coordenadas cerca de 0 se rompían en exportaciones GPX
- Corregido: Algunas detenciones
- Intento de corregir: ANR al iniciar
- Intento de corregir: Faltan datos de cachés en el mapa en vivo

##
- Corrección: Error en la búsqueda por palabras clave
- Corregido: Error en el mapa
- Corregir: El texto de la pista no se podía seleccionar
- Corregir: Varios problemas de Wherigo

##
- Corregido: Cifrar/descifrar una pista necesita un toque extra inicialmente
- Corregido: Wherigo se detenía al leer las partidas guardadas antiguas
- Corregido: El registro desde dentro de c:geo no se guarbada a veces
- Corregido: Faltaba actualización de datos en vivo para los cachés encontrados & archivados
- Corregido: Los Waypoints en el mapa sin conexión no se mostraban a veces

##
- Corregido: Pistas de caché sin cifrar (cambio del sitio web)
- Corregido: Los Lab Adventures no cargaban en la app (cambio de sitio web, necesitarás actualizar los Adventure Lab almacenados para poder verlos desde c:geo otra vez)
- Solucionado: VTM UnifiedMap: Alternar edificios 3D no funciona para mapas combinados
- Corregido: Traducción sin conexión: Listado de idiomas detectado a veces como --

##
- Corregido: Detención de la aplicación en el módulo de traducción
- Corregido: Fallo en la detección de inicio de sesión (cambio del sitio web)
- Corregido: Aplicación detenida al recuperar el cartucho de Wherigo
- Corregido: "Cargar más" no respetaba los filtros sin conexión

##
- Corregido: Inventario de TB(s) no cargado mientras se registra un caché

##
- Corregido: Migración de cachés definidos por el usuario durante el inicio de c:geo falla => eliminado por el momento
- Corregido: Las tareas finalizadas del Wherigo no están marcadas como terminadas o fallaron































