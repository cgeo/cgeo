(nur nightly-Versionen: "nightly"-Banner temporär vom Logo entfernt während der Überarbeitung des Logo-Designs)

### Roadmap UnifiedMap (Vereinheitlichte Karte) & Abkündigung der alten Karten
c:geo hat seit einiger Zeit eine komplett neue Kartenimplementierung namens "UnifiedMap", die bald die alten Implementierungen von Google Maps und Mapsforge (OpenStreetMap) ersetzen wird. Dies ist ein Abkündigungs-Hinweis und informiert über die weitere Planung.

UnifiedMap wurde vor etwa einem Jahr veröffentlicht. Google Maps und OpenStreetMap (online + offline) werden weiterhin unterstützt, aber in komplett überarbeiteter technischer Art und Weise und mit vielen neuen Features, die die "alten" Karten nicht unterstützen. Einige davon sind:
- Kartendrehung für OpenStreetMap-basierte Karten (online und offline)
- Auswahl-Popups für Google Maps bei überlappenden Caches/Wegpunkten
- Kartenquellen ausblenden, die nicht benötigt werden
- Höhendiagramm für Routen und Tracks
- Wechseln zwischen Listen direkt auf der Karte
- "Fahrmodus" für OpenStreetMap-basierte Karten

UnfiedMap ist seit geraumer Zeit stabil, daher werden wir die alten Kartenimplementierungen entfernen, um die Aufwände zur Pflege von c:geo zu reduzieren.

Roadmap:
- "Alte" Karten sind jetzt abgekündigt - wir werden dazu keine Fehler mehr beheben.
- UnifiedMap wird für alle Benutzer im Herbst 2025 voreingestellt.
- "Alte" Kartenimplementierungen werden im Frühjahr 2026 entfernt.

Bis dahin kannst du in Einstellungen => Kartenquellen zwischen den verschiedenen Implementierungen wechseln.

### Karte
- Neu: Geo-Begrenzung für Labstationen anzeigen (UnifiedMap) - Aktiviere "Kreise" in den Kartenschnelleinstellungen, um sie anzuzeigen
- Neu: Option zum Anzeigen von Kreisen mit individuellem Radius für Wegpunkte ("Geo-Begrenzung"-Kontextmenü-Option)
- Korrektur: Kartenansicht nicht aktualisiert beim Entfernen des Caches von der aktuell angezeigten Liste
- Korrektur: Anzahl des Caches in der Listenauswahl wird beim Ändern der Listeninhalte nicht aktualisiert
- Änderung: Aktuellen Kartenausschnitt bei Anzeige der Caches einer Liste behalten, wenn alle Caches in den aktuellen Kartenausschnitt passen
- Neu: Folge meinem Standort im Höhendiagramm (UnifiedMap)
- Neu: Aktiviere "Verschieben" / "Kopieren" Aktionen für "Zeige als Liste"
- Neu: Unterstützung für Elevate Winter Design im Kartendownloader
- Neu: Adaptive Kartenschattierung, optionaler Modus mit hoher Qualität (UnifiedMap Mapsforge)
- Neu: Neues Design für die Schnelleinstellungen für Routen/Tracks
- Neu: Lange auf das Kartenauswahlsymbol tippen, um den vorherigen Karten-Anbieter auszuwählen (UnifiedMap)
- Neu: Erlaubt die Einstellung des Anzeigennamens für Offline-Karten in der Begleitdatei (UnifiedMap)
- Neu: Langes Tippen auf "Live-Modus"-Icon, um Offline-Caches zu laden
- Neu: Offline Hangschattierung für UnifiedMap (VTM-Variante)
- Neu: Unterstützung für Hintergrundkarten (UnifiedMap)
- Korrektur: Kompakte Icons kehren nicht zu großen Icons zurück, wenn sie im Auto-Modus eingeschoben werden (UnifiedMap)

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
- Korrektur: Map-Downloader bietet defekte (0 Bytes) Dateien zum Download an
