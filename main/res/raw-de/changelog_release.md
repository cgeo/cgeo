### Fixes
- "Add to watchlist" / "Remove from watchlist" failing (Website change)
- "Add to favorite" / "Remove from favorite" buttons not shown after "found" log
- Date in logbook cut off on larger fonts
- Filtering in live map for more rare cache types returning only few results

## Bugfix Release 2021.08.28

### Design
- Schriftgröße für Texteingabefelder vergrößert
- Schriftgröße für einige Kompass-Elemente vergrößert
- Benutze Schrift mit höherem Kontrast im Wegpunkt-Tab
- Häkchen im schnellen Offline-Log wieder sichtbar machen
- Schriftgröße für Koordinateneingabe vergrößert
- Systemeinstellungen für die Schriftgröße auch bei älteren Android-Versionen (5,6 und 7) berücksichtigen

### Cache-Details
- Fehlender Cache-Titel, wenn der Cache über Geocode oder Link geöffnet wurde (Änderung auf der Webseite)
- Fehlende Cache-Beschreibung bei einigen Caches korrigiert

### Sonstiges
- Premium-Caches erneut in den Suchergebnissen der Basismitglieder anzeigen
- Weitere Erstellung von benutzerdefinierten Caches möglich, nachdem benutzerdefinierte Caches aus einer GPX hinzugefügt wurden
- Verwende gebräuchlichere englische Abkürzung für traditionelle Cache im Cachetyp-Filter

## Feature-Version 2021.08.15:

### Erweitertes Filtersystem
- Einführung eines neuen Filtersystems in c:geo, unterstützt flexible, kombinierbare und speicherbare Filter
- Verfügbar sowohl in Cachelisten als auch in der Karte
- Neue "Suche mit Filter"-Funktion

### Karte
- Neu: Beim Erstellen eines benutzerdefinierten Caches während die Karte für eine Liste angezeigt wird: Biete dem Nutzer an den neuen Cache in der aktuellen Liste (statt in der Standardliste für benutzerdefinierte Caches) zu speichern
- Neu: Separate Filter in den Schnelleinstellungen der Karte für "Eigene" und "Gefundene" Caches
- Änderung: Zeige zusätzlich den Cache-Namen in den Popup auf der Karte

### Cache-Details
- Neu: Benutze Google Übersetzungs-Popup in der App
- Neu: Erlaube das Ändern des zugewiesenen Symbols im Popup der Cache-Details durch langes Klicken (nur gespeicherte Caches)

### Downloader
- Änderung: Downloads werden nun komplett im Hintergrund durchgeführt, eine Benachrichtigung wird angezeigt
- Änderung: Erfolgreich heruntergeladene Dateien überschreiben Dateien mit demselben Namen
- Änderung: Wenn eine Karte ein bestimmtes Kartendesign benötigt, wird dieses bei Bedarf automatisch heruntergeladen

### Sonstiges
- Änderung: Wir haben interne technische Aspekte der graphischen Oberfläche von c:geo komplett überarbeitet, um einige modernere Komponenten von Android nutzen zu können. Dies wird einige Nebenwirkungen haben, von denen einige unbeabsichtigt sind. Bitte berichte Fehler oder Unschönheiten entweder auf unserer [GitHub-Seite](https://www.github.com/cgeo/cgeo/issues) oder über unseren Support.
- Neu: Verwende Tag/Nacht-Modus des Systems (optional)
- Neu: Lesezeichenlisten von geocaching.com herunterladen - siehe "Listen / Pocket-Queries" im Hauptmenü
- Neu: Ignorier-Funktion für geocaching.su
- Änderung: Nicht mehr gepflegte RMAPS-Navigations-App entfernt
- Korrektur: Wegpunkt mit gleichem Namen, aber unterschiedliche Koordinaten aus der persönlichen Notiz extrahieren
- Korrektur: Fehler beim Extrahieren der Benutzernotiz für Wegpunkt mit Formel
- Korrektur: Exportiere Formel zu PN statt Koordinaten für abgeschlossene Formel
- Korrektur: Offline-Karte und Theme-Ordner nach Neuinstallation und Wiederherstellung der Sicherung fehlerhaft
- Fix: Track/Route kann nicht aktualisiert werden
- Korrektur: Fehler im Theme für Downloader des hellen Theme
