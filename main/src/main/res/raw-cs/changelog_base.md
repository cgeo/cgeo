(nightly only: Temporarily removed "nightly" banner from logo while fine-tuning the design)

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
- Fix: Compact icons not returning to large icons on zooming in in auto mode (UnifiedMap)

### Detaily kešky
- Nový: Offline překlad textu a logů (experimentální)
- Novinka: Možnost sdílení kešky s uživatelskými daty (souřadnice, osobní poznámka)
- Oprava: Přerušení hlasové služby při otáčení obrazovky
- Oprava: Podrobnosti o kešce: Seznamy kešek se neaktualizují po klepnutí na název seznamu a odebrání dané kešky z tohoto seznamu
- Oprava: Uživatelská poznámka se ztratí při obnovení dobrodružství v Lab Adventures
- Změna: zástupné symboly budou namísto aktuálního data používat zvolené datum
- Novinka: Sbalení dloouhých logů je nyní ve výchozím nastavení

### Wherigo přehrávač
- Novinka: Integrovaná kontrola chybějících pověření v přehrávači Wherigo
- Změna: Odstraněno hlášení chyb Wherigo (jako jsou chyby většinou související s cartridgí, musí být opraveno majitelem cartridge)
- Novinka: Schopnost přejít do zóny pomocí kompasu
- Novinka: Možnost kopírovat souřadnice centra zóny do schránky
- Novinka: Nastavit centrum zóny jako cíl při otevření mapy (získat pro něj informace o směrování a vzdálenosti)
- Novinka: Podpora otevírání místních Wherigo souborů
- Změna: Dlouhým poklepáním na zónu na mapě již není rozpoznáno. To umožňuje uživatelům dělat další věci v oblasti mapy dostupné na dlouhém klepnutí, např.: vytvořit uživatelem definovanou kešku
- Novinka: Zobrazí upozornění, pokud na wherigo.com chybí zprávy EULA (což vede k selhávajícímu stahování cartridge)

### Obecné
- Novinka: Přepracovaná stránka vyhledávání
- Novinka: Filtr počtu inventáře
- Novinka: Podpora pro souřadnice ve formátu DD,DDDDDDD
- Novinka: Zobrazení názvu naposledy použitého filtru v dialogovém okně filtru
- Novinka: Souřadnicová kalkulačka: Funkce pro nahrazení symbolu "x" symbolem násobení
- Oprava: Nesprávná nadmořská výška (při použití střední hodnoty nad hladinou moře)
- Oprava: Nastavení omezení vzdálenosti v blízkosti nefunguje správně pro malé hodnoty
- Oprava: Řazení seznamů kešek podle vzdálenosti sestupně nefunguje správně
- Oprava: Lab kešky vyloučené O/T filtrem i s aktivním "zahrnout nejisté"
- Oprava: Problémy s ikonami menu ve světlém režimu
- Novinka: Přidat "Odstranit minulé události" do seznamu "Vše"
- Novinka: Zobrazit konektor pro "uživatelem definované kešky" jako aktivní ve zdrojovém filtru
- Novinka: Export GPX: export záznamů / trasovatelných položek je volitelný
- Novinka: Přidáno tlačítko pro odstranění šablon logů
- Oprava: Importování místního mapového souboru získá náhodný název mapy
- Fix: Map downloader offering broken (0 bytes) files for download
