V souladu s pravidly Obchodu Play jsme aktualizovali úroveň API systému Android, na kterou se zaměřuje tato verze aplikace c:geo, a změnili jsme některé rutiny rozložení obrazovky. To může mít některé nežádoucí vedlejší účinky, zejména u novějších verzí systému Android. Pokud narazíte na problémy s touto verzí c:geo, nahlaste je prosím buď na [GitHub](https://github.com/cgeo/cgeo), nebo e-mailem na [support@cgeo.org](mailto:support@cgeo.org)

### Mapa
- Novinka: Optimalizace trasy ukládá vypočítaná data do mezipaměti
- Novinka: Zapnutí živého režimu udržuje viditelné trasové body aktuálně nastaveného cíle
- Novinka: Dlouhým stisknutím navigační lišty se otevře výškový graf (Sjednocená mapa)
- Graf nadmořské výšky pro trasy a stopy
- Novinka: Stáhnout kešky seřazené podle vzdálenosti
- Oprava: Zdvojnásobení jednotlivých položek trasy
- Novinka: Podpora motivu Motorider (pouze VTM)
- Novinka: Podpora zobrazení offline map s průhledným pozadím (pouze VTM)
- New: NoMap tile provider (don't show map, just caches etc.)
- Change: Max distance to connect points on history track lowered to 500m (configurable)

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
- New: Placeholders for trackable logging (geocache name, geocache code, user)
- Change: Removed the link to outdated WhereYouGo player. Integrated Wherigo player is now default for Wherigos.
- Fix: Missing quick toggle in guided mode of waypoint calculator

### Wherigo přehrávač
- Novinka: Offline překlad pro Wherigo
- Novinka: Vylepšené ovládání tlačítek
- New: Status auto-save
- New: Option to create shortcout to Wherigo player on your mobile's home screen

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
- New: Preview bookmark lists
- Change: Increase minimum required Android version to Android 8
- New: Default quick buttons for new installations
- Fix: Titles in range input dialogs cut off
- Fix: Notification for nightly update points to regular APK even for FOSS variant
- New: "Ignore year" option for date filters
