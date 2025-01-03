Novinka: Integrovaný přehrávač Wherigo (beta verze) - viz nabídka na domovské obrazovce.<br> (Možná budete chtít [konfigurovat položku rychlého spuštění](cgeo-setting://quicklaunchitems_sorted) nebo [přizpůsobit spodní navigaci](cgeo-setting://custombnitem) pro snadnější přístup, je třeba nejprve povolit rozšířené nastavení.)

### Mapa
- Novinka: Ukládání motivu mapy podle poskytovatele dlaždic (Sjednocená mapa)
- Novinka: Zvýraznění vybrané kešky/trasového bodu (Sjednocená mapa)
- Novinka: Přidán oddělovač mezi offline a online zdroje map
- Novinka: Podpora Mapsforge jako alternativy k VTM ve Sjednocené mapě, viz [Nastavení => Zdroje map => Sjednocená mapa](cgeo-setting://useMapsforgeInUnifiedMap)
- Změna: Volba "Zobrazit výškový graf" přesunuto do nabídky dlouhého klepnutí (Sjednocená mapa)
- Změna: Použití nového algoritmu stínování kopců pro offline mapy Mapsforge
- Novinka: Podpora stínování kopců pro offline mapy Sjednocená mapa Mapsforge
- Novinka: Podpora stínování kopců pro mapy Sjednocená mapa VTM (vyžaduje online připojení)
- Oprava: Vyhledávání adres nezohledňuje živý režim (Sjednocená mapa)
- Změna: Funkce "sledovat mou polohu" se přesunula na mapu, čímž se uvolnilo více místa pro tlačítko "Živý režim"
- Změna: Dlouhé stisknutí špendlíku více připomíná c:geo
- Změna: Funkce pro správu offline dat (stahování map, kontrola chybějících dat o trasování / stínování kopců) přesunuty do nabídky výběru map => „Správa offline dat“
- Fix: Map not updating changed caches

### Detaily kešky
- Novinka: V seznamu proměnných zatím neexistují proměnné použité v projekcích
- Novinka: Povolení velkých celých čísel ve vzorcích
- Novinka: Podpora více konstelací pro proměnné ve vzorcích
- Oprava: Více obrázků v osobní poznámce nebylo přidáno na kartu Obrázky
- Oprava: Zpracování projekcí v trasových bodech a osobních poznámkách
- Novinka: Dlouhým klepnutím na datum v logu se načte předchozí datum logu
- Oprava: Obnovení kešky na původní souřadnice neodstraní příznak “změněné souřadnice“
- Novinka: Potvrzení přepsání logu v rychlém offline logu
- Novinka: Aktualizace stavu kešky při odeslání logu
- Novinka: Barevné zobrazení HTML zdroje s podrobnostmi o kešce
- Fix: checksum(0) returning wrong value
- Fix: Editing logs removes "friends" status

### Obecné
- Změna: Použije nadmořskou výšku nad střední hladinou moře (pokud je k dispozici, pouze Android 14+)
- Novinka: Povolení více úrovní hierarchie v seznamech kešek
- Novinka: Vyhrazené ikony pro typy eventů na geocaching.com blockparty a HQ
- Novinka: Nastavení preferované velikosti obrázku pro obrázky načtené z kešek a trasovatelných předmětů na webu geocaching.com
- Oprava: Volba „Otevřít v prohlížeči“ nefunguje pro sledovatelné protokoly
- Novinka: Možnost spravovat stažené soubory (mapy, motivy, směrovací data a data stínování kopců)
- Novinka: Možnost odstranit kešku ze všech seznamů (= označit ji jako ke smazání)
- Oprava: Obnovení souřadnic nezjištěno pomocí c:geo pro neuložené kešky
- Novinka: Povolit vymazání filtru, pokud není uložen žádný pojmenovaný filtr
- Oprava: Vyskakující potvrzení "Prázdný seznam" při spuštění stahování v nově vytvořeném seznamu
- Změna: Vlastní kešky s offline logy zobrazují značku offline logu
- Novinka: Nastavitelný formát data (např. logy kešek), viz [Nastavení => Vzhled => Formát data](cgeo-settings://short_date_format)
- Novinka: Přesměrování informací o konektorech na domovské obrazovce na obrazovku předvoleb konkrétního konektoru
- Novinka: Další emotikony pro ikony kešek
- Změna: Filtr typu kešky „Speciální“ zahrnuje události typu mega, giga, community celebration, HQ celebtration, block party a maze
- Změna: Filtr typu kešky „Ostatní“ zahrnuje typy GCHQ, APE a neznámé typy
- Fix: History length and proximity settings sharing slider values
- Fix: Trackable log page showing time/coordinate input fields for trackables not supporting this
- Fix: Some crashes
