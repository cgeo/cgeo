## Beta-Version 2021.05.11-RC

### Geocaching-Dienste
- Neu: Konnektor für Adventure Lab Caches - Zeigt die Startkoordinaten von Adventure Labs mit grundlegenden Informationen auf der Karte und in der Suche (Nur Premium). Nutze den Link auf der Seite Cache-Details, um die Adventure-App zu starten.

### Cache-Details
- Neu: Lange auf Wegpunktkoordinaten klicken, um Koordinaten zu kopieren
- Neu: Benutzerdefinierte Caches mit leeren Koordinaten exportieren und importieren
- Neu: Ändern des Fund-Status für benutzerdefinierte Caches und Adventure-Labs
- Neu: Formeln für Wegpunkte in persönlicher Cache-Notiz auslesen
- Neu: Indikator für berechnete Koordinaten in Wegpunktliste hinzugefügt


### Karte
- Neu: Automatische Suche nach Updates für heruntergeladene Karten- und Design-Dateien (optional)
- New: BRouter: Show info message on missing routing data
- New: Export individual route as track (in addition to "Export as route")

### Integrated routing engine
- New: Integrated BRouter routing engine - you can now use either external BRouter app or the integrated routing engine
- New: Integrated routing engine supports automatic download of missing routing tiles
- New: Integrated routing engine supports automatic updates of downloaded routing tiles
- New: Integrated routing engine supports selecting different routing profiles


### Other
- Change: "Sort individual route" automatically closes on saving and checks for unsaved changes on using the back arrow
- Fix: A couple of theming issues, esp. aligned theming of Google Maps and settings to "rest of app"
- Fix: Optimize global search: If no trackable with matching tracking code is found, execute online cache name search afterwards
- Fix: Avoid avatar images being displayed too wide and pushing the "Update / remove authorization" functionalty aside
- Fix: Fix conversion error in some distance settings for imperial units
- New: Debug view for pending downloads
- Fix: Directory selected by user not taken over in wizard on older devices
- Fix: Scan for map themes now run as background task on startup
- Fix: Changing map source via settings being recognized after full restart only
- Fix: Crash in "View settings" under certain conditions
- Fix: Back arrow in map downloader returning to main screen
- Fix: Avoid strange popup messages when attaching image to log
- Fix: Possible crash on map
