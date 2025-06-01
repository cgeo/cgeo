(nur nightly-Versionen: "nightly"-Banner temporär vom Logo entfernt während der Überarbeitung des Logo-Designs)

### Roadmap UnifiedMap (Vereinheitlichte Karte) & Abkündigung der alten Karten
c:geo hat seit einiger Zeit eine komplett neue Kartenimplementierung namens "UnifiedMap", die bald die alten Implementierungen von Google Maps und Mapsforge (OpenStreetMap) ersetzen wird. Dies ist ein Abkündigungs-Hinweis und informiert über die weitere Planung.

UnifiedMap wurde vor etwa einem Jahr veröffentlicht. Google Maps und OpenStreetMap (online + offline) werden weiterhin unterstützt, aber in komplett überarbeiteter technischer Art und Weise und mit vielen neuen Features, die die "alten" Karten nicht unterstützen. Einige davon sind:
- Map rotation for OpenStreetMap based maps (online + offline)
- Cluster popup for Google Maps
- Hide map sources you don't need
- Elevation chart for routes and tracks
- Switch between lists directly from map
- "Driving mode" for OpenStreetMap based maps

UnfiedMap ist seit geraumer Zeit stabil, daher werden wir die alten Kartenimplementierungen entfernen, um die Aufwände zur Pflege von c:geo zu reduzieren.

Roadmap:
- "Old" maps are in deprecation mode now - we won't fix bugs for it anymore.
- UnifiedMap will be made default for all users in fall of 2025.
- "Old" map implementations will be removed in spring 2026.

Bis dahin kannst du in Einstellungen => Kartenquellen zwischen den verschiedenen Implementierungen wechseln.

### Karte
- New: Show geofences for lab stages (UnifiedMap) - enable "Circles" in map quick settings to show them
- New: Option to set circles with individual radius to waypoints ("geofence" context menu option)
- Fix: Map view not updated when removing cache from currently shown list
- Fix: Number of cache in list chooser not updated on changing list contents
- Change: Keep current viewport on mapping a list, if all caches fit into current viewport
- New: Follow my location in elevation chart (UnifiedMap)
- New: Enable "move to" / "copy to" actions for "show as list"
- New: Support Elevate Winter theme in map downloader
- New: Adaptive hillshading, optional high quality mode (UnifiedMap Mapsforge)
- New: Redesigned routes/tracks quick settings dialog
- New: Long tap on map selection icon to select previous tile provider (UnifiedMap)
- New: Allow setting display name for offline maps in companion file (UnifiedMap)
- New: Long tap on "enable live button" to load offline caches
- New: Offline hillshading for UnifiedMap (VTM variant)
- New: Support for background maps (UnifiedMap)
- Fix: Compact icons not returning to large icons on zooming in in auto mode (UnifiedMap)

### Cache-Details
- Neu: Offline-Übersetzung von Text und Logs (experimentell)
- Neu: Option zum Teilen des Caches mit Benutzerdaten (Koordinaten, persönliche Notiz)
- Korrektur: Sprachdienst wurde bei der Bildschirmrotation unterbrochen
- Korrektur: Cache-Details: Listen für den Cache nicht aktualisiert, nachdem auf den Listennamen getippt und der Cache von dieser Liste entfernt wurde
- Korrektur: Persönliche Notiz geht beim Aktualisieren eines Lab Caches verloren
- Änderung: Logdatum-bezogene Platzhalter verwenden gewähltes statt des aktuellen Datums
- Neu: Lange Logeinträge standardmäßig verkürzt anzeigen

### Wherigo Player
- Neu: Integrierter Wherigo Player prüft auf fehlende Anmeldeinformationen
- Änderung: Mailen von Wherigo-Fehlerberichten entfernt (da Fehler meist in der Cartridges liegen und nur vom Besitzer der Cartridge behoben werden können)
- Neu: Mit Kompass zu einer Zone navigieren
- Neu: Koordinaten der Zonenmitte in die Zwischenablage kopieren
- Neu: Zonenmitte beim Öffnen der Karte als Ziel festlegen (um Routen- und Distanzinformationen dafür zu erhalten)
- Neu: Unterstützt das Öffnen lokaler Wherigo-Dateien
- Änderung: Lange Tippen auf eine Zone auf der Karte wird ignoriert. Dies ermöglicht Zugriff auf andere Funktionen mit langem Tippen, z. B. Erstellen benutzerdefinierter Caches
- Neu: Warnung anzeigen, wenn wherigo.com fehlende EULA meldet (was zum fehlgeschlagenen Download von Cartridges führt)

### Allgemein
- Neu: Neu gestaltete Suchseite
- Neu: Filter: Inventarzähler
- Neu: Unterstützung für Koordinaten im DD,DDDDDDD Format
- Neu: Zeige den letzten Filter im Filterdialog
- Neu: Koordinatenrechner: Funktion zum Ersetzen von "x" durch Multiplikationssymbol
- Korrektur: Falsche Höhe angezeigt (nicht Mittlere Höhe über Meeresspiegel)
- Korrektur: Einstellung der Annäherungswerte funktioniert nicht bei kleinen Entfernungen
- Korrektur: Sortierung der Cachelisten nach Entfernung absteigend funktioniert nicht korrekt
- Korrektur: Lab-Caches durch D/T-Filter selbst bei "Caches ohne D/T einbeziehen" ausgeschlossen
- Korrektur: Farbprobleme bei Menü-Symbolen im hellen Design
- Neu: "Vergangene Events löschen" in Liste "alle"
- Neu: Konnektor für "benutzerdefinierte Caches" als aktiv im "Herkunft"-Filter anzeigen
- Neu: GPX-Export: Exportieren von Logs / Trackables optional
- Neu: Button zum Löschen von Logtemplates hinzugefügt
- Korrektur: Beim Importieren einer lokalen Kartendatei wird ein zufälliger Kartenname vergeben
- Fix: Map downloader offering broken (0 bytes) files for download
