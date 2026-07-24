### Allmän versionsinformation

**Kant till kant**

På grund av Play Store-policyer har vi uppdaterat Android API-nivån denna version av c:geo riktar sig mot + vi har ändrat några av skärmlayoutrutinerna. Detta kan komma med några oönskade biverkningar, särskilt på nyare Android-versioner. Om du har problem med denna version av c:geo, vänligen rapportera antingen på [GitHub](https://github.com/cgeo/cgeo) eller via e-post till [support@cgeo.org](mailto:support@cgeo.org)

**Äldre kartor**

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
- Nytt: Erbjud att ställa in cache-ikonen även om cachen ännu inte är lagrad
- Nyhet: Infobox för höjddiagram som visar resterande avstånd, stigning, nedstigning
- Nyhet: Visa koordinater för vägpunkter i vägpunkt-popup
- Fix: Snabbinställningar på kartan kan visa knapparna "1"/"2" för tomma routing-profiler efter att ha bytt språk
- Nytt: Beräkna saknade höjddata vid import av spår (om höjddata laddas ner)
- Fix: Nedladdare av rutor stoppas under vissa förhållanden (OpenStreetMap endast onlinekartor)
- Nytt: Villkorade cachemarkörer
- Nytt: Visa navigeringstips (pil + avstånd)

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
- Fix: Felaktig hantering av DNF-status för opencaching-plattformar
- Nytt: Ta bort offline-logg efter sammanslagning med online-logg
- Nyhet: Visa bekräftelse vid borttagning av cacher med offlineloggar
- Nyhet: Visa bekräftelse när du tar bort alla cacher från listan "Alla"
- Nytt: Tillåt markdown-formatering för att lista text i användardefinierade cacher
- Ändra: Lagra cache innan du lägger till användarbild
- Fix: Krasch vid inläsning av bilder inbäddade direkt i text-listning
- Nyhet: Visa egna favoriter i loggvy (Geocaching.com + offline-loggar)

### Wherigo-spelare
- Nyhet: Offline-översättning för Wherigos
- Nytt: Förbättrad knapphantering
- Ny: Status auto-spara
- Nyhet: Alternativ för att skapa genväg till Wherigo-spelare på mobilens hemskärm

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
- Nytt: Förhandsgranska bokmärkeslistor
- Förändring: Öka den minsta nödvändiga Android-versionen till Android 8
- Nyhet: Standard-snabbinställningar för nya installationer
- Fix: Titlar i inmatningsdialogrutor för intervall avskurna
- Fix: Avisering för nattliga uppdateringspunkter till vanlig APK även för FOSS variant
- Nyhet: "Ignorera år"-alternativ för datumfilter
- Nyhet: Gör fjärr-URI klickbar i väntande nedladdningar
- Ändring: Använd systeminställningar som standardtema för nya installationer
- Nyhet: GPX-export: Skriv GSAK Lat/Lon före korrigera-kommentarer vid export av ursprungliga vägpunkter
- Nytt: Visa ångra-fältet när cacher tas bort från listan från kartan
- Fix: Krascher i procentuellt favoritfilter
- Nyhet: Gör det enklare att använda enkla listor som överordnade listor
- Ändra: Använd lokal tidszon (för enheten, inte event) för kalenderposter (istället för UTC)
- Fix: Vissa texter ignorerar språkväxling
- Fix: "Använd brittiska inställningar" initieras inte korrekt på nya installationer
- Förändring: Bergamot översättningsmodul med öppen källkod som ersätter Google ML Kit översättare
- Förändring: Ny emoji-väljare
