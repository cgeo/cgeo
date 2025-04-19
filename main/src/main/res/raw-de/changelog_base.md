### UnifiedMap roadmap & "old" maps deprecation notice
c:geo has an all-new map implementation called "UnifiedMap" since some time, which will ultimately replace the old implementations of Google Maps and Mapsforge (OpenStreetMap). This is a deprecation notice to inform you about the further roadmap.

UnifiedMap got published about a year ago. It still supports Google Maps and OpenStreetMap (online + offline), but in a completely reworked technical way, and with a lot of exciting new features that the "old" maps do not support, some of which are
- Map rotation for OpenStreetMap based maps (online + offline)
- Cluster popup for Google Maps
- Hide map sources you don't need
- Elevation chart for routes and tracks
- Switch between lists directly from map
- "Driving mode" for OpenStreetMap based maps

UnfiedMap has proven to be stable since quite some time, thus we will remove the old map implementations to reduce the efforts for maintaining c:geo.

Roadmap:
- "Old" maps are in deprecation mode now - we won't fix bugs for it anymore.
- UnifiedMap will be made default for all users in fall of 2025.
- "Old" map implementations will be removed in spring 2026.

Until then, you can switch between the different implementations in settings => map sources.

### Karte
- Neu: Geo-Begrenzung für Labstationen anzeigen (UnifiedMap) - Aktiviere "Kreise" in den Kartenschnelleinstellungen, um sie anzuzeigen
- Neu: Option zum Anzeigen von Kreisen mit individuellem Radius für Wegpunkte ("Geo-Begrenzung"-Kontextmenü-Option)
- Korrektur: Kartenansicht nicht aktualisiert beim Entfernen des Caches von der aktuell angezeigten Liste
- Fix: Number of cache in list chooser not updated on changing list contents
- Änderung: Aktuellen Viewport auf der Zuordnung einer Liste behalten, wenn alle Caches in den aktuellen Viewport passen
- Neu: Folge meinem Standort im Höhendiagramm (UnifiedMap)
- Neu: Aktiviere "Verschieben" / "Kopieren" Aktionen für "Zeige als Liste"
- Neu: Unterstützung für Elevate Winter Design im Kartendownloader
- Neu: Adaptive Kartenschattierung, optionaler Modus mit hoher Qualität (UnifiedMap Mapsforge)
- Neu: Neues Design für die Schnelleinstellungen für Routen/Tracks
- Neu: Lange auf das Kartenauswahlsymbol tippen, um den vorherigen Karten-Anbieter auszuwählen (UnifiedMap)
- Neu: Erlaubt die Einstellung des Anzeigennamens für Offline-Karten in der Begleitdatei (UnifiedMap)
- Neu: Langes Tippen auf "Live-Modus"-Icon, um Offline-Caches zu laden

### Cache-Details
- Neu: Offline-Übersetzung von Text und Logs (experimentell)
- Neu: Option zum Teilen des Caches mit Benutzerdaten (Koordinaten, persönliche Notiz)
- Korrektur: Sprachdienst wurde bei der Bildschirmrotation unterbrochen
- Korrektur: Cache-Details: Listen für den Cache nicht aktualisiert, nachdem auf den Listennamen getippt und der Cache von dieser Liste entfernt wurde
- Korrektur: Persönliche Notiz geht beim Aktualisieren eines Lab Caches verloren
- Änderung: Logdatum-bezogene Platzhalter verwenden gewähltes statt des aktuellen Datums
- Neu: Lange Logeinträge standardmäßig verkürzt anzeigen

### Wherigo player
- New: Integrated Wherigo player checking for missing credentials
- Change: Removed Wherigo bug report (as errors are mostly cartridge-related, need to be fixed by cartridge owner)
- New: Ability to navigate to a zone using compass
- New: Ability to copy zone center coordinates to clipboard
- New: Set zone center as target when opening map (to get routing and distance info for it)
- Neu: Unterstützt das Öffnen lokaler Wherigo-Dateien
- Change: Long-tap on a zone on map is no longer recognized. This allows users to do other stuff in map zone area available on long-tap, eg: create user-defined cache

### Allgemein
- Neu: Neu gestaltete Suchseite
- Neu: Filter: Inventarzähler
- Neu: Unterstützung für Koordinaten im DD,DDDDDDD Format
- Neu: Zeige den letzten Filter im Filterdialog
- Neu: Koordinatenrechner: Funktion zum Ersetzen von "x" durch Multiplikationssymbol
- Fix: Incorrect altitude (not using mean above sea level)
- Korrektur: Einstellung der Annäherungswerte funktioniert nicht bei kleinen Entfernungen
- Korrektur: Sortierung der Cachelisten nach Entfernung absteigend funktioniert nicht korrekt
- Korrektur: Lab-Caches durch D/T-Filter selbst bei "Caches ohne D/T einbeziehen" ausgeschlossen
- Korrektur: Farbprobleme bei Menü-Symbolen im hellen Design
- Neu: "Vergangene Events löschen" in Liste "alle"
- Neu: Konnektor für "benutzerdefinierte Caches" als aktiv im "Herkunft"-Filter anzeigen
- Neu: GPX-Export: Exportieren von Logs / Trackables optional
- New: Added button to delete log templates
- Korrektur: Beim Importieren einer lokalen Kartendatei wird ein zufälliger Kartenname vergeben
