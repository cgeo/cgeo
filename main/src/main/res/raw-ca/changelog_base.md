(només per a la nit: s'ha eliminat temporalment el bàner "nocturn" del logotip mentre s'ajustava el disseny)

### Full de ruta de UnifiedMap & avís de desús de mapes "vells".
c:geo té una implementació de mapes totalment nova anomenada "UnifiedMap" des de fa temps, que finalment substituirà les antigues implementacions de Google Maps i Mapsforge (OpenStreetMap). Aquest és un avís d'abandonament per informar-vos sobre el full de ruta posterior.

UnifiedMap es va publicar fa aproximadament un any. Encara és compatible amb Google Maps i OpenStreetMap (en línia + fora de línia), però d'una manera tècnica completament reelaborada, i amb moltes novetats interessants que els mapes "vells" no admeten, algunes de les quals són
- Map rotation for OpenStreetMap based maps (online + offline)
- Cluster popup for Google Maps
- Hide map sources you don't need
- Elevation chart for routes and tracks
- Switch between lists directly from map
- "Driving mode" for OpenStreetMap based maps

UnfiedMap ha demostrat ser estable des de fa força temps, per la qual cosa eliminarem les implementacions de mapes antigues per reduir els esforços per mantenir c:geo.

Full de ruta:
- "Old" maps are in deprecation mode now - we won't fix bugs for it anymore.
- UnifiedMap will be made default for all users in fall of 2025.
- "Old" map implementations will be removed in spring 2026.

Fins aleshores, podeu canviar entre les diferents implementacions a la configuració => fonts de mapes.

### Mapa
- New: Show geofences for lab stages (UnifiedMap) - enable "Circles" in map quick settings to show them
- New: Option to set circles with individual radius to waypoints ("geofence" context menu option)
- Fix: Map view not updated when removing cache from currently shown list
- Fix: Number of cache in list chooser not updated on changing list contents
- Change: Keep current viewport on mapping a list, if all caches fit into current viewport
- New: Follow my location in elevation chart (UnifiedMap)
- New: Enable "move to" / "copy to" actions for "show as list"
- New: Support Elevate Winter theme in map downloader
- New: Adaptive hillshading, optional high quality mode (UnifiedMap Mapsforge)
- New: Redesigned routes/tracks quick settings dialog
- New: Long tap on map selection icon to select previous tile provider (UnifiedMap)
- New: Allow setting display name for offline maps in companion file (UnifiedMap)
- New: Long tap on "enable live button" to load offline caches
- New: Offline hillshading for UnifiedMap (VTM variant)
- New: Support for background maps (UnifiedMap)

### Detalls del catxé
- Nou: traducció fora de línia del text i de la llista dels registres  (experimental)
- Nou: opció per compartir els catxés amb les dades de l'usuari (coordenades, nota personal)
- Correcció: el servei de veu s'ha interromput en la rotació de la pantalla
- Correcció: detalls del catxé: les llistes de catxés no s'actualitzen després de tocar el nom de la llista i eliminar aquest catxé d'aquesta llista
- Correcció: la nota de l'usuari es perd en actualitzar una lab adventure
- Canvi: els marcadors de posició relacionats amb la data de registre utilitzaran la data escollida en lloc de la data actual
- Nou: redueix les entrades de registre llargues per defecte

### Wherigo Player
- Nou: el Wherigo player integrat comprova les credencials que falten
- Canvi: S'ha eliminat l'informe d'error de Wherigo (ja que els errors estan relacionats principalment amb els cartutxos, el propietari del cartutx ha de solucionar-los)
- Nou: possibilitat de navegar a una zona amb la brúixola
- Nou: possibilitat de copiar les coordenades del centre de la zona al porta-retalls
- Nou: estableix el centre de la zona com a objectiu en obrir el mapa (per obtenir informació sobre l'itinerari i la distància)
- Nou: Admet l'obertura de fitxers Wherigo locals
- Canvi: el toc llarg en una zona del mapa ja no es reconeix. Això permet als usuaris fer altres coses a l'àrea de la zona del mapa disponible amb un toc llarg, per exemple: crear un catxé definit per l'usuari
- Nou: Mostra un avís si wherigo.com informa que no hi ha CLUF (la qual cosa fa que la descàrrega del cartutx no sigui possible)

### General
- Nou: pàgina de cerca redissenyada
- Nou: filtre de recompte d'inventari
- Nou: suport per a coordenades en format DD, DDDDDDD
- Nou: mostra el nom del darrer filtre utilitzat al quadre de diàleg del filtre
- Novetat: Calculadora de coordenades: funció per substituir "x" pel símbol de multiplicació
- Correcció: altitud incorrecta (no utilitzant la mitjana sobre el nivell del mar)
- Correcció: la configuració del límit de distància propera no funciona correctament per a valors petits
- Correcció: l'ordenació de les llistes de catxés per distància descendent no funciona correctament
- Correcció: El catxés Lab excloses pel filtre D/T fins i tot amb "inclou incert" actiu
- Solució: problemes de color amb les icones del menú en mode de llum
- Nou: afegiu "Elimina els esdeveniments passats" per llistar "tots"
- Novetat: mostra el connector pels "Catxés definits per l'usuari" com a actiu al filtre font
- Novetat: exportació GPX: exportació de registres / rastrejables es fa opcional
- Nou: S'ha afegit un botó per eliminar les plantilles de registre
- Correcció: la importació del fitxer de mapa local obté un nom de mapa aleatori
