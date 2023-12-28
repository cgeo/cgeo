### Vereinheitlichte Karte
Willkommen bei unserer völlig neuen Kartenimplementierung, intern als "Vereinheitlichte Karte" bezeichnet. Dies ist das Ergebnis der fast zweijährigen Arbeit des c:geo Teams an einer völlig neuen Kartenimplementierung. Der Anlass für diese Arbeit war, dass unsere alten Kartenimplementierungen immer schwieriger zu pflegen und von den Funktionen her (mehr oder weniger) synchron zu halten waren, da einige Code-Teilen bereits zehn Jahre oder älter sind.

Mit der Vereinheitlichten Karte haben wir versucht, die gleiche Benutzererfahrung über alle verschiedenen Kartenarten zu erreichen (soweit möglich) und gleichzeitig die interne Architektur zu modernisieren und zu vereinheitlichen.

Die Vereinheitlichte Karte unterstützt grundsätzlich (fast) alle Funktionen unserer alten Kartenimplementierungen und bietet zugleich einige zusätzliche Funktionen:

- Kartendrehung für OpenStreetMap-basierte Karten (online und offline)
- Nicht-ganzzahlige Skalierung für OpenStreetMap-basierte Karten
- Auswahl-Popups für Google Maps bei überlappenden Caches/Wegpunkten
- Kartenquellen ausblenden, die nicht benötigt werden
- Höhendiagramm für Routen und Tracks (auf Route tippen)

Die Vereinheitlichte Karte hat inzwischen den Beta-Status erreicht, daher haben wir uns entschlossen, sie zur Standardkarte für alle Nightly-Nutzer zu machen.

Grundsätzlich sollte alles funktionieren, aber es kann (und wird) noch einige Fehler geben. Bei Bedarf kannst du zwischen alten und neuer Kartenimplementierungen wechseln (siehe Einstellungen - Kartenquellen), aber wir würden uns wünschen, dass du die neue ausprobierst. Bitte melde alle Fehler im Support ([support@cgeo.org](mailto:support@cgeo.org)) oder bei [c:geo auf GitHub](github.com/cgeo/cgeo/issues). Jede Rückmeldung ist willkommen!

---

Weitere Änderungen:

### Karte
- Neu: Hervorhebung vorhandener Downloads im Download-Manager
- Neu: Gefunden-Status bei Wegpunkt-Icons eines Caches anzeigen
- New: Long-tap option to connect a cache with its waypoints by lines
- Change: Show cache/waypoint details in a non-blocking way

### Cache-Details
- Änderung: "Sprachausgabe ein-/ausschalten" zu einem Umschalter geändert
- Change: Increased maximum log length for geocaching.com
- Fix: Cannot upload longer personal notes on opencaching sites

### Allgemein
- Tippen auf Downloader-Benachrichtigung öffnet die Ansicht "Ausstehende Downloads"
- Änderung: Verwendung des Hintergrundbildes benötigt nicht mehr die Berechtigung zum Lesen von Dateien
- New: Two column-layout for settings in landscape mode
