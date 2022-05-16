### Cachedetaljer

- Nytt: Omgjord koordinat-kalkylator (stöder funktioner)
- Förändring: Variabler för waypoint-beräkning är nu cache-global
- Nytt: Fliken Variabler i cache-detaljer
- Nytt: Genererar vägpunkter med hjälp av formler och variabler med områden
- Nytt: Loggmallar för loggar offline
- Nytt: Lägg \[location\] till mallmenyn
- Nytt: Tillåt val av loggtexter
- Fix: GC checker länk som leder till loop i vissa förhållanden på Android 12
- Nytt: Tillagd geochecker-knapp i slutet av beskrivningstexten (vid behov)
- Nytt: Lagt till alternativet "Logga in webbläsare" i cache-menyn

### Cachelista

- Nytt: Lagt till alternativ för "har användardefinierade waypoints" till avancerat statusfilter
- New: Allow inclusion of caches without D/T in filter
- Fix: Omsortera cache-listan vid varje plats ändring när distanssortering används

### Karta

- New: Map theming for Google Maps
- New: Map scaling options for OpenStreetMap (see theme options)
- Förändring: Inställningar => Karta => Långtryck på kartan kommer nu att aktivera/inaktivera långtryck även i cache-kartan (relevant för att skapa nya waypoints för aktuell cache)
- Ändring: Visa inte distans-cirkel för arkiverade cacher
- Fix: Krasch i OpenStreetMap-kartor under vissa förhållanden
- Fix: Routing becoming unresponsive when many routing tiles are installed

### Allmänt

- Nytt: Utför säkerhetskopior automatiskt (valfritt)
- Fix: Återuppta importering av färdiga nedladdningar
- Nytt: Lagt till konfigurerbara snabbstartsknappar till startskärmen, se Inställningar => Utseende
- Nytt: Uppdaterad intern routing till BRouter v1.6.3
- New: Limit the need of repetitive back key usage by starting a new activity stack when changing to another part of the app
- New: Add setting to decrypt the cache hint by default (instead of only when tapping on it)