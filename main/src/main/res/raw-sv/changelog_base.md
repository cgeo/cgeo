### UnifiedMap roadmap & "old" maps deprecation notice
c:geo has an all-new map implementation called "UnifiedMap" since some time, which will ultimately replace the old implementations of Google Maps and Mapsforge (OpenStreetMap). This is a deprecation notice to inform you about the further roadmap.

UnifiedMap got published about a year ago. It still supports Google Maps and OpenStreetMap (online + offline), but in a completely reworked technical way, and with a lot of exciting new features that the "old" maps do not support, some of which are
- Kartrotation för OpenStreetMap-baserade kartor (online + offline)
- Klusterpopup för Google Maps
- Dölj kartkällor som du inte behöver
- Höjddiagram för rutter och spår
- Växla mellan listor direkt från kartan
- "Körläge" för OpenStreetMap-baserade kartor

UnfiedMap has proven to be stable since quite some time, thus we will remove the old map implementations to reduce the efforts for maintaining c:geo.

Roadmap:
- "Gamla" kartor är i avskrivningsläge nu - vi kommer inte att fixa buggar för dem längre.
- UnifiedMap kommer att göras som standard för alla användare under hösten 2025.
- "Gamla" kartimplementationer kommer att tas bort våren 2026.

Until then, you can switch between the different implementations in settings => map sources.

### Karta
- Nyhet: Visa geofences för labsteg (UnifiedMap) - aktivera "Cirklar" i snabbinställningar på kartan för att visa dem
- Ny: Alternativ för att ställa in cirklar med individuell radie till vägpunkter ("geofence" alternativ i sammanhangsmeny)
- Fix: Kartvy inte uppdaterad när cachen tas bort från listan som visas just nu
- Fix: Antal cacher i list-väljaren inte uppdaterad vid förändring av innehållet i listan
- Ändra: Behåll nuvarande vy för att visa en lista på karta, om alla cacher passar in i nuvarande vy
- Nyhet: Följ min plats i höjddiagram (UnifiedMap)
- Nytt: Aktivera "flytta till" / "kopiera till"-åtgärder för "visa som lista"
- Nyhet: Stöd Höjd vintertema i kartan nedladdning
- Nyhet: Adaptiv terrängskuggning, valfritt högkvalitetsläge (UnifiedMap Mapsforge)
- Nyhet: Omgjorda rutter/spår snabbinställningsdialog
- Nyhet: Långtryck på ikonen för kartval för att välja tidigare leverantör (UnifiedMap)
- Nytt: Tillåt inställning av visningsnamn för offline-kartor i följeslagarfil (UnifiedMap)
- Nytt: Långtryck på "aktivera live-knappen" för att ladda offline cacher
- Nyhet: Offline terrängskuggning för UnifiedMap (VTM-variant)
- Nytt: Stöd för bakgrundskartor (UnifiedMap)
- Fix: Kompakta ikoner som inte återvänder till stora ikoner vid zoomning i autoläge (UnifiedMap)
- Nytt: Åtgärder för långtryck på cache-infosida: GC-kod, cachetitel, koordinater, personlig anteckning/ledtråd
- Ändring: Byter cache infosida långtryck för emoji-väljare till kort tryck för att lösa kollisionen

### Cachedetaljer
- Nyhet: Offline-översättning av notering, text och loggar (experimentell)
- Nyhet: Alternativ för att dela cache med användardata (koordinater, personlig anteckning)
- Fix: Talservice avbryts vid skärmrotation
- Fix: Cache details: Lists for cache not updated after tapping on list name an removing that cache from that list
- Fix: Användaranteckning tappas när du laddar upp ett lab adventure
- Ändra: Loggdatum-relaterade platshållare kommer att använda valt datum istället för aktuellt datum
- New: Collapse long log entries per default

### Wherigo player
- Nytt: Integrerad Wherigo-spelarkontroll kontrollerar saknade inloggningsuppgifter
- Change: Removed Wherigo bug report (as errors are mostly cartridge-related, need to be fixed by cartridge owner)
- New: Ability to navigate to a zone using compass
- New: Ability to copy zone center coordinates to clipboard
- New: Set zone center as target when opening map (to get routing and distance info for it)
- Nyhet: Stöd för att öppna lokala Wherigo-filer
- Change: Long-tap on a zone on map is no longer recognized. This allows users to do other stuff in map zone area available on long-tap, eg: create user-defined cache
- New: Display warning if wherigo.com reports missing EULA (which leads to failing download of cartridge)

### Allmänt
- New: Redesigned search page
- Nytt: Filter för antal i inventariet
- Nytt: Stöd för koordinater i DD,DDDDDDD-format
- Nytt: Visa senast använda filternamn i filterdialogen
- Nytt: Koordinatkalkylator: Funktion för att ersätta "x" med multiplikationssymbol
- Fix: Felaktig höjd (använder inte höjd över havsmedelnivån)
- Fix: Avståndsgränsen i närheten fungerar inte korrekt för små värden
- Fix: Sorting of cache lists by distance descending not working correctly
- Fix: Lab-cacher exkluderade av D/T-filter även med aktiva "inkludera osäkerhet"
- Fix: Färg-problem med menyikoner i ljust läge
- Nyhet: Lägg till "Ta bort tidigare händelser" för att lista "alla"
- Nyhet: Visa koppling för "användardefinierade cacher" som aktiv i källfiltret
- New: GPX export: exporting logs / trackables made optional
- New: Added button to delete log templates
- Fix: Import av lokal kartfil får slumpmässigt kartnamn
- Fix: Kartnedladdning erbjuder trasiga (0 bytes) filer för nedladdning
- Nytt: Lade till mappningar för vissa saknade OC-cache-typer
- Nytt: Flytta "nyligen använda"-listorna i dialogrutan för val av lista till toppen när du trycker på "nyligen använda"-knappen
- Nyhet: Dela en lista med geocoder från cachelistan
- Ändra: "Navigering (bil)" etc. använd parameter "q=" istället för föråldrad parameter "ll="
