### Sjednocená mapa & upozornění na odstranění „starých“ map
c:geo má zcela novou implementaci mapy nazvanou "Sjednocená mapa", která nakonec nahradí staré implementace Mapy Google a Mapsforge (OpenStreetMap). Toto je notifikace o zastaralosti, která vás informuje o dalším plánu.

Sjenocená mapa byla publikována asi před rokem. Stále podporuje Mapy Google a OpenStreetMap (online + offline), ale ve zcela přepracované technické podobě a se spoustou nových zajímavých funkcí, které „staré“ mapy nepodporují, mezi něž patří například
- Otáčení mapy pro mapy založené na OpenStreetMap (online + offline)
- Vyskakovací okno pro Google mapy
- Skrytí nepotřebných zdrojů map
- Graf nadmořské výšky pro trasy a stopy
- Přepínání mezi seznamy přímo z mapy
- "Řidičský režim" pro mapy založené na OpenStreetMap

Sjednocená mapa se již delší dobu osvědčila jako stabilní, proto odstraníme staré implementace map, abychom snížili nároky na údržbu c:geo.

Cestovní mapa:
- „Staré“ mapy jsou nyní v režimu zastaralosti – nebudeme již opravovat chyby, které se v nich vyskytují.
- Sjednocená mapa se stane výchozí pro všechny uživatele na podzim roku 2025.
- „Staré“ implementace map budou odstraněny na jaře 2026.

Do té doby můžete přepínat mezi různými implementacemi v nastavení => mapové zdroje.

### Mapa
- Novinka: Zobrazí geofence u zastávek labek (Sjednocená mapa) - povolte "Kruhy" v rychlém nastavení na mapě pro jejich zobrazení
- Novinka: Možnost nastavit kruhy s individuálním poloměrem na trasové body (možnost kontextového menu "Sledovaná oblast")
- Oprava: Zobrazení mapy se neaktualizuje při odebrání kešky z aktuálně zobrazeného seznamu
- Oprava: Číslo kešky ve výběrovém seznamu se neaktualizuje při změně obsahu seznamu
- Změna: Ponechat aktuální zobrazení při mapování seznamu, pokud se všechny kešky vejdou do aktuálního zobrazení
- Novinka: Sledování mé polohy v grafu nadmořské výšky (Sjednocená mapa)
- Novinka: Povolit akce "přesunout do" / "kopírovat do" pro "zobrazit jako seznam"
- Novinka: Podpora motivu Elevate Winter ve stahovači map
- Novinka: Adaptivní stínování kopců, volitelný režim vysoké kvality (Sjednocená mapa Mapsforge)
- Novinka: Přepracované dialogové okno rychlého nastavení tras/stop
- Novinka: Dlouhým klepnutím na ikonu výběru mapy vyberete předchozího poskytovatele dlaždic (Sjednocená mapa)
- Novinka: Povolit zobrazení názvu offline mapy v doprovodném souboru (Sjednocená mapa)
- Novinka: Dlouhým klepnutím na "Povolit živé tlačítko" načíst offline kešky
- Nový: Offline stínování kopců pro Sjednocenou mapu (VTM varianta)
- Novinka: Podpora pro mapy pozadí (Sjednocená mapa)
- Oprava: Kompaktní ikony se při přiblížení v automatickém režimu nevrátí do velkých ikon (Sjednocená mapa)
- Novinka: Akce po dlouhém stisknutí v informačním listu kešky: kód GC, název kešky, souřadnice, osobní poznámka/nápověda
- Změna: Přepíná dlouhé stisknutí informačního listu kešky pro výběr emodži na krátké stisknutí, aby se vyřešila kolize

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
- Oprava: Stahovač map nabízí ke stažení poškozené (0 bajtů) soubory
- Novinka: Přidáno mapování pro některé chybějící typy OC kešek
- Novinka: Přesunutí „nedávno použitých“ seznamů v dialogovém okně pro výběr seznamu na začátek, při stisknutí tlačítka „nedávno použité“
- Novinka: Sdílení seznamu geokódů ze seznamu kešek
- Změna: "Navigace (auto) apod. použijte parametr "q=" místo zastaralého parametru "ll="
