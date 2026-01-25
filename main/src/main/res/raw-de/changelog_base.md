Edge to Edge: Aufgrund der Richtlinien des Play Store haben wir die Android-API-Stufe dieser Version von c:geo aktualisiert und einige Routinen für das Bildschirmlayout geändert. Dies kann zu unerwünschten Nebeneffekten führen, insbesondere bei neueren Android-Versionen. Falls du Probleme mit dieser Version von c:geo hast, melde sie bitte entweder auf [GitHub](https://github.com/cgeo/cgeo) oder per E-Mail an [support@cgeo.org](mailto:support@cgeo.org)

Legacy-Karten: Wie in den Versionen 2025.07.17 und 2025.12.01 angekündigt, haben wir endgültig die Legacy-Implementierungen für unsere Karten entfernt. Sie werden automatisch zu unserem neuen UnifiedMap weitergeleitet und sollten außer einigen neuen Funktionen keine Unterschiede bemerken. Einige davon sind
- Kartendrehung für OpenStreetMap-basierte Karten (online und offline)
- Auswahl-Popups für Google Maps bei überlappenden Caches/Wegpunkten
- Kartenquellen ausblenden, die nicht benötigt werden
- Höhendiagramm für Routen und Tracks
- Wechseln zwischen Listen direkt auf der Karte
- "Fahrmodus" für OpenStreetMap-basierte Karten
- Lange auf Strecke/einzelne Route tippen, um weitere Optionen anzuzeigen

### Karte
- Neu: Routenoptimierung speichert Ergebnisse temporär
- Neu: Bei Aktivierung des Live-Modus bleiben Wegpunkte des aktuell gesetzten Ziels sichtbar
- Neu: Lange Tippen auf die Navigationslinie öffnet das Höhendiagramm (UnifiedMap)
- Neu: Zeige generierte Wegpunkte auf der Karte
- Neu: Caches nach Entfernung sortiert herunterladen
- Korrektur: Verdopplung einzelner Routenelemente
- Neu: Unterstützung für Motorider Design (nur VTM)
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
- Neu: Remote-URI bei ausstehenden Downloads anklickbar
- Änderung: Systemeinstellung als Standard-Design für neue Installationen verwenden
- Neu: GPX-Export: GSAK Lat/LonBeforeCorrect Tag beim Export von Original-Wegpunkten ergänzt
- Neu: Undo-Leiste beim Löschen von Caches aus "Liste von Karte"
