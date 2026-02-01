Edge to Edge: Due to Play Store policies we have updated the Android API level this version of c:geo targets + we have changed some of the screen layout routines. Detta kan komma med några oönskade biverkningar, särskilt på nyare Android-versioner. Om du har problem med denna version av c:geo, vänligen rapportera antingen på [GitHub](https://github.com/cgeo/cgeo) eller via e-post till [support@cgeo.org](mailto:support@cgeo.org)

Legacy Maps: As announced with 2025.07.17 and 2025.12.01 releases, we have finally removed the legacy implementations for our maps. You will be switched to our new UnifiedMap automatically and should notice no differences except a couple of new features, some of which are
- Kartrotation för OpenStreetMap-baserade kartor (online + offline)
- Klusterpopup för Google Maps
- Dölj kartkällor som du inte behöver
- Höjddiagram för rutter och spår
- Växla mellan listor direkt från kartan
- "Körläge" för OpenStreetMap-baserade kartor
- Long-tap on track / individual route for further options

### Karta
- Nyhet: Ruttoptimering cachar beräknade data
- Nyhet: Aktivering av live-läge håller waypoints för nuvarande inställda mål synliga
- Nyhet: Långtryck på navigationslinjen öppnar höjddiagram (UnifiedMap)
- Nytt: Visa genererade vägpunkter på kartan
- Nyhet: Ladda ned cacher beställda på distans
- Fix: Fördubbling av enskilda rutt-objekt
- Nyhet: Stöd för Motorider-tema (endast VTM)
- Nyhet: NoMap tile provider (visa inte karta, bara cacher osv.)
- Förändring: Max avstånd för att ansluta punkter på historikspår sänkt till 500m (konfigurerbart)

### Cachedetaljer
- Nytt: Upptäck ytterligare tecken i formler: –, ⋅, ×
- Nyhet: Bevara tidsstämpel för egna loggar vid uppdatering av en cache
- Nyhet: Valfri kompass-minivy (se inställningar => cachedetaljer => Visa riktning i cache-detaljvyn)
- Nyhet: Visa ägarnas loggar på fliken "vänner/egna"
- Ändra: "Vänner/egna" fliken visar antal loggar för den fliken istället för globalt antal
- Förändring: Förbättrad rubrik i variabel- och vägpunkt-flikar
- Fix: Två "ta bort logg" objekt visas
- Fix: c:geo kraschar i cache-detaljer när du roterar skärmen
- Förändring: Mer kompakt layout för att "lägga till ny vägpunkt"
- Nytt: Alternativ för att ladda bilder för geocaching.com cacher i "oförändrad" storlek
- Nytt: Variabler kan filtreras
- Nytt: Visualisera beräknat koordinat-spill i vägpunkt-listan
- Nytt: Menypost i vägpunkt-listan för att markera vissa vägpunkt-typer som besökta
- Nytt: Platshållare för loggning av spårbara (geocache-namn, geocache-kod, användare)
- Förändring: Länken till föråldrad WhereYouGo-spelare borttagen. Integrerad Wherigo-spelare är nu standard för Wherigos.
- Fix: Saknar snabbväxling i styrt läge av vägpunkt-kalkylatorn

### Wherigo player
- Nyhet: Offline-översättning för Wherigos
- Nytt: Förbättrad knapphantering
- Ny: Status auto-spara
- New: Option to create shortcout to Wherigo player on your mobile's home screen

### Allmänt
- Nyhet: alternativ att dela efter att ha loggat en cache
- Ändra: Visa inte "behöver underhåll" eller "behöver arkiveras"-alternativen för egna cacher
- Fix: Återställning av en säkerhetskopia kan duplicera spårfiler i internt lagringsutrymme och efterföljande säkerhetskopior
- Förändring: Referenser till Twitter borttagna
- Nytt: Ta bort övergivna spårfiler vid rensning och återställ säkerhetskopiering
- Nytt: Varning vid försök att lägga till för många cacher i en bokmärkeslista
- Nytt: Funktioner för att bevaka/avbevaka listan
- Nyhet: Erbjud offline-översättning med appar från Google Translate eller DeepL (om installerat)
- Nytt: Ta bort objekt från sökhistoriken
- Ändring: Ta bort GCVote (tjänsten upphör)
- Nyhet: Färgat verktygsfält på cache-detaljsidor
- Nyhet: Välj flera bokmärkeslistor / fickfrågor att ladda ner
- New: Preview bookmark lists
- Förändring: Öka den minsta nödvändiga Android-versionen till Android 8
- New: Default quick buttons for new installations
- Fix: Titles in range input dialogs cut off
- Fix: Notification for nightly update points to regular APK even for FOSS variant
- New: "Ignore year" option for date filters
- Nyhet: Gör fjärr-URI klickbar i väntande nedladdningar
- Ändring: Använd systeminställningar som standardtema för nya installationer
- New: GPX export: Write GSAK Lat/LonBeforeCorrect annotations when exporting original waypoints
- New: Show undo bar when deleting caches from list from map
