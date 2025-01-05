Nytt: Integrerad Wherigo-spelare (beta) - se menypost på startskärmen.<br> (Du kanske vill [konfigurera ett snabbstartobjekt](cgeo-setting://quicklaunchitems_sorted) eller [anpassa bottennavigationen](cgeo-setting://custombnitem) för enklare åtkomst, måste aktivera utökade inställningar först.)

### Karta
- Nyhet: Spara kart-tema per leverantör (UnifiedMap)
- Nytt: Markera vald cache/vägpunkt (UnifiedMap)
- Nytt: Lägg till separator mellan kartkällor offline och online
- Nyhet: Stöd för Mapsforge som alternativ till VTM i UnifiedMap, se [Inställningar => Kartkällor => Unified Map](cgeo-setting://useMapsforgeInUnifiedMap)
- Ändring: 'Visa höjddiagram' flyttat till långtrycksmenyn (UnifiedMap)
- Ändra: Använd ny terrängskuggnings-algoritm för Mapsforge offline-kartor
- Nyhet: Terrängskuggnings-stöd för UnifiedMap Mapsforge offline-kartor
- Nyhet: Terrängskuggings-stöd för UnifiedMap VTM-kartor (kräver online-anslutning)
- Fix: Adresssökning tar inte hänsyn till live-läge (UnifiedMap)
- Förändring: "Följ min plats" flyttad till kartan, vilket ger mer utrymme för "live-läge" knappen
- Ändra: Gör långtryckstift mer c:geo-liknande
- Change: Offline data management functions (download maps, check for missing routing / hillshading data) moved to map selection menu => "Manage offline data"
- Fix: Karta uppdaterar inte ändrade cacher

### Cachedetaljer
- Ny: Ännu inte existerande variabler som används i projektionen skapas i variabellistan
- New: Tillåt stora heltal i formler
- Nytt: Stöd för fler konstellationer för variabler i formler
- Fix: Multiple images in personal note not added to images tab
- Fix: Handling of projections in waypoints and personal notes
- Nyhet: Långtryck på datum för inloggning hämtar tidigare loggdatum
- Fix: Att återställa cachen till ursprungliga koordinater tar inte bort "ändrade koordinater"-flaggan
- Nytt: Bekräfta att skriva över loggen på snabb offline-logg
- Nytt: Uppdatera cache-status när du skickar en logg
- New: Colored HTML source view of cache details
- Fix: checksum(0) returnerar fel värde
- Fix: Redigering av loggar tar bort "vänner"-status

### Allmänt
- Förändring: Använd höjd över havsytan (om tillgängligt, endast för Android 14+)
- Nytt: Tillåt flera hierarkiska nivåer i cachelistor
- Nyhet: Dedikerade ikoner för geocaching.com blockparty och HQ-event-typer
- Nyhet: Ange önskad bildstorlek för bilder som laddas från geocaching.com cacher och spårbara objekt
- Fix: "Open in browser" not working for trackable logs
- Nyhet: Alternativ för att hantera nedladdade filer (kartor, teman, routing och terrängskuggningsdata)
- Nyhet: Alternativ för att ta bort en cache från alla listor (= markera den att raderas)
- Fix: Återställda koordinater upptäcks inte av c:geo för osparade cacher
- Nytt: Tillåt rensning av filter om inget namngivet filter lagras
- Fix: "Empty list" confirmation popping up when starting a pocket query download in newly created list
- Change: Owned caches with offline logs show offline log marker
- New: Configurable date format (eg.: cache logs), see [Settings => Appearance => Date format](cgeo-settings://short_date_format)
- New: Point connector info on home screen to connector-specific preference screen
- Nytt: Ytterligare emojis för cache-ikoner
- Change: Cache type filter "Specials" includes events of types mega, giga, community celebration, HQ celebration, block party and maze
- Change: Cache type filter "Other" includes GCHQ, APE and unknown types
- Fix: History length and proximity settings sharing slider values
- Fix: Logg-sida för spårbara objekt visar tid/koordinat inmatningsfält för spårbara objekt som inte stöder detta
- Fix: Vissa krascher
