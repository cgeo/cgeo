Edge to Edge: Due to Play Store policies we have updated the Android API level this version of c:geo targets + we have changed some of the screen layout routines. To může mít některé nežádoucí vedlejší účinky, zejména u novějších verzí systému Android. Pokud narazíte na problémy s touto verzí c:geo, nahlaste je prosím buď na [GitHub](https://github.com/cgeo/cgeo), nebo e-mailem na [support@cgeo.org](mailto:support@cgeo.org)

Legacy Maps: As announced with 2025.07.17 and 2025.12.01 releases, we have finally removed the legacy implementations for our maps. You will be switched to our new UnifiedMap automatically and should notice no differences except a couple of new features, some of which are
- Otáčení mapy pro mapy založené na OpenStreetMap (online + offline)
- Vyskakovací okno pro Google mapy
- Skrytí nepotřebných zdrojů map
- Graf nadmořské výšky pro trasy a stopy
- Přepínání mezi seznamy přímo z mapy
- "Řidičský režim" pro mapy založené na OpenStreetMap
- Long-tap on track / individual route for further options

### Mapa
- Novinka: Optimalizace trasy ukládá vypočítaná data do mezipaměti
- Novinka: Zapnutí živého režimu udržuje viditelné trasové body aktuálně nastaveného cíle
- Novinka: Dlouhým stisknutím navigační lišty se otevře výškový graf (Sjednocená mapa)
- Graf nadmořské výšky pro trasy a stopy
- Novinka: Stáhnout kešky seřazené podle vzdálenosti
- Oprava: Zdvojnásobení jednotlivých položek trasy
- Novinka: Podpora motivu Motorider (pouze VTM)
- Novinka: Podpora zobrazení offline map s průhledným pozadím (pouze VTM)
- Novinka: Poskytovatel dlaždic NoMap (nezobrazovat mapu, jen kešky atd.)
- Změna: Maximální vzdálenost pro propojení bodů na historii trasy snížena na 500m (konfigurovatelné)

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
