### Yleiset julkaisumerkinnät

**Reunasta reunaan**

Play-kaupan käytäntöjen vuoksi olemme päivittäneet Android API -tasoa tällä c:geo-kohteiden versiolla + olemme muuttaneet joitakin näytön asettelun rutiineja. Tästä voi ilmeentyä ei-toivottuja sivuvaikutuksia, etenkin uusimmilla Android-versioilla. Jos sinulla on ongelmia tämän c:geo-version kanssa, ilmoitathan siitä joko [GitHubiin](https://github.com/cgeo/cgeo) tai sähköpostitse osoitteeseen [support@cgeo.org](mailto:support@cgeo.org)

**Vanhat kartat**

Niin kuin julkaisuissa 2025.07.17 ja 2025.12.01 ilmoitettiin, olemme viimein poistanut karttojemme vanhat toteutukset. Uusi UnifiedMap tulee käyttöön automaattisesti ja sinun ei pitäisi huomata eroja, paitsi muutamia uusia ominaisuuksia, joihin sisältyy
- Kartan kierto OpenStreetMapsiin perustuville karttoille (online + offline)
- Ponnahdusikkuna Google Mapsille
- Piilota kartan lähteet, joita et tarvitse
- Korkeuskaavio reittejä ja jälkiä varten
- Vaihda listojen välillä suoraan kartalta
- "Ajotila" OpenStreetMap pohjaisille karttoille
- Pitkä napautus polulla / yksittäisellä reitillä lisävalintoja varten

### Kartta
- New: Route optimization caches calculated data
- New: Enabling live mode keeps waypoints of currently set target visible
- New: Long-tap on navigation line opens elevation chart (UnifiedMap)
- New: Show generated waypoints on map
- New: Download caches ordered by distance
- Korjaus: Yksittäisten reittien kohteiden kahdentuminen
- Uusi: Tuki Motorider-teemalle (vain VTM)
- Uusi: NoMap laattojen tarjoaja (ei näytä karttaa, vain kätköt jne.)
- Muutos: Maksimietäisyys pisteiden liittämiseksi jälkihistoriassa laskettu 500 metriin (muokattavissa)
- Uusi: Salli KML-tiedostojen tuonti jälkinä
- Uusi: Mahdollisuus asettaa kätkön kuvake, vaikka kätköä ei olisikaan vielä tallennettu
- Uusi: Inforuutu korkeuskaavioille joka näyttää etäisyyden, nousut ja laskut
- Uusi: Näytä reittipisteiden koordinaatit reittipisteen ponnahdusikkunassa
- Korjaus: Kartan pika-asetukset saattavat näyttää painikkeet "1"/"2" tyhjille reititysprofiileille kielen vaihtamisen jälkeen
- Uusi: Laske puuttuvat korkeustiedot tuontiraidoista (jos korkeustiedot on ladattu)
- Korjaus: Laattalatain pysähtyy tietyissä tilanteissa (vain OpenStreetMap-onlinekartat)
- New: Conditional cache markers
- New: Show navigation hint (arrow + distance)

### Kätkön tiedot
- New: Detect additional characters in formulas: –, ⋅, ×
- New: Preserve timestamp of own logs on refreshing a cache
- New: Optional compass mini view (see settings => cache details => Show direction in cache detail view)
- Uusi: Näytä omistajien lokit "ystävät/omat" välilehdessä
- Change: "Friends/own" tab shows log counts for that tab instead of global counters
- Change: Improved header in variable and waypoint tabs
- Fix: Two "delete log" items shown
- Fix: c:geo crashing in cache details when rotating screen
- Change: More compact layout for "adding new waypoint"
- New: Option to load images for geocaching.com caches in "unchanged" size
- New: Variables view can be filtered
- New: Visualize calculated coordinates overflow in waypoint list
- New: Menu entry in waypoint list to mark certain waypoint types as visited
- New: Placeholders for trackable logging (geocache name, geocache code, user)
- Change: Removed the link to outdated WhereYouGo player. Integrated Wherigo player is now default for Wherigos.
- Fix: Missing quick toggle in guided mode of waypoint calculator
- New: Aggregate functions with range support: add/sum, min/minimum, max/maximum, cnt/count, avg/average, multiply/product/prod
- Fix: Incorrect handling of DNF status for opencaching platforms
- New: Delete offline log after merge with online log
- New: Show confirmation when deleting caches with offline logs
- New: Show confirmation when deleting all caches from "All" list
- New: Allow Markdown formatting for listing text in user-defined caches
- Change: Store cache before adding user image
- Fix: Crash on loading images embedded directly in listing text
- New: Show own favorites in log view (Geocaching.com + offline logs)

### Wherigo -toistin
- New: Offline translation for Wherigos
- New: Improved button handling
- New: Status auto-save
- New: Option to create shortcout to Wherigo player on your mobile's home screen

### Yleinen
- New: Share option after logging a cache
- Change: Do not show "needs maintenance" or "needs archived" options for own caches
- Fix: Restoring a backup may duplicate track files in internal storage and subsequent backups
- Change: Removed references to Twitter
- New: Delete orphaned trackfiles on clean up and restore backup
- New: Warning on trying to add too many caches to a bookmark list
- New: Watch/unwatch list functions
- New: Offer offline translation with Google Translate or DeepL apps (if installed)
- New: Delete items from search history
- Change: Remove GCVote (service discontinued)
- New: Colored toolbar on cache details pages
- New: Select multiple bookmark lists / pocket queries to download
- New: Preview bookmark lists
- Change: Increase minimum required Android version to Android 8
- New: Default quick buttons for new installations
- Fix: Titles in range input dialogs cut off
- Fix: Notification for nightly update points to regular APK even for FOSS variant
- New: "Ignore year" option for date filters
- New: Make remote URI clickable in pending downloads
- Change: Use system-settings as default theme for new installations
- New: GPX export: Write GSAK Lat/LonBeforeCorrect annotations when exporting original waypoints
- New: Show undo bar when deleting caches from list from map
- Fix: Crahs in percentage favorite filter
- New: Make it easier to use simple lists as parent lists
- Change: Use local timezone (of device, not event) for calendar entries (instead of UTC)
- Fix: Some texts ignore language switching
- Fix: "Use imperial settings" not initialized correctly on fresh installs
- Change: Bergamot open source offline translation module replacing closed-source Google ML Kit translator
- Change: New emoji selector
