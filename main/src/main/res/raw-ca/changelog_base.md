### UnifiedMap roadmap & "old" maps deprecation notice
c:geo has an all-new map implementation called "UnifiedMap" since some time, which will ultimately replace the old implementations of Google Maps and Mapsforge (OpenStreetMap). This is a deprecation notice to inform you about the further roadmap.

UnifiedMap got published about a year ago. It still supports Google Maps and OpenStreetMap (online + offline), but in a completely reworked technical way, and with a lot of exciting new features that the "old" maps do not support, some of which are
- Map rotation for OpenStreetMap based maps (online + offline)
- Cluster popup for Google Maps
- Hide map sources you don't need
- Elevation chart for routes and tracks
- Switch between lists directly from map
- "Driving mode" for OpenStreetMap based maps

UnfiedMap has proven to be stable since quite some time, thus we will remove the old map implementations to reduce the efforts for maintaining c:geo.

Roadmap:
- "Old" maps are in deprecation mode now - we won't fix bugs for it anymore.
- UnifiedMap will be made default for all users in fall of 2025.
- "Old" map implementations will be removed in spring 2026.

Until then, you can switch between the different implementations in settings => map sources.

### Mapa
- Novetat: Mostra les tanques geogràfiques per a les etapes de laboratori (UnifiedMap): activeu "Cercles" a la configuració ràpida del mapa per mostrar-los
- Novetat: opció per establir cercles amb radi individual als punts de referència (opció del menú contextual "geotanca")
- Correcció: la vista del mapa no s'actualitza quan s'elimina el catxé de la llista mostrada actualment
- Fix: Number of cache in list chooser not updated on changing list contents
- Canviar: manteniu la finestra gràfica actual en el mapa d'una llista, si tots els catxés encaixen a la finestra gràfica actual
- Nou: segueix la meva ubicació al gràfic d'elevació (UnifiedMap)
- Nou: habiliteu les accions "mou a" / "copiar a" per a "mostrar com a llista"
- Nou: admet el tema Elevate Winter al descarregador de mapes
- Nou: ombrejat adaptatiu, mode d'alta qualitat opcional (UnifiedMap Mapsforge)
- Novetat: diàleg de configuració ràpida de rutes/tracks redissenyades
- Nou: toqueu llargament la icona de selecció de mapa per seleccionar el proveïdor anterior (UnifiedMap)
- Nou: permet configurar el nom de visualització per als mapes fora de línia al fitxer complementari (UnifiedMap)
- Nou: toqueu llargament "activa el botó en directe" per carregar xatxés fora de línia

### Detalls del catxé
- Nou: traducció fora de línia del text i de la llista dels registres  (experimental)
- Nou: opció per compartir els catxés amb les dades de l'usuari (coordenades, nota personal)
- Correcció: el servei de veu s'ha interromput en la rotació de la pantalla
- Correcció: detalls del catxé: les llistes de catxés no s'actualitzen després de tocar el nom de la llista i eliminar aquest catxé d'aquesta llista
- Correcció: la nota de l'usuari es perd en actualitzar una lab adventure
- Canvi: els marcadors de posició relacionats amb la data de registre utilitzaran la data escollida en lloc de la data actual
- Nou: redueix les entrades de registre llargues per defecte

### Wherigo player
- Nou: el Wherigo player integrat comprova les credencials que falten
- Canvi: S'ha eliminat l'informe d'error de Wherigo (ja que els errors estan relacionats principalment amb els cartutxos, el propietari del cartutx ha de solucionar-los)
- New: Ability to navigate to a zone using compass
- New: Ability to copy zone center coordinates to clipboard
- New: Set zone center as target when opening map (to get routing and distance info for it)
- Nou: Admet l'obertura de fitxers Wherigo locals
- Change: Long-tap on a zone on map is no longer recognized. This allows users to do other stuff in map zone area available on long-tap, eg: create user-defined cache

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
- New: Added button to delete log templates
- Correcció: la importació del fitxer de mapa local obté un nom de mapa aleatori
