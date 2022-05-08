### Cache-Details

- Neu: Umgestalteter Koordinatenrechner (unterstützt nun auch Funktionen)
- Änderung: Variablen zur Wegpunkte-Berechnung haben nun Gültigkeit innerhalb aller Wegpunkte des Caches
- Neu: Variablen-Tab in Cache-Details
- Neu: Generierte Wegpunkte (verwendet Funktionen und Variablen mit Bereichen)
- Neu: Log-Vorlagen für Offline-Logs
- Neu: \[Standort\] als Log-Vorlage hinzugefügt
- Neu: Erlaube Markieren und Kopieren von Logeinträgen
- Korrektur: GC Geochecker-Verknüpfung führt in bestimmten Situation unter Android 12 zu Schleife
- Neu: Geochecker-Button am Ende der Beschreibung ergänzt (wenn sinnvoll)
- Neu: 'Im Browser loggen'-Option im Cachemenü ergänzt

### Cacheliste

- Neu: Option für "hat benutzerdefinierte Wegpunkte" zum erweiterten Statusfilter hinzugefügt
- New: Allow inclusion of caches without D/T in filter
- Korrektur: Nach Entfernung sortierte Cacheliste bei Positionsänderungen neu sortieren

### Karte

- New: Map theming for Google Maps
- New: Map scaling options for OpenStreetMap (see theme options)
- Änderung: Einstellungen => Karte => Langes Tippen auf Karte berücksichtigt nun auch die Cache-Karte (relevant für das Erstellen neuer Wegpunkte für den aktuellen Cache)
- Änderung: Entfernungskreis für archivierte Caches nicht anzeigen
- Korrektur: Absturz bei OpenStreetMap unter bestimmten Bedingungen
- Fix: Routing becoming unresponsive when many routing tiles are installed

### Allgemein

- Neu: Automatische Sicherungen durchführen (optional)
- Korrektur: Importieren von fertigen Downloads fortsetzen
- Neu: Konfigurierbare Schnellstart-Buttons zum Startbildschirm hinzugefügt, siehe Einstellungen => Erscheinungsbild
- Neu: Internes Routing auf BRouter v1.6.3 aktualisiert
