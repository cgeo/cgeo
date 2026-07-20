##
Dags att uppdatera! Om du fortfarande använder Android 7 eller äldre kan detta vara den sista uppdateringen av c:geo för dig! Med vår nästa version av c:geo kommer vi att släppa stödet för Android 5-7 för att minska vår underhållsbelastning och för att kunna uppdatera några externa komponenter som används av c:geo som vi för närvarande fortfarande håller tillbaka. Vi kommer fortfarande att stödja Android 8 upp till Android 16 (och nyare versioner när de publiceras), en spännvidd av mer än åtta år av Android-historia.

- Fix: Cache/vägpunkt popup som öppnas försenat på vissa enheter
- Fix: Redigera cachebeskrivning stöder inte kopiera & klistra
- Fix: Vissa krascher och "appen svarar inte"
- Fix: Borttagning av spårbar logg misslyckas (ändring av webbplatsen)

##
- Fix: Radering av loggbilder trasig (webbplatsändring)
- Ändring: Enhetliga spår och enskilda ruttladdningsknappar
- Fix: Cache-attribut upptäcks inte korrekt under vissa förhållanden
- Fix: Loggning av cacher (ändring av webbplatsen)
- Fix: Loggning av spårbara (ändring av webbplatsen)

##
- Fix: Pocket query import trasig (webbplats-ändring)

##
- Fix: Krasch vid åtkomst till rutter
- Fix: Krasch på waypoint-sidan
- Ändra: Sök efter "egna cacher" börjar med färska filter
- Fix: Osparade labbäventyr-steg förlorar "besökt"-information vid uppdatering
- Fix: Återkommande fråga för källuppdateringar av rutor
- Fix: Slumpmässig plats när en lista visas på karta (Google Maps)

##
- Fix: Krasch i cache infoblad
- Fix: Wherigo-cartridges kan inte laddas ner längre (webbplatsändring)

##
 - Ändra: Wherigo-filer kan inte laddas ner för närvarande, visa instruktioner för åtgärder
 - Fix: Orsak till radering av logg upprätthåller inte längdgräns
 - Nytt: Utökad loggning för krascher i nedladdningshanteraren
 - Fix: Waypoint infosheet kan bli för lång, knappar oåtkomliga
 - Fix: Viss platsinformation blir trunkerad
 - Fix: Intern routing fungerar inte längre, endast rak linje visas
 - Fix: Några problem med att skapa mappar

Obs: Om du använder intern routing måste du köra följande steg en gång efter installationen av denna utgåva: Gå till c:geo startskärm, öppna "Hantera offline data" - "Uppdatera routing data" och låt c:geo installera de uppdaterade filerna. (Orsak: BRouter routing datafil-struktur har förändrats och alla routing datafiler måste följa samma version.)

##
- Fix: Tolkning av cache-platssträng misslyckas för vissa språk på webbplatsen
- Fix: Att öppna spårbar från bevakningslistan misslyckas
- FIx: Tangentbord kan blockera listval
- Fix: Användardefinierad tile-leverantör stöder inte ytterligare URL-parametrar
- Fix: Innehav / Spårbara för en cache laddats inte längre
- Ändra: Uppdaterad intern användaragent för att ta itu med några nedladdningsproblem
- Fix: Visa spårbara detaljer tar bort den från cache-inventering

##
- Fix: dialogrutan för Offline-översättning som visas i installationer utan stöd för översättningar offline
- Fix: Koordinatformat ändras i cache/vägpunkt informationsblad
- Fix: Inloggningsdatum avskurna i listan över loggar (beroende på datumformat och teckenstorlek)
- Fix: Händelsetider kan inte upptäckas under vissa förhållanden
- Fix: Länk i listan inte klickbar under vissa villkor
- Fix: Åtgärder vid loggning av spårbara objekt blandas ibland

##
- Ändring: Maximalt antal GC-spårbara objekt som besöks per cache-logg reduceras till 100 (enligt begäran från geocaching.com för att minska deras server belastning som orsakas av extrema spårbara-älskare)
- Fix: Några möjliga säkerhetsundantag när användaren inte har beviljat vissa rättigheter (t.ex.: notifieringar)
- Fix: Cache-cirklar ofullständiga på låga zoomnivåer (endast VTM)
- Fix: Krasch vid omladdning av vägpunkter under vissa lastförhållanden
- Fix: Event-datum-filter fungerar inte under vissa omständigheter
- Fix: Max längd för loggrad fungerar inte tillförlitligt i "obegränsad" inställning
- Fix: Krasch när karta öppnas under vissa förutsättningar
- Fix: Ingen karta visas om wherigo inte har några synliga zoner
- Fix: Krasch på cachedetaljernas bildflik under vissa förutsättningar
- Fix: Kartsökningar med ogiltiga koordinater
- Fix: Vissa översättningar respekterar inte c:geo-interna språkinställningar

##
- Ändring: Sätt UnifiedMap som standardkarta för vem som helst (som del av vår färdplan till UnifiedMap) Du kan för tillfället byta tillbaka i "inställningar" - "kartkällor". Borttagning av äldre kartor är planerad till våren 2026 i våra ordinarie utgåvor.
- Fix: Favorit-kryssrutan återställs när du återgår till offline-loggskärmen
- Fix: Geofence-radie-inmatningsrutan visar decimaltal
- Fix: Synkronisering av personliga anteckningar fungerar inte
- Ändring: Ny ikon för GPX-spår/ruttimport i snabbinställningar för kartspår/rutt

##
- Fix: Negativa värden i höjddiagram skalas inte
- Fix: Koordinater nära 0 felaktiga i GPX-export
- Fix: Vissa krascher
- Försök att fixa: ANR vid start
- Försök att fixa: Saknar geocache-data på livekartan

##
- Fix: Krasch vid sökning på nyckelord
- Fix: Krasch i kartan
- Fix: Tipstext inte längre valbar
- Fix: Flera Wherigo-problem

##
- Fix: Kryptera/dekryptera en ledtråd behöver ett extra tryck initialt
- Fix: Wherigo kraschar vid läsning av gamla sparade spel
- Fix: Loggning inifrån c:geo inte ihågkommen ibland
- Fix: Saknar live-data-uppdatering för hittade & arkiverade cacher
- Fix: Vägpunkter i offline-karta visas inte ibland

##
- Fix: Okrypterade cache-tips (ändring av webbplatsen)
- Fix: Lab Adventures laddas inte i appen (webbplats ändras, du kommer att behöva uppdatera lagrade lab-äventyr för att kunna använda dem från c:geo igen)
- Fix: UnifiedMap VTM: Växla 3D-byggnader fungerar inte för kombinerade kartor
- Fix: Offline-översättning: Listspråk detekteras ibland som --

##
- Fix: Krasch i översättningsmodulen
- Fix: Inloggningsdetektering misslyckas (webbplatsändring)
- Fix: Krasch vid hämtning av Wherigo-cartridge
- Fix: "Ladda mer" tar inte hänsyn till offline-filter

##
- Fix: Inventarie med spårbara inte laddat vid loggning av en cache

##
- Fix: Migrering av användardefinierade cacher under c:geo uppstart misslyckas => tog bort det tills vidare
- Fix: Avslutade Wherigo-uppgifter inte markerade som färdiga eller misslyckade































