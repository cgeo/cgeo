Aufgrund der Play Store-Richtlinien haben wir die Android-Zielversion aktualisiert + einige der Bildschirmlayout-Routinen geändert. Dies kann zu unerwünschten Nebeneffekten führen, insbesondere bei neueren Android-Versionen. Falls du Probleme mit dieser Version von c:geo hast, melde sie bitte entweder auf [GitHub](https://github.com/cgeo/cgeo) oder per E-Mail an [support@cgeo.org](mailto:support@cgeo.org)

### Karte
- Neu: Routenoptimierung speichert Ergebnisse temporär
- Neu: Bei Aktivierung des Live-Modus bleiben Wegpunkte des aktuell gesetzten Ziels sichtbar
- Neu: Lange Tippen auf die Navigationslinie öffnet das Höhendiagramm (UnifiedMap)
- Neu: Zeige generierte Wegpunkte auf der Karte
- Neu: Caches nach Entfernung sortiert herunterladen
- Korrektur: Verdopplung einzelner Routenelemente
- Neu: Unterstützung für Motorider Design (nur VTM)
- Neu: Hintergrund von Offline-Karten transparent anzeigen (optional, nur VTM)
- Neu: "Keine Karte"-Kartenauswahl (zeigt keine Karte, nur Caches etc.)
- Änderung: Maximale Distanz zum Verbinden von Punkten im Positionsverlauf auf 500m gesenkt (konfigurierbar)

### Cache-Details
- Neu: Erkenne zusätzliche Zeichen in Formeln: –, ⋅, ×
- Neu: Zeitstempel eigener Logs beim Aktualisieren eines Caches beibehalten
- Neu: Optionaler Mini-Kompass (siehe Einstellungen => Cache-Details => Richtung in Cache-Detailansicht anzeigen)
- Neu: Logs der Besitzer auf der Registerkarte "Freunde/Eigene" anzeigen
- Änderung: "Freunde/Eigene" Tab zeigt die Anzahl der Logs für diesen Tab anstelle von globalen Zählern an
- Änderung: Fixierter Header in Variablen und Wegpunkt-Tabs
- Korrektur: Doppelte "Log löschen"-Einträge anzeigen
- Korrektur: c:geo stürzt in den Cache-Details ab, wenn der Bildschirm dreht
- Änderung: Kompaktere Ansicht für "Neuen Wegpunkt hinzufügen"
- Neu: Option, um Bilder von geocaching.com in "unveränderter" Größe zu laden
- Neu: Variablenansicht kann gefiltert werden
- Neu: Hinweis in Wegpunkt-Liste bei Überlauf berechneter Koordinaten
- Neu: Menüeintrag in Wegpunktliste, um bestimmte Wegpunkttypen als besucht zu markieren
- Neu: Platzhalter für Trackable Logging (Geocache-Name, Geocache-Code, Benutzer)
- Änderung: Link zum veralteten WhereYouGo Player entfernt. Verwende standardmäßig integrierten Player für Wherigos.
- Korrektur: Fehlender Umschalter im geführten Modus des Wegpunktrechners

### Wherigo Player
- Neu: Offline-Übersetzung für Wherigos
- Neu: Verbesserter Umgang mit Schaltflächen
- Neu: Automatische Speicherung des Status
- Neu: Option zum Erstellen einer Verknüpfung zum Wherigo-Player auf dem Startbildschirm des Handys

### Allgemein
- Neu: Freigabeoption nach dem Loggen eines Caches
- Änderung: Logoptionen "Benötigt Wartung" oder "Sollte archiviert werden" für eigene Caches ausblenden
- Korrektur: Wiederherstellung eines Backups kann Dateien von Tracks im internen Speicher und in anschließenden Sicherungen duplizieren
- Änderung: Verweise auf Twitter entfernt
- Neu: Löschen verwaister Dateien von Tracks beim Bereinigen und Wiederherstellen der Sicherung
- Neu: Warnung, wenn zu viele Caches zu einer Lesezeichenliste hinzugefügt werden sollen
- Neu: Listen beobachten/nicht beobachten
- Neu: Offline-Übersetzung mit Google Translate oder DeepL-Apps (falls installiert)
- Neu: Elemente aus dem Suchverlauf löschen
- Änderung: GCVote entfernen (Dienst eingestellt)
- Neu: Farbige Symbolleiste auf Cache-Detailseiten
- Neu: Auswahl mehrerer Lesezeichenlisten / Pocket queries zum Herunterladen
- Neu: Vorschau für Lesezeichen-Listen
- Änderung: Mindestens benötigte Android-Version auf Android 8 erhöht
- Neu: Standard Schnellstart-Schaltflächen für Neuinstallationen
- Korrektur: Titel in Eingabedialogen für Zahlenbereiche abgeschnitten
- Korrektur: Update-Hinweis für FOSS-nightly verweist auf Nicht-FOSS-APK
- Neu: Option "Jahr ignorieren" für Datumsfilter
