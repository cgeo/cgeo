### Obecné poznámky k verzi

**Edge to Edge**

V souladu s pravidly Obchodu Play jsme aktualizovali úroveň API systému Android, na kterou se zaměřuje tato verze aplikace c:geo, a změnili jsme některé rutiny rozložení obrazovky. To může mít některé nežádoucí vedlejší účinky, zejména u novějších verzí systému Android. Pokud narazíte na problémy s touto verzí c:geo, nahlaste je prosím buď na [GitHub](https://github.com/cgeo/cgeo), nebo e-mailem na [support@cgeo.org](mailto:support@cgeo.org)

**Starší mapy**

Jak bylo oznámeno ve verzích ze 17.07.2025 a 01.12.2025, konečně jsme odstranili starší implementace našich map. Budete automaticky převedeni na naši novou službu Sjednocená mapa a kromě několika nových funkcí, z nichž některé jsou
- Otáčení mapy pro mapy založené na OpenStreetMap (online + offline)
- Vyskakovací okno pro Google mapy
- Skrytí nepotřebných zdrojů map
- Graf nadmořské výšky pro trasy a stopy
- Přepínání mezi seznamy přímo z mapy
- "Řidičský režim" pro mapy založené na OpenStreetMap
- Dlouhým klepnutím na trasu / individuální trasu zobrazíte další možnosti

### Mapa
- Novinka: Optimalizace trasy ukládá vypočítaná data do mezipaměti
- Novinka: Zapnutí živého režimu udržuje viditelné trasové body aktuálně nastaveného cíle
- Novinka: Dlouhým stisknutím navigační lišty se otevře výškový graf (Sjednocená mapa)
- Graf nadmořské výšky pro trasy a stopy
- Novinka: Stáhnout kešky seřazené podle vzdálenosti
- Oprava: Zdvojnásobení jednotlivých položek trasy
- Novinka: Podpora motivu Motorider (pouze VTM)
- Novinka: Poskytovatel dlaždic NoMap (nezobrazovat mapu, jen kešky atd.)
- Změna: Maximální vzdálenost pro propojení bodů na historii trasy snížena na 500m (konfigurovatelné)
- Novinka: Možnost importovat soubory KML jako trasy (např. trasu s možností sledování)
- Novinka: Nabídka nastavení ikony kešky i v případě, že keška ještě není uložena
- Novinka: Infobox pro výškový graf zobrazující zbývající vzdálenost, stoupající, klesající
- Novinka: Zobrazení souřadnic trasových bodů ve vyskakovacím okně trasového bodu
- Oprava: V rychlých nastaveních mapy se po přepnutí jazyka mohou u prázdných profilů tras zobrazovat tlačítka „1“/„2“
- Novinka: Výpočet chybějících údajů o nadmořské výšce při importu tras (pokud jsou data o nadmořské výšce stažena)
- Oprava: Stahovač map se za určitých podmínek zastaví (pouze online mapy OpenStreetMap)
- New: Conditional cache markers
- New: Show navigation hint (arrow + distance)

### Detaily kešky
- Novinka: Detekce dalších znaků ve vzorcích: –, ⋅, ×
- Novinka: Zachování časového razítka vlastních logů při obnovení kešky
- Novinka: Volitelné mini zobrazení kompasu (viz nastavení => podrobnosti kešky => Zobrazit směr v podrobnostech kešky)
- Novinka: Možnost zobrazit logy majitelů na kartě „přátelé/vlastní“
- Změna: Karta „přátelé/vlastní“ zobrazuje počet logů pro danou kartu namísto globálních počítadel
- Změna: Vylepšená záhlaví v záložkách proměnných a trasových bodů
- Oprava: Zobrazeny dvě položky „smazat protokol“
- Oprava: c:geo se zhroutí v detailech kešky při otočení obrazovky
- Změna: Kompaktnější rozložení pro „přidání nového trasového bodu“
- Novinka: Možnost načíst obrázky pro kešky z geocaching.com v „nezměněné“ velikosti
- Novinka: Proměnné lze filtrovat
- Novinka: Vizualizace vypočítaných souřadnic v seznamu trasových bodů
- Novinka: Položka v nabídce seznamu trasových bodů pro označení určitých typů trasových bodů jako navštívených
- Novinka: Zástupné symboly pro zápis sledovatelných předmětů (název kešky, kód kešky, uživatel)
- Změna: Odstraněn odkaz na zastaralý přehrávač WhereYouGo. Integrovaný přehráč Wherigo je nyní výchozí pro Wherigo kešky.
- Oprava: Chybějící rychlé přepínání v režimu průvodce kalkulátoru trasových bodů
- Novinka: Souhrnné funkce s podporou rozsahu: add/součet, min/minimum, max/maximum, cnt/počet, avg/průměr, multiply/součin/pro
- Oprava: Nesprávné zpracování stavu DNF pro platformy opencaching
- Novinka: Po sloučení s online logem odstranit offline log
- Novinka: Zobrazit potvrzení při mazání kešek pomocí offline logů
- Novinka: Zobrazit potvrzení při mazání všech kešek ze seznamu "Vše"
- Novinka: Povolit formátování Markdown pro výpis textu v uživatelsky definovaných keškách
- Změna: Uložení kešky před přidáním obrázku uživatele
- Fix: Crash on loading images embedded directly in listing text
- New: Show own favorites in log view (Geocaching.com + offline logs)

### Wherigo přehrávač
- Novinka: Offline překlad pro Wherigo
- Novinka: Vylepšené ovládání tlačítek
- Novinka: Automatické ukládání stavu
- Novinka: Možnost vytvořit zástupce pro přehrávač Wherigo na domovské obrazovce vašeho mobilního telefonu

### Obecné
- Nové: Sdílet možnost po zalogování kešky
- Změna: Nezobrazovat možnosti "vyžaduje údržbu" nebo "vyžaduje archivaci" pro vlastní kešky
- Oprava: Obnovení zálohy může duplikovat soubory stop v interním úložišti a následných zálohách
- Změna: Odstraněny odkazy na Twitter
- Oprava: Uživatelská poznámka se ztratí při obnovení dobrodružství v Lab Adventures
- Změna: zástupné symboly budou namísto aktuálního data používat zvolené datum
- Novinka: Sbalení dloouhých logů je nyní ve výchozím nastavení
- Novinka: Nabídka offline překladu pomocí aplikací Překladač Google nebo DeepL (pokud jsou nainstalovány)
- Novinka: Odstranit položky z historie hledání
- Změna: Odebráno GCVote (služba pozastavena)
- Novinka: Barevný panel nástrojů na stránkách s podrobnostmi o kešce
- Novinka: Vyberte více seznamů záložek / pocket queries ke stažení
- Novinka: Náhled seznamů záložek
- Změna: Zvýšení minimální požadované verze Androidu na Android 8
- Novinka: Výchozí rychlá tlačítka pro nové instalace
- Oprava: Dlaždice v dialogových oknech pro zadávání rozsahu jsou oříznuté
- Oprava: Oznámení o noční aktualizaci odkazuje na běžný soubor APK i pro variantu FOSS
- Novinka: Možnost „Ignorovat rok“ pro filtry data
- Novinka: Možnost kliknout na vzdálený URI v čekajících stahováních
- Změna: Použít nastavení systému jako výchozí téma pro nové instalace
- Novinka: Export GPX: Při exportu původních trasových bodů zapisujte poznámky GSAK Lat/LonBeforeCorrect
- Novinka: Zobrazit lištu pro vrácení zpět při mazání kešek ze seznamu na mapě
- Oprava: Pád aplikace při použití filtru podle procentuální obliby
- Novinka: Usnadnění používání jednoduchých seznamů jako nadřazených seznamů
- Změna: Pro položky v kalendáři používat místní časové pásmo (zařízení, nikoli události) (namísto UTC)
- Oprava: Některé texty ignorují přepínání jazyků
- Oprava: Volba „Použít imperiální jednotky“ se při nové instalaci nenastavila správně
- Change: Bergamot open source offline translation module replacing closed-source Google ML Kit translator
- Change: New emoji selector
