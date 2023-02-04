### Kaart
- Nieuw: OSM kaart bron osm.paws.cz
- Nieuw: Lezen van flopp.net GPX bestanden als tracks inschakelen
- Oplossing: Ontbrekend routeringssymbool na 'toevoegen aan route'
- Oplossing: Ontbrekende route berekening voor tussen gevoegde punten
- Nieuw: Voeg ondersteuning toe voor 'Vrijwillige MF5' OpenAndroMaps thema
- New: Add support for GeoJSON data
- Change: Use last known map position as fallback (when no GPS available and "follow my location" inactive)

### Cache details
- Nieuw: Nieuwe meer geavanceerde afbeeldingen galerij
- Oplossing: Herstel de positie in de lijst van waypoints na het bijwerken of verwijderen van waypoint
- Oplossing: Naar beneden verplaatsen bij het maken van nieuw waypoint
- Nieuw: Herken variabelen ingevoerd in waypoint gebruikersnotities
- Nieuw: Toon lab adventure knop in mystery cache details als link naar lab avontuur gedetecteerd is
- Oplossing: Verwijdering van de waypoint beschrijving is niet gesynchroniseerd voor server-zijde waypoints
- Fix: Waypoint list not updated after scan

### Algemeen
- API niveau wijzigen (compileSDK 32)
- Enkele afhankelijke bibliotheken bijwerken
- Wijziging: Gebruik een verschillend Android mechanisme voor het ontvangen van downloads (voor een betere compatibiliteit met Android 12+)
- Nieuw: Gebruik GPX bestandsnaam als lijstnaam bij import
- New: Allow import of GPX track files that do not provide a xmlns namespace tag
- New: Add monochrome launcher icon for Android 13
- New: Display geocaching.com member status on home screen
- Change: GPX-Import: Use name as geocode for 'unknown' connector
- Fix: Allow filtering for archived caches in owner search
- Fix: Line breaks sometimes missing in logbook view directly after posting a log
- Fix: Preview button displayed only with filter setting "show all" in PQ list
