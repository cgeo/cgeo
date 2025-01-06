Neu: Integrierter Wherigo Player (Beta) - siehe Menüeintrag auf dem Startbildschirm.<br> (Sie können für den einfacheren Zugriff ein [Schnellstart-Element](cgeo-setting://quicklaunchitems_sorted) oder einen [benutzerdefinierten Menüeintrag](cgeo-setting://custombnitem) konfigurieren, dazu zuerst erweiterte Einstellungen aktivieren.)

### Karte
- Neu: Kartendesign pro Kartenanbieter merken (UnifiedMap)
- Neu: Ausgewählten Cache/Wegpunkt hervorheben (UnifiedMap)
- Neu: Trennlinie zwischen Offline-und Online-Kartenquellen
- Neu: Unterstütze Mapsforge als Alternative zu VTM in UnifiedMap, siehe [Einstellungen => Kartenquellen => Vereinheitlichte Karte](cgeo-setting://useMapsforgeInUnifiedMap)
- Änderung: Höhenprofil über langes Tippen aufrufen (UnifiedMap)
- Änderung: Neuer Algorithmus für Kartenschattierung auf Mapsforge Offline-Karten
- Neu: Kartenschattierung für UnifiedMap Mapsforge Offline-Karten
- Neu: Kartenschattierung für UnifiedMap VTM Karten (erfordert Online-Verbindung)
- Korrektur: Adresssuche berücksichtigt Live-Modus nicht (UnifiedMap)
- Änderung: "Meinem Standort folgen" auf die Karte verschoben (mehr Platz für den "Live-Modus"-Button)
- Änderung: Karten-Marker bei langem Tippen mehr an den c:geo-Stil angepasst
- Änderung: Funktionen zum Verwalten von Offline-Daten (Karten-Download, Überprüfung auf fehlende Routing- / Kartenschattierungs-Daten) in das Karten-Auswahlmenü verschoben => "Offline-Daten verwalten"
- Fix: Karte aktualisiert nicht geänderte Caches

### Cache-Details
- Neu: Noch nicht vorhandene Variablen in der Projektion werden in der Variablenliste erstellt
- Neu: Erlaube große Ganzzahlen in Formeln
- Neu: Unterstütze mehr Konstellationen für Variablen in Formeln
- Korrektur: Enthält eine persönliche Notiz mehrere Bilder, werden diese nicht zum Bilder-Tab hinzugefügt
- Korrektur: Handhabung von Projektionen in Wegpunkten und persönlichen Notizen
- Neu: Langes Tippen auf das Datum beim Loggen stellt das vorherige Logdatum ein
- Korrektur: Zurücksetzen des Caches auf die ursprünglichen Koordinaten entfernt das "geänderte Koordinaten" Flag nicht
- Neu: Überschreiben des Logs beim Schnellen Offline-Log bestätigen
- Neu: Aktualisieren des Cache-Status beim Senden eines Logs
- Neu: Farbige HTML-Quellansicht der Cache-Details
- Korrektur: checksum (0) gibt falschen Wert zurück
- Korrektur: Logs bearbeiten entfernt den Status "Freunde"

### Allgemein
- Änderung: Höhe über mittlerem Meeresspiegel verwenden (falls verfügbar, nur Android 14+)
- Neu: Erlaube mehrere Hierarchieebenen in Cachelisten
- Neu: Eigene Icons für geocaching.com Blockparty und HQ Event Typen
- Neu: Bevorzugte Bildgröße für Bilder festlegen, die von geocaching.com Caches und Trackables geladen werden
- Korrektur: "Im Browser öffnen" funktioniert nicht für Trackable Logs
- Neu: Option zum Verwalten von heruntergeladenen Dateien (Karten, Designs, Routing und Kartenschattierung)
- Neu: Option, um einen Cache von allen Listen zu entfernen (= ihn als gelöscht zu markieren)
- Korrektur: Zurücksetzen der Koordinaten bei ungespeicherten Caches nicht erkannt
- Neu: Filter zurücksetzen möglich, auch wenn kein benannter Filter gespeichert ist
- Korrektur: "Leere Liste" Bestätigung erscheint beim Starten eines Pocket Query-Downloads in einer neu erstellten Liste
- Änderung: Eigene Caches mit Offline-Logs zeigen Offline-Log-Marker
- Neu: Konfigurierbares Datumsformat (z.B.: Cache-Logs), siehe [Einstellungen => Erscheinungsbild => Datumsformat](cgeo-settings://short_date_format)
- Neu: Konnektor-Informationen auf dem Startbildschirm verweisen auf jeweilige Konfigurationsseite
- Neu: Zusätzliche Emojis für Cache-Symbole
- Änderung: Cache-Filter "Besondere" umfasst Events der Typen Mega, Giga, Community Celebration, HQ Celebration, HQ Blockparty und Labyrinth
- Änderung: Cache-Filter "Andere" enthält GC HQ, APE und unbekannte Typen
- Korrektur: Schieberegler "Länge des Positionsverlaufs" und "Weite Entfernung" verwenden identischen Wert
- Korrektur: Trackable Log-Seite zeigt Zeit-/Koordinaten-Eingabefelder für Trackables an, die dies nicht unterstützen
- Korrektur: Mehrere Abstürze
