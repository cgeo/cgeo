(nightly only: Temporarily removed "nightly" banner from logo while fine-tuning the design)

### UnifiedMap roadmap & "old" maps deprecation notice
c:geo has an all-new map implementation called "UnifiedMap" since some time, which will ultimately replace the old implementations of Google Maps and Mapsforge (OpenStreetMap). This is a deprecation notice to inform you about the further roadmap.

UnifiedMap got published about a year ago. It still supports Google Maps and OpenStreetMap (online + offline), but in a completely reworked technical way, and with a lot of exciting new features that the "old" maps do not support, some of which are
- Map rotation for OpenStreetMap based maps (online + offline)
- Cluster popup for Google Maps
- Hide map sources you don't need
- Elevation chart for routes and tracks
- Switch between lists directly from map
- "Driving mode" for OpenStreetMap based maps

UnfiedMap has proven to be stable since quite some time, thus we will remove the old map implementations to reduce the efforts for maintaining c:geo.

Roadmap:
- "Old" maps are in deprecation mode now - we won't fix bugs for it anymore.
- UnifiedMap will be made default for all users in fall of 2025.
- "Old" map implementations will be removed in spring 2026.

Until then, you can switch between the different implementations in settings => map sources.

### Karta
- Nyhet: Visa geofences för labsteg (UnifiedMap) - aktivera "Cirklar" i snabbinställningar på kartan för att visa dem
- Ny: Alternativ för att ställa in cirklar med individuell radie till vägpunkter ("geofence" alternativ i sammanhangsmeny)
- Fix: Map view not updated when removing cache from currently shown list
- Fix: Number of cache in list chooser not updated on changing list contents
- Ändra: Behåll nuvarande vy för att visa en lista på karta, om alla cacher passar in i nuvarande vy
- Nyhet: Följ min plats i höjddiagram (UnifiedMap)
- Nytt: Aktivera "flytta till" / "kopiera till" åtgärder för "visa som lista"
- New: Support Elevate Winter theme in map downloader
- Nyhet: Adaptiv terrängskuggning, valfritt högkvalitetsläge (UnifiedMap Mapsforge)
- Nyhet: Omgjorda rutter/spår snabbinställningsdialog
- Nyhet: Långtryck på ikonen för kartval för att välja tidigare leverantör (UnifiedMap)
- Nytt: Tillåt inställning av visningsnamn för offline-kartor i följeslagarfil (UnifiedMap)
- Nytt: Långtryck på "aktivera live-knappen" för att ladda offline cacher
- Nyhet: Offline backskuggning för UnifiedMap (VTM-variant)
- Nytt: Stöd för bakgrundskartor (UnifiedMap)

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
- New: Add "Remove past events" to list "all"
- New: Show connector for "user-defined caches" as active in source filter
- New: GPX export: exporting logs / trackables made optional
- New: Added button to delete log templates
- Fix: Import av lokal kartfil får slumpmässigt kartnamn
