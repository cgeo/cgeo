Aufgrund der Play Store-Richtlinien haben wir die Android-Zielversion aktualisiert + einige der Bildschirmlayout-Routinen geändert. Dies kann zu unerwünschten Nebeneffekten führen, insbesondere bei neueren Android-Versionen. Falls du Probleme mit dieser Version von c:geo hast, melde sie bitte entweder auf [GitHub](https://github.com/cgeo/cgeo) oder per E-Mail an [support@cgeo.org](mailto:support@cgeo.org)

### Karte
- Neu: Routenoptimierung speichert Ergebnisse temporär
- Neu: Bei Aktivierung des Live-Modus bleiben Wegpunkte des aktuell gesetzten Ziels sichtbar
- Neu: Lange Tippen auf die Navigationslinie öffnet das Höhendiagramm (UnifiedMap)
- Neu: Zeige generierte Wegpunkte auf der Karte
- New: Download caches ordered by distance
- Fix: Doubling of individual route items
- New: Support for Motorider theme (VTM only)
- New: Support for transparent background display of offline maps (VTM only)

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
- New: Visualize calculated coordinates overflow in waypoint list
- New: Menu entry in waypoint list to mark certain waypoint types as visited

### Wherigo Player
- Neu: Offline-Übersetzung für Wherigos
- Neu: Verbesserter Umgang mit Schaltflächen

### Allgemein
- Neu: Freigabeoption nach dem Loggen eines Caches
- Änderung: Logoptionen "Benötigt Wartung" oder "Sollte archiviert werden" für eigene Caches ausblenden
- Korrektur: Wiederherstellung eines Backups kann Dateien von Tracks im internen Speicher und in anschließenden Sicherungen duplizieren
- Änderung: Verweise auf Twitter entfernt
- Neu: Löschen verwaister Dateien von Tracks beim Bereinigen und Wiederherstellen der Sicherung
- Neu: Warnung, wenn zu viele Caches zu einer Lesezeichenliste hinzugefügt werden sollen
- Neu: Listen beobachten/nicht beobachten
- Neu: Offline-Übersetzung mit Google Translate oder DeepL-Apps (falls installiert)
- New: Delete items from search history
- Change: Remove GCVote (service discontinued)
- New: Colored toolbar on cache details pages
- New: Select multiple bookmark lists / pocket queries to download
