På grund av Play Store-policyer har vi uppdaterat Android API-nivån denna version av c:geo-mål + vi har ändrat några av skärmlayoutrutinerna. Detta kan komma med några oönskade biverkningar, särskilt på nyare Android-versioner. Om du har problem med denna version av c:geo, vänligen rapportera antingen på [GitHub](https://github.com/cgeo/cgeo) eller via e-post till [support@cgeo.org](mailto:support@cgeo.org)

Äldre kartor: Som meddelats med utgåvorna från 2025.07.17 och 2025.12.01, har vi äntligen tagit bort äldre implementationer för våra kartor. Du kommer att bytas till vår nya UnifiedMap automatiskt och bör inte märka några skillnader förutom ett par nya funktioner, varav några är
- Kartrotation för OpenStreetMap-baserade kartor (online + offline)
- Klusterpopup för Google Maps
- Dölj kartkällor som du inte behöver
- Höjddiagram för rutter och spår
- Växla mellan listor direkt från kartan
- "Körläge" för OpenStreetMap-baserade kartor
- Långtryck på spåret / individuell rutt för ytterligare alternativ

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
- Nyhet: Tillåt import av KML-filer som spår (t.ex. spårbar resväg)
- New: Offer to set cache icon even if cache is not yet stored

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
- Nyhet: Aggregerade funktioner med intervallstöd: add/sum, min/minimum, max/maximum, cnt/count, avg/genomsnitt, multiply/product/prod

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
- Nytt: Visa ångra-fältet när cacher tas bort från listan från kartan
- Fix: Crahs in percentage favorite filter
- New: Make it easier to use simple lists as parent lists
