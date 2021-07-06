### Pokročilý systém filtrování
- Představujeme nový systém filtrování v aplikaci c:geo, který podporuje flexibilní, kombinovatelné a ukládatelné filtry
- K dispozici v seznamech kešek i v zobrazení mapy
- Nová funkce "Hledat podle filtru"

### Mapa
- Novinka: Při vytváření uživatelem definované kešky při zobrazení mapy ze seznamu: Nabídne uživateli uložení nové kešky do aktuálního seznamu (místo výchozího seznamu pro uživatelem definované kešky)
- Novinka: Oddělené filtry "vlastní" a "nalezené" v rychlém nastavení mapy
- Změna: Zobrazení názvu kešky v podrobnostech ve vyskakovacím okně

### Detaily kešky
- Novinka: Využijte vyskakovací okno pro překlad v aplikaci Překladač Google
- Novinka: Umožňuje změnit přiřazenou ikonu ve vyskakovacím okně s podrobnostmi o kešce pomocí dlouhého kliknutí (pouze uložené kešky)

### Downloader
- Změna: Stahování nyní probíhá zcela na pozadí, zobrazí se oznámení
- Změna: Úspěšně stažené soubory automaticky přepíší existující soubory se stejným názvem
- Změna: Pokud mapa vyžaduje určitý motiv, který ještě není nainstalován, aplikace c:geo automaticky stáhne a nainstaluje daný motiv

### Ostatní
- Změna: Kompletně jsem přepracovali interní technické aspekty motivu aplikace c:geo, abychom mohli využívat některé modernější komponenty poskytované systémem Android. To s sebou přináší několik vedlejších efektů, z nichž některé nejsou zamýšlené. Jakékoli chyby nebo jiné neduhy nahlaste na naší [stránce GitHub](https://www.github.com/cgeo/cgeo/issues) nebo kontaktujte podporu.
- Novinka: Podpora denního / nočního režimu ze systému (volitelně)
- Novinka: Stahování seznamů záložek z geocaching.com - viz "Seznamy / pocket queries" v hlavním menu
- Novinka: Ignorování možností pro geocaching.su
- Změna: Odstraněna již neudržovaná navigační aplikace RMAPS
- Oprava: Extrahování bodu trasy se stejným názvem ale odlišnými souřadnicemi z osobní poznámky
- Oprava: Chyba při extrahování osobní poznámky pro bod trasy se vzorcem
- Oprava: Export vzorce do osobní poznámky namísto souřadnic pro vyplněný vzorec
- Oprava: Složka s offline mapami a motivy není po přeinstalování a obnovení zálohy v pořádku
- Oprava: Stopu/trasu nelze aktualizovat
- Oprava: Chyba motivu pro stahování ve světlém motivu
