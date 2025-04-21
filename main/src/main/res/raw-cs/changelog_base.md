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
- Novinka: Zobrazí geofence u zastávek labek (Sjednocená mapa) - povolte "Kruhy" v rychlém nastavení na mapě pro jejich zobrazení
- Novinka: Možnost nastavit kruhy s individuálním poloměrem na trasové body (možnost kontextového menu "Sledovaná oblast")
- Oprava: Zobrazení mapy se neaktualizuje při odebrání kešky z aktuálně zobrazeného seznamu
- Fix: Number of cache in list chooser not updated on changing list contents
- Změna: Ponechat aktuální zobrazení při mapování seznamu, pokud se všechny kešky vejdou do aktuálního zobrazení
- Novinka: Sledování mé polohy v grafu nadmořské výšky (Sjednocená mapa)
- Novinka: Povolit akce "přesunout do" / "kopírovat do" pro "zobrazit jako seznam"
- Novinka: Podpora motivu Elevate Winter ve stahovači map
- Novinka: Adaptivní stínování kopců, volitelný režim vysoké kvality (Sjednocená mapa Mapsforge)
- Novinka: Přepracované dialogové okno rychlého nastavení tras/stop
- Novinka: Dlouhým klepnutím na ikonu výběru mapy vyberete předchozího poskytovatele dlaždic (Sjednocená mapa)
- Novinka: Povolit zobrazení názvu offline mapy v doprovodném souboru (Sjednocená mapa)
- Novinka: Dlouhým klepnutím na "Povolit živé tlačítko" načíst offline kešky
- New: Offline hillshading for UnifiedMap (VTM variant)

### Detaily kešky
- Nový: Offline překlad textu a logů (experimentální)
- Novinka: Možnost sdílení kešky s uživatelskými daty (souřadnice, osobní poznámka)
- Oprava: Přerušení hlasové služby při otáčení obrazovky
- Oprava: Podrobnosti o kešce: Seznamy kešek se neaktualizují po klepnutí na název seznamu a odebrání dané kešky z tohoto seznamu
- Oprava: Uživatelská poznámka se ztratí při obnovení dobrodružství v Lab Adventures
- Změna: zástupné symboly budou namísto aktuálního data používat zvolené datum
- Novinka: Sbalení dloouhých logů je nyní ve výchozím nastavení

### Wherigo player
- Novinka: Integrovaná kontrola chybějících pověření v přehrávači Wherigo
- Změna: Odstraněno hlášení chyb Wherigo (jako jsou chyby většinou související s cartridgí, musí být opraveno majitelem cartridge)
- New: Ability to navigate to a zone using compass
- New: Ability to copy zone center coordinates to clipboard
- New: Set zone center as target when opening map (to get routing and distance info for it)
- Novinka: Podpora otevírání místních Wherigo souborů
- Change: Long-tap on a zone on map is no longer recognized. This allows users to do other stuff in map zone area available on long-tap, eg: create user-defined cache
- New: Display warning if wherigo.com reports missing EULA (which leads to failing download of cartridge)

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
- New: Added button to delete log templates
- Oprava: Importování místního mapového souboru získá náhodný název mapy
