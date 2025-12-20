##
- Time to update! If you are still using Android 7 or older, this might be the last c:geo update for you! With our next feature release of c:geo we will drop support for Android 5-7 to reduce our maintenance load and to be able to update some external components used by c:geo which we are currently still holding back. We will still be supporting Android 8 up to Android 16 then (and newer versions when they will be published), a span of more than eight years of Android history.
- Change: Maximum number of GC trackables visiting per cache log reduced to 100 (as per request from geocaching.com to reduce their server load caused by extreme trackable lovers)
- Fix: Some possible security exceptions when user has not granted certain rights (eg.: notifications)
- Fix: Cache circles incomplete on low zoom levels (VTM only)

##
- Änderung: Die Vereinheitlichte Karte ist nun die Standardkarte für alle Nutzer (ein Schritt zur Ablösung der alten Karten). Kann vorläufig unter "Einstellungen" - "Kartenquellen" noch umgeschaltet werden. Das Entfernen der alten Kartenimplementierung ist im Rahmen unserer regulären Releases im Frühjahr 2026 geplant.
- Korrektur: Favoriten-Checkbox wird beim erneuten Öffnen des Offline-Logs zurückgesetzt
- Korrektur: Geofence Radius Eingabefeld zeigt Dezimalzahl an
- Korrektur: Synchronisation von persönlichen Notizen funktioniert nicht
- Änderung: Neues Icon für GPX-Track/Routenimport in Track-/Routen-Schnelleinstellungen auf Karte

##
- Korrektur: Negative Werte im Höhendiagramm nicht skaliert
- Korrektur: Koordinaten nahe 0 in GPX-Exporten fehlerhaft
- Korrektur: Mehrere Abstürze
- Mögliche Korrektur: Langsamer Programmstart
- Mögliche Korrektur: Fehlende Geocache-Daten auf Live-Karte

##
- Korrektur: Absturz bei der Stichwortsuche
- Korrektur: Absturz in der Karte
- Korrektur: Hinweistext nicht mehr auswählbar
- Korrektur: Mehrere Wherigo-Probleme

##
- Korrektur: Verschlüsseln/Entschlüsseln eines Hinweises erfordert initial ein extra Tippen
- Korrektur: Wherigo Absturz beim Lesen alter Spielstände
- Korrektur: Fund-Log von innerhalb c:geo manchmal nicht gespeichert
- Korrektur: Fehlendes Live-Datenupdate für gefundene & archivierte Caches
- Korrektur: Wegpunkte werden in der Offline-Karte manchmal nicht angezeigt

##
- Fix: Cache-Hinweise nicht mehr verschlüsselt (Webseiten-Änderung)
- Fix: Labcaches können in der App nicht geöffnet werden (Webseiten-Änderung, du musst gespeicherte Labcaches aktualisieren um sie erneut aus c:geo zu öffnen)
- Korrektur: UnifiedMap VTM: Umschalten von 3D-Gebäude funktioniert nicht für kombinierte Karten
- Fix: Offline-Übersetzung: Listing-Sprache manchmal als -- erkannt

##
- Korrektur: Absturz in Übersetzungsfunktion
- Korrektur: Loginerkennung fehlgeschlagen (Website-Änderung)
- Korrektur: Absturz beim Abrufen der Wherigo-Cartridge
- Fix: "Mehr laden" ignoriert Offline-Filter

##
- Korrektur: Trackable Inventar beim Loggen eines Caches nicht geladen

##
- Korrektur: Migration von benutzerdefinierten Caches während des Starts von c:geo schlägt fehl, c:geo startet dann nicht mehr => Migration bis auf Weiteres entfernt
- Korrektur: Beendete Wherigo Aufgaben nicht als beendet oder fehlgeschlagen markiert



















