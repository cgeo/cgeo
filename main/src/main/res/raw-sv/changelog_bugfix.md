##
Time to update! If you are still using Android 7 or older, this might be the last c:geo update for you! With our next feature release of c:geo we will drop support for Android 5-7 to reduce our maintenance load and to be able to update some external components used by c:geo which we are currently still holding back. We will still be supporting Android 8 up to Android 16 then (and newer versions when they will be published), a span of more than eight years of Android history.

- Fix: Offline translation download dialog shown in installations without offline translation support
- Fix: Coordinate format changing in cache/waypoint info sheet
- Fix: Log date cut off in list of logs (depending on date format and font size)
- Fix: Event times not detected in certain conditions
- Fix: Link in listing not clickable under certain conditions
- Fix: Logging actions for trackables get mixed up sometimes

##
- Förändring: Maximalt antal GC-spårbara objekt som besöks per cache-logg reduceras till 100 (enligt begäran från geocaching.com för att minska deras server belastning som orsakas av extrema spårbara-älskare)
- Fix: Några möjliga säkerhetsundantag när användaren inte har beviljat vissa rättigheter (t.ex.: notifieringar)
- Fix: Cache-cirklar ofullständiga på låga zoomnivåer (endast VTM)
- Fix: Crash on reloading waypoints in certain load conditions
- Fix: Event date filter not working under certain conditions
- Fix: Max log line limit not working reliably in "unlimited" setting
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
- Ändra: Ny ikon för GPX-spår/ruttimport i snabbinställningar för kartspår/rutt

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



























