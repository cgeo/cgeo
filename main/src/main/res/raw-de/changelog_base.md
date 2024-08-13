### Karte
- Neu: "Persönliche Notiz bearbeiten" von der Cache-Informationsansicht auf der Karte aus
- Korrektur: Wegpunkte werden bei Anzeige eines einzelnen Caches nicht gefiltert (UnifiedMap)
- Neu: Unterstützung für benutzerdefinierte Kartenanbieter für OSM Online-Karten
- Korrektur: Kartendaten nach dem Öffnen / Schließen von Einstellungen aktualisieren (UnifiedMap)
- Neu: Anzeige von Gebäuden in 2D oder 3D (UnifiedMap OSM Karten)
- Neu: Cache-Download/-Aktualisierung aus Infofenster im Hintergrund durchführen
- Änderung: Suche nach Koordinaten: Zeige Richtung und Distanz zum Ziel und nicht zur aktuellen Position
- Neu: Grafischer D/T-Indikator auf der Cache-Informationsseite
- Korrektur: Kompassrose versteckt, wenn Filterleiste sichtbar ist (UnifiedMap)

### Cache-Details
- Neu: In der persönlichen Notiz verlinkte Bilder auf dem "Bilder"-Reiter anzeigen
- Änderung: Vereinfachte Aktion für langes Tippen in Cache-Details und Trackable Details
- Neu: Feiner geglättete Skalierung von Logbildern
- Änderung: Geändertes Symbol für "Liste bearbeiten": Liste + Bleistift
- Korrektur: `vanity` Funktion fehlerhaft bei langen Zeichenketten
- Korrektur: Falsche Parsing-Priorität bei Formel-Backup
- Change: Allow larger integer ranges in formulas (disallow usage of negation)
- New: Allow user-stored cache images on creating/editing log
- Fix: Spoiler images no longer being loaded (website change)

### Allgemein
- Neu: Schalter, um Gefunden-Status von Lab-Adventures automatisch oder manuell zu setzen
- Neu: Listenauswahl: Automatische Gruppierung von Einträgen mit einem ':' im Namen
- Änderung: Benutze OSM Nominatum als Fallback-Geokodierer, ersetze MapQuest Geocoder (der für uns nicht mehr funktioniert)
- Neu: Internen BRouter auf v1.7.5 aktualisiert
- Neu: Höheninformationen beim Import einer Trackdatei auswerten
- Neu: API für Locus unterstützt die Cache-Größe "virtuell"
- Korrektur: Ergebnisse für Adresssuche sind nicht mehr nach Entfernung zum Zielort sortiert
- Neu: Filter "Geänderte Koordinaten"
- Änderung: TargetSDK auf 34 aktualisiert, um den bevorstehenden Play Store-Anforderungen zu entsprechen
- Neu: Eintrag "keine" zur Auswahl der Routingprofile hinzugefügt
- Änderung: Verbesserte Beschreibung für Wartungsfunktion (Verwaiste Daten löschen)
- Neu: Zeige Warnungen beim Auftreten von HTTP-Fehler 429 (zu viele Anfragen)
- Korrektur: Flackern beim Aktualisieren der Cacheliste
- New: Allow display of passwords in connector configuration
- Fix: Search for geokretys no longer working when using trackingcodes
