### UnifiedMap färdplan & "gamla" kartor avskrivningsmeddelande
c:geo har en helt ny kartimplementation som heter "UnifiedMap" sedan en tid, vilket i slutändan kommer att ersätta de gamla implementationerna av Google Maps och Mapsforge (OpenStreetMap). Detta är en avskrivningsanmälan för att informera dig om den fortsatta färdplanen.

UnifiedMap publicerades för ungefär ett år sedan. Det stöder fortfarande Google Maps och OpenStreetMap (online + offline), men på ett helt omarbetat tekniskt sätt, och med en hel del spännande nya funktioner som de "gamla" kartorna inte stöder, varav en del är
- Kartrotation för OpenStreetMap-baserade kartor (online + offline)
- Klusterpopup för Google Maps
- Dölj kartkällor som du inte behöver
- Höjddiagram för rutter och spår
- Växla mellan listor direkt från kartan
- "Körläge" för OpenStreetMap-baserade kartor

UnifiedMap har visat sig vara stabil sedan länge, så vi kommer att ta bort de gamla kartimplementeringarna för att minska ansträngningarna för att upprätthålla c:geo.

Färdplan:
- "Gamla" kartor är i avskrivningsläge nu - vi kommer inte att fixa buggar för dem längre.
- UnifiedMap kommer att göras som standard för alla användare under hösten 2025.
- "Gamla" kartimplementationer kommer att tas bort våren 2026.

Fram till dess kan du växla mellan de olika implementationerna i inställningarna => kartkällor.

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
- Fix: Cache-information: Listor för cache som inte uppdaterats efter att ha tryckt på listnamnet och tagit bort cachen från den listan
- Fix: Användaranteckning tappas när du laddar upp ett lab adventure
- Ändra: Loggdatum-relaterade platshållare kommer att använda valt datum istället för aktuellt datum
- Ny: Kollapsa långa loggposter som standard

### Wherigo-spelare
- Nytt: Integrerad Wherigo-spelarkontroll kontrollerar saknade inloggningsuppgifter
- Ändring: Borttagen Wherigo felrapport (eftersom fel är mestadels patronrelaterade, måste rättas av patronägare)
- Nytt: Möjlighet att navigera till en zon med kompass
- Nytt: Möjlighet att kopiera zoncentrums koordinater till urklipp
- Nytt: Ange zoncentrum som mål när du öppnar kartan (för att få rutt- och distansinformation för det)
- Nyhet: Stöd för att öppna lokala Wherigo-filer
- Förändring: Långtryck på en zon på kartan känns inte längre igen. Detta gör det möjligt för användare att göra andra grejer i kartzonen som finns på långtryck, t.ex.: skapa användardefinierad cache
- Nyhet: Visa varning om wherigo.com rapporterar saknad EULA (som leder till misslyckad nedladdning av kassett)

### Allmänt
- Nyhet: Omdesignad söksida
- Nytt: Filter för antal i inventariet
- Nytt: Stöd för koordinater i DD,DDDDDDD-format
- Nytt: Visa senast använda filternamn i filterdialogen
- Nytt: Koordinatkalkylator: Funktion för att ersätta "x" med multiplikationssymbol
- Fix: Felaktig höjd (använder inte höjd över havsmedelnivån)
- Fix: Avståndsgränsen i närheten fungerar inte korrekt för små värden
- Fix: Sortering av cachelistor efter avstånd fungerar inte korrekt
- Fix: Lab-cacher exkluderade av D/T-filter även med aktiva "inkludera osäkerhet"
- Fix: Färg-problem med menyikoner i ljust läge
- Nyhet: Lägg till "Ta bort tidigare händelser" för att lista "alla"
- Nyhet: Visa koppling för "användardefinierade cacher" som aktiv i källfiltret
- Nyhet: GPX-export: exportera loggar / spårbara objekt gjorda valfria
- Fix: Lade till knapp för att ta bort loggmallar
- Fix: Import av lokal kartfil får slumpmässigt kartnamn
- Fix: Kartnedladdning erbjuder trasiga (0 bytes) filer för nedladdning
- Nytt: Lade till mappningar för vissa saknade OC-cache-typer
- Nytt: Flytta "nyligen använda"-listorna i dialogrutan för val av lista till toppen när du trycker på "nyligen använda"-knappen
- Nyhet: Dela en lista med geocoder från cachelistan
- Ändra: "Navigering (bil)" etc. använd parameter "q=" istället för föråldrad parameter "ll="
