### Full de ruta de UnifiedMap & avís de desús de mapes "vells".
c:geo té una implementació de mapes totalment nova anomenada "UnifiedMap" des de fa temps, que finalment substituirà les antigues implementacions de Google Maps i Mapsforge (OpenStreetMap). Aquest és un avís d'abandonament per informar-vos sobre el full de ruta posterior.

UnifiedMap es va publicar fa aproximadament un any. Encara és compatible amb Google Maps i OpenStreetMap (en línia + fora de línia), però d'una manera tècnica completament reelaborada, i amb moltes novetats interessants que els mapes "vells" no admeten, algunes de les quals són
- Rotació de mapes per a mapes basats en OpenStreetMap (en línia + fora de línia)
- Popup de clúster per a Google Maps
- Amaga les fonts de mapes que no necessites
- Carta de cotes de rutes i tracks
- Canvia entre llistes directament des del mapa
- "Mode de conducció" per a mapes basats en OpenStreetMap

UnfiedMap ha demostrat ser estable des de fa força temps, per la qual cosa eliminarem les implementacions de mapes antigues per reduir els esforços per mantenir c:geo.

Full de ruta:
- Els mapes "vells" estan ara en mode obsolet; ja no els arreglarem els errors.
- UnifiedMap es convertirà per defecte per a tots els usuaris a la tardor del 2025.
- Les implementacions de mapes "antigues" s'eliminaran a la primavera de 2026.

Fins aleshores, podeu canviar entre les diferents implementacions a la configuració => fonts de mapes.

### Mapa
- Novetat: Mostra les tanques geogràfiques per a les etapes de laboratori (UnifiedMap): activeu "Cercles" a la configuració ràpida del mapa per mostrar-los
- Novetat: opció per establir cercles amb radi individual als punts de referència (opció del menú contextual "geotanca")
- Correcció: la vista del mapa no s'actualitza quan s'elimina el catxé de la llista mostrada actualment
- Correcció: el nombre de catxés al selector de llista no s'ha actualitzat en canviar el contingut de la llista
- Canviar: manteniu la finestra gràfica actual en el mapa d'una llista, si tots els catxés encaixen a la finestra gràfica actual
- Nou: segueix la meva ubicació al gràfic d'elevació (UnifiedMap)
- Nou: habiliteu les accions "mou a" / "copiar a" per a "mostrar com a llista"
- Nou: admet el tema Elevate Winter al descarregador de mapes
- Nou: ombrejat adaptatiu, mode d'alta qualitat opcional (UnifiedMap Mapsforge)
- Novetat: diàleg de configuració ràpida de rutes/tracks redissenyades
- Nou: toqueu llargament la icona de selecció de mapa per seleccionar el proveïdor anterior (UnifiedMap)
- Nou: permet configurar el nom de visualització per als mapes fora de línia al fitxer complementari (UnifiedMap)
- Nou: toqueu llargament "activa el botó en directe" per carregar xatxés fora de línia
- Nou: ombrejat fora de línia per a UnifiedMap (variant VTM)
- Nou: suport per a mapes de fons (només UnifiedMap VTM)

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
