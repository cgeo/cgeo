Due to Play Store policies we have updated the Android API level this version of c:geo targets + we have changed some of the screen layout routines. To může mít některé nežádoucí vedlejší účinky, zejména u novějších verzí systému Android. Pokud narazíte na problémy s touto verzí c:geo, nahlaste je prosím buď na [GitHub](https://github.com/cgeo/cgeo), nebo e-mailem na [support@cgeo.org](mailto:support@cgeo.org)

### Mapa
- Novinka: Optimalizace trasy ukládá vypočítaná data do mezipaměti
- Novinka: Zapnutí živého režimu udržuje viditelné trasové body aktuálně nastaveného cíle
- Novinka: Dlouhým stisknutím navigační lišty se otevře výškový graf (Sjednocená mapa)
- New: Show generated waypoints on map

### Detaily kešky
- Novinka: Detekce dalších znaků ve vzorcích: –, ⋅, ×
- Novinka: Zachování časového razítka vlastních logů při obnovení kešky
- Novinka: Volitelné mini zobrazení kompasu (viz nastavení => podrobnosti kešky => Zobrazit směr v podrobnostech kešky)
- Novinka: Možnost zobrazit logy majitelů na kartě „přátelé/vlastní“
- Změna: Karta „přátelé/vlastní“ zobrazuje počet logů pro danou kartu namísto globálních počítadel
- Change: Improved header in variable and waypoint tabs
- Fix: Two "delete log" items shown
- Fix: c:geo crashing in cache details when rotating screen
- Change: More compact layout for "adding new waypoint"
- New: Option to load images for geocaching.com caches in "unchanged" size
- New: Variables view can be filtered

### Wherigo přehrávač
- Novinka: Offline překlad pro Wherigo
- New: Improved button handling

### Obecné
- Nové: Sdílet možnost po zalogování kešky
- Změna: Nezobrazovat možnosti "vyžaduje údržbu" nebo "vyžaduje archivaci" pro vlastní kešky
- Oprava: Obnovení zálohy může duplikovat soubory stop v interním úložišti a následných zálohách
- Změna: Odstraněny odkazy na Twitter
- New: Delete orphaned trackfiles on clean up and restore backup
- New: Warning on trying to add too many caches to a bookmark list
- New: Watch/unwatch list functions
- New: Offer offline translation with Google Translate or DeepL apps (if installed)
