##
- Fix: `vanity` function failing on long strings
- Fix: wrong parsing priority in formula backup

##
- Korrektur: Längenprüfung bei Logs zählt manche Zeichen doppelt
- Korrektur: hylly-Karte wird im Downloader nicht angezeigt (Webseitenänderung)
- Neu: Zusätzliche Design-Optionen für Google Maps
- Korrektur: Kompassrose durch Distanzansichten verdeckt (Google Maps v2)
- Neu: Erweiterte Protokollierung bei GC-Login-Fehlern
- Fix: Editing cache logs does not take care of existing favorite points
- Fix: "Save offline" not working after failing to edit a found log
- New: Option to limit search radius for address search
- New: Show notification for missing location permission

##
- Korrektur: Caches werden nach Aktivierung der Live-Karte nicht geladen (UnifiedMap)
- Korrektur: Option 'Aktuelle Liste verwenden' fehlt beim Erstellen eines benutzerdefinierten Caches (UnifiedMap)
- Korrektur: Kompassrose durch Distanzansichten verdeckt (UnifiedMap)
- Korrektur: Cache-Detailseite scrollt nach dem Bearbeiten der persönlichen Notiz an den Seitenanfang
- Neu: Eventdatum bei Cache-Auswahl anzeigen
- Korrektur: Die Anmeldung zu einer OC-Plattform wird vom Installationsassistenten nicht erkannt
- Korrektur: Routing funktioniert standardmäßig nicht nach einer Neuinstallation
- Korrektur: Toolbar auf Cache-Infoseite im Querformat auch auf großen Geräten verdeckt
- Korrektur: "Meinem Standort folgen" nach Zoomen mit gleichzeitigem Verschieben weiterhin aktiv (UnifiedMap)
- Korrektur: Individuelle Routen, die als Track exportiert werden, können von Garmin-Geräten nicht gelesen werden
- Korrektur: Laden von Trackables aus der internen Datenbank schlägt unter bestimmten Bedingungen fehl
- Korrektur: Route zum Navigationsziel wird beim Ändern des Routing-Modus nicht neu berechnet
- Korrektur: Fehler beim Lesen der verfügbaren Logaktionen für Trackables

##
- Korrektur: Trackable Links mit TB-Parameter funktionieren nicht
- Neu: Hinweis auf deaktivierte Stichwortsuche für Basismitglieder ergänzt
- Korrektur: Logging von Trackables funktioniert erneut nicht (Änderungen der Webseite)
- Korrektur: Höhen-Info dreht sich mit Positionsmarkierung
- Korrektur: Benutzername wird beim Login nicht erkannt, wenn bestimmte Sonderzeichen enthalten sind

##
- Korrektur: Wegpunkte ein-/ausblenden funktioniert nicht korrekt, wenn Wegpunktlimit überschritten wird (UnifiedMap)
- Korrektur: Loggen von Caches oder Trackables funktioniert aufgrund von Webseiten-Änderung nicht mehr
- Korrektur: Löschen eigener Logs funktioniert nicht

##
- Korrektur: Fundzähler wird in bestimmten Situationen aufgrund von Webseiten-Änderungen nicht korrekt erkannt
- Korrektur: Absturz beim Öffnen der Karte mit leeren Track-Dateinamen
- Korrektur: Automatische Kartendrehung weiter aktiv nach Zurücksetzen über Kompassrose (UnifiedMap)
- Korrektur: Fehlende Kompassrose in Modi zur automatischen Kartendrehung auf Google Maps (UnifiedMap)
- Korrektur: Trackable Logs können aufgrund von Änderungen der Webseite nicht geladen werden
- Änderung: "Höhe (Karte)" und "Koordinaten anzeigen" im Kartenmenü zu "Ausgewählte Position" zusammengefasst und Entfernung zur aktuellen Position ergänzt

##
- Neu: Löschen von Offline-Logs über Kontextmenü
- Korrektur: Löschen eines Offline-Logs funktioniert unter bestimmten Bedingungen nicht
- Korrektur: Filtername nach Änderungen über Schnelleinstellungen verloren
- Änderung: Trackdateien nach Namen sortieren
- Änderung: Speichere gewählte Trackable-Aktion auch bei Offline-Logs
- Korrektur: Karte springt zu 0,0 Koordinaten bei Kartenänderung (UnifiedMap)
- Korrektur: Nach Setzen eines Wegpunktes als Ziel springt die Zielanzeige wieder zurück zum Cache (UnifiedMap)
- Korrektur: "Speichern" eines Caches ohne eine Liste auszuwählen
- Korrektur: Anmeldefehler bei geocaching.com (Webseitenänderung)
- Änderung: Höhenangabe unterhalb des Positionsmarkers anzeigen (falls aktiviert)
- HINWEIS: Es gibt weitere Probleme aufgrund kürzlicher Änderungen auf der geocaching.com-Webseite, die noch nicht behoben werden konnten. Wir arbeiten daran. Auf unserer [Statusseite](https://github.com/cgeo/cgeo/issues/15555) findest du den aktuellen Fortschritt.
