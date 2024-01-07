### Sjednocená mapa
Vítejte v naší zcela nové implementaci map, interně nazývané "Sjednocená mapa". Jedná se o výsledek téměř dvouleté práce týmu c:geo na zcela nové implementaci map. Důvodem, proč jsme s tím začali, bylo to, že naše staré implementace map bylo stále obtížnější udržovat a (víceméně) synchronizovat z hlediska funkcí, přičemž některé části kódu byly staré deset (nebo více) let.

Se Sjednocenou mapou jsme se snažili dosáhnout stejného uživatelského prostředí jako ve všech různých typech map (pokud to bylo možné) a zároveň modernizovat a sjednotit vnitřní architekturu.

Sjednocená mapa v podstatě nabízí (téměř) všechny funkce, které mají naše staré implementace map, ale poskytuje několik dalších funkcí:

- Otáčení mapy pro mapy založené na OpenStreetMap (online i offline)
- Frakční škálování pro mapy založené na OpenStreetMap
- Vyskakovací okno pro Google mapy
- Skrytí nepotřebných zdrojů map
- Graf nadmořské výšky pro trasy a stopy (klepněte na trasu)
- Přepínání mezi seznamy přímo z mapy (nebo dlouhým klepnutím na ikonu mapy)

Sjednocená mapa již dosáhla stavu beta verze, a proto jsme se rozhodli, že se stane naší výchozí mapou pro všechny uživatele používající noční sestavení.

Vše by mělo fungovat, ale stále se mohou vyskytovat (a budou vyskytovat) nějaké chyby. V případě potřeby můžete přepínat mezi starou a novou implementací map (viz nastavení - zdroje map), ale byli bychom rádi, kdybyste vyzkoušeli tu novou. Jakékoli nalezené chyby prosím hlaste na podporu ([support@cgeo.org](mailto:support@cgeo.org)) nebo [c:geo na GitHubu](github.com/cgeo/cgeo/issues). Každá zpětná vazba je vítaná!

---

Další změny:

### Mapa
- Novinka: Zvýraznění stávajících stahování ve správci stahování
- Novinka: Zobrazit stav nálezu kešek na ikonách trasových bodů
- Novinka: Možnost dlouhým klepnutím spojit kešku s jejími trasovými body pomocí čar
- Změna: Zobrazení podrobností o kešce/trasovém bodu neblokujícím způsobem
- Novinka: Volitelně uchovávat dočasné OAM soubory (stahování map, užitečné při použití souborů POI s jinými aplikacemi)

### Detaily kešky
- Změna: Z "Přepnout mluvení" byl vytvořen skutečný přepínač
- Změna: Zvýšení maximální délky logu pro geocaching.com
- Oprava: Nelze nahrát delší osobní poznámky na stránkách opencaching
- New: Edit/delete own logs

### Obecné
- Novinka: Klepnutím na oznámení stahovače se otevře zobrazení "čekající na stažení"
- Změna: Použití tapety jako pozadí již nevyžaduje oprávnění READ_EXTERNAL_STORAGE
- Novinka: Rozložení dvou sloupců pro nastavení v režimu na šířku
- Oprava: Obnovení záloh bez platné informace o datu/času
