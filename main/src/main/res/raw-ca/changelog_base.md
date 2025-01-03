Nou: Wherigo player integrat (beta): vegeu l'entrada del menú a la pantalla d'inici.<br> (És possible que vulgueu [configurar un element d'inici ràpid](cgeo-setting://quicklaunchitems_sorted) o [personalitzar la navegació inferior](cgeo-setting://custombnitem) per a un accés més fàcil, primer heu d'activar la configuració ampliada.)

### Mapa
- Nou: emmagatzema el tema del mapa per proveïdor de mosaic (UnifiedMap)
- Nou: ressalteu el catxé/punt d'interés seleccionat (UnifiedMap)
- Nou: afegiu un separador entre fonts de mapes fora de línia i en línia
- Nou: Admet Mapsforge com a alternativa a VTM a UnifiedMap, vegeu [Configuració => Fonts del mapa => Mapa unificat](cgeo-setting://useMapsforgeInUnifiedMap)
- Canvi: "Mostra el gràfic d'elevació" s'ha mogut al menú de toc llarg (UnifiedMap)
- Canvi: utilitzeu el nou algorisme d'ombrejat per als mapes fora de línia de Mapsforge
- Nou: suport d'ombrejat per als mapes fora de línia UnifiedMap Mapsforge
- Nou: suport d'ombrejat per als mapes UnifiedMap VTM (requereix connexió en línia)
- Solució: la cerca d'adreces no té en compte el mode en directe (UnifiedMap)
- Canvi: "seguiu la meva ubicació" s'ha mogut al mapa, donant més espai al botó "mode en directe"
- Canvi: feu que el pin de premsa llarga sigui més semblant a c:geo
- Canvi: les funcions de gestió de dades fora de línia (descàrrega de mapes, comproveu si no hi ha dades d'itineraris o d'ombra) s'han mogut al menú de selecció de mapes => "Gestiona les dades fora de línia"
- Fix: Map not updating changed caches

### Detalls del catxé
- Nou: les variables encara no existents utilitzades a la projecció es creen a la llista de variables
- Nou: permeten nombres enters grans a les fórmules
- Nou: Admet més constel·lacions per a variables en fórmules
- Correcció: diverses imatges a la nota personal no s'afegeixen a la pestanya d'imatges
- Correcció: maneig de projeccions en waypoints i notes personals
- Nou: un toc llarg a la data al registre recupera la data del registre anterior
- Solució: el restabliment del catxé a les coordenades originals no elimina la marca "coordenades canviades"
- Nou: confirmeu la sobreescritura del registre al registre ràpid fora de línia
- Nou: actualitzeu l'estat del catxé en enviar un registre
- Nou: visualització de la font HTML amb colors dels detalls del catxé
- Fix: checksum(0) returning wrong value
- Fix: Editing logs removes "friends" status

### General
- Canvi: utilitzeu l'elevació sobre el nivell mitjà del mar (si està disponible, només Android 14+)
- Nou: permet múltiples nivells de jerarquia a les llistes de catxés
- Novetat: icones dedicades per als tipus de trobada blockparty i HQ de geocaching.com
- Nou: defineix la mida d'imatge preferida per a les imatges carregades des del catxé i els elements de seguiment de geocaching.com
- Correcció: "Obre al navegador" no funciona per als registres rastrejables
- Novetat: opció per gestionar els fitxers descarregats (mapes, temes, dades d'itineraris i ombrejats)
- Nou: opció per eliminar un catxé de totes les llistes (= marcar-la com a suprimir)
- Correcció: restableix les coordenades no detectades per c:geo per a els catxés no desats
- Nou: permet esborrar el filtre si no s'emmagatzema cap filtre amb nom
- Correcció: la confirmació de la "Llista buida" apareix quan s'inicia una descàrrega de pocket query a la llista acabada de crear
- Canvi: Als catxés de propietat amb registres fora de línia mostren el marcador de registre fora de línia
- Nou: format de data configurable (p. ex.: registres de catxé), vegeu [Configuració => Aparença => Format de data](cgeo-settings://short_date_format)
- Nou: apunta la informació del connector a la pantalla d'inici a la pantalla de preferències específiques del connector
- Nou: emojis addicionals per a les icones del catxé
- Canvi: el filtre de tipus de catxés "Especials" inclou trobades de tipus mega, giga, community celebration, HQ celebration, block party i maze
- Canvi: el filtre de tipus de catxé "Altres" inclou GCHQ, APE i tipus desconeguts
- Fix: History length and proximity settings sharing slider values
- Fix: Trackable log page showing time/coordinate input fields for trackables not supporting this
- Fix: Some crashes
