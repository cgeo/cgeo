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
- Change: Make long-press pin more c:geo-like

### Detalls del catxé
- Nou: les variables encara no existents utilitzades a la projecció es creen a la llista de variables
- Nou: permeten nombres enters grans a les fórmules
- Nou: Admet més constel·lacions per a variables en fórmules
- Correcció: diverses imatges a la nota personal no s'afegeixen a la pestanya d'imatges
- Correcció: maneig de projeccions en waypoints i notes personals
- Nou: un toc llarg a la data al registre recupera la data del registre anterior
- Solució: el restabliment del catxé a les coordenades originals no elimina la marca "coordenades canviades"
- Nou: confirmeu la sobreescritura del registre al registre ràpid fora de línia
- New: Update cache status on sending a log
- New: Colored HTML source view of cache details

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
- Fix: "Empty list" confirmation popping up when starting a pocket query download in newly created list
- Change: Owned caches with offline logs show offline log marker
- New: Configurable date format (eg.: cache logs), see [Settings => Appearance => Date format](cgeo-settings://short_date_format)
- New: Point connector info on home screen to connector-specific preference screen
