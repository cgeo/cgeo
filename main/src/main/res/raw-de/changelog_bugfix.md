##
Zeit zum Aktualisieren! Wenn du noch Android 7 oder älter verwendest, ist dies möglicherweise das letzte c:geo Update für dich! Mit unserem nächsten Feature Release von c:geo werden wir die Unterstützung für Android 5-7 einstellen, um unseren Wartungsaufwand zu reduzieren und einige von c:geo verwendete externe Komponenten aktualisieren zu können, die wir derzeit noch zurückhalten. Wir werden dann weiterhin Android 8 bis Android 16 unterstützen (und neuere Versionen, wenn sie veröffentlicht werden), was mehr als acht Jahre Android-Geschichte umfasst.

- Korrektur: Kann geänderte Koordinaten nicht hochladen (Webseitenänderung)
- Korrektur: Kann persönliche Notizen nicht hochladen (Webseitenänderung)

##
- Fix: Cache- / Wegpunkt-Popup öffnet auf einigen Geräten verzögert
- Fix: Cache-Beschreibung Bearbeiten unterstützt kein Kopieren & Einfügen
- Korrektur: Einige Abstürze und "App reagiert nicht"
- Korrektur: Löschen von Trackable-Logs schlägt fehl (Webseitenänderung)

##
- Korrektur: Löschen von Logbildern defekt (Webseitenänderung)
- Änderung: Vereinheitlichung der Buttons zum Laden von Tracks und Individueller Route
- Fix: Cache-Attribute unter bestimmten Bedingungen nicht korrekt erkannt
- Korrektur: Loggen von Caches (Webseitenänderung)
- Korrektur: Loggen von Trackables (Webseitenänderung)

##
- Korrektur: Import von Pocket Queries schlägt fehl (Webseitenänderung)

##
- Korrektur: Absturz beim Zugriff auf Routen
- Korrektur: Absturz auf Wegpunkt-Seite
- Änderung: Suche nach "eigenen Caches" beginnt mit neuen Filtern
- Korrektur: Nicht gespeicherte Lab-Wegpunkte verlieren "besucht"-Information beim Aktualisieren
- Korrektur: Wiederkehrende Aufforderung für Update von Routingdaten
- Korrektur: Zufällige Kartenposition bei Anzeige einer Liste (Google Maps)

##
- Korrektur: Absturz im Cache-Popup
- Korrektur: Wherigo-Cartridges können nicht mehr heruntergeladen werden (Webseitenänderung)

##
 - Änderung: Wherigo-Dateien können momentan nicht heruntergeladen werden, zeige Umgehungslösung an
 - Korrektur: Grund für das Löschen von Logs erzwingt keine Längenbegrenzung
 - Neu: Erweitertes Logging für Abstürze im Download-Manager
 - Korrektur: Wegpunktpopup kann zu lang werden, Buttons nicht erreichbar
 - Korrektur: Bestimmte Standortinformationen werden abgeschnitten
 - Korrektur: Internes Routing funktioniert nicht mehr, nur gerade Linie angezeigt
 - Korrektur: Probleme beim Erstellen von Ordnern

Hinweis: Wenn du das interne Routing verwendest, führe nach der Installation dieser Version einmalig folgenden Schritt aus: Gehe zum c:geo Startbildschirm, öffne "Offline-Daten verwalten" - "Routingdaten aktualisieren" und lass c:geo die Updates installieren. (Grund: BRouter Routing Datenstruktur hat sich geändert und alle Routing-Datendateien müssen der gleichen Version entsprechen.)

##
- Korrektur: Erkennung der Cacheregion schlägt bei manchen Website-Sprachen fehl
- Korrektur: Öffnen von Trackables aus der Watchlist schlägt fehl
- Korrektur: Die Tastatur blockiert ggf. die Listenauswahl
- Korrektur: Benutzerdefinierter Kartenanbieter unterstützt keine zusätzlichen URL-Parameter
- Korrektur: Inventar / Trackables eines Caches werden nicht mehr geladen/aktualisiert
- Änderung: Interner User-Agent aktualisiert, um einige Download-Probleme zu lösen
- Korrektur: Ansehen der Trackable-Details entfernt ihn aus dem Cache-Inventar

##
- Korrektur: Download-Dialog für Offline-Übersetzungen wird auch in Installationen ohne Unterstützung von Offline-Übersetzungen angezeigt
- Korrektur: Koordinatenformat wechselt auf Cache/Wegpunkt-Infoseite
- Korrektur: Logdatum in Logliste abgeschnitten (je nach Datumsformat und Schriftgröße)
- Korrektur: Event-Zeiten unter bestimmten Bedingungen nicht erkannt
- Korrektur: Link in Listings unter bestimmten Bedingungen nicht anklickbar
- Korrektur: Log-Aktionen für Trackables werden manchmal vertauscht

##
- Änderung: Maximale Anzahl von "Besuchen" von GC-Trackables pro Cache-Log auf 100 reduziert (auf Bitte von geocaching.com hin, um die Serverlast zu reduzieren, die durch extreme Trackable-Liebhaber verursacht wird)
- Korrektur: Mögliche Sicherheits-Abbrüche, wenn der Benutzer bestimmte Rechte nicht gewährt hat (z. B.: Benachrichtigungen)
- Korrektur: Cache-Kreise unvollständig bei niedrigen Zoomstufen (nur VTM)
- Fix: Crash on reloading waypoints in certain load conditions
- Fix: Event date filter not working under certain conditions
- Fix: Max log line limit not working reliably in "unlimited" setting
- Fix: Crash on opening map under certain conditions
- Fix: No map shown if wherigo has no visible zones
- Fix: Crash on cache details' image tab under certain conditions
- Fix: Map searches with invalid coordinates
- Fix: Some translations do not respect c:geo-internal language setting

##
- Change: UnifiedMap set as default map for anyone (as part of our roadmap to UnifiedMap) You can switch back in "settings" - "map sources" for the time being. Removal of legacy maps is planned for spring 2026 in our regular releases.
- Fix: Favorite checkbox gets reset on reentering offline log screen
- Fix: Geofence radius input box shows decimal number
- Fix: Syncing of personal notes not working
- Change: New icon for GPX track/route import in map track/route quick settings

##
- Fix: Negative values in elevation chart not scaled
- Fix: Coordinates near 0 broken in GPX exports
- Fix: Some crashes
- Try to fix: ANR on startup
- Try to fix: Missing geocache data on live map

##
- Fix: Crash in keyword search
- Fix: Crash in map
- Fix: Hint text no longer selectable
- Fix: Several Wherigo issues

##
- Fix: Encrypting/decrypting a hint needs an extra tap initially
- Fix: Wherigo crash on reading old saved games
- Fix: Logging from within c:geo not remembered sometimes
- Fix: Missing live data update for found & archived caches
- Fix: Waypoints in offline map are not shown sometimes

##
- Fix: Unencrypted cache hints (website change)
- Fix: Lab Adventures not loading in app (website change, you will need to update stored lab adventures to be able to call them from c:geo again)
- Fix: UnifiedMap VTM: Toggling 3D buildings doesn't work for combined maps
- Fix: Offline translation: Listing language sometimes detected as --

##
- Fix: Crash in translation module
- Fix: Login detection fails (website change)
- Fix: Crash on retrieving Wherigo cartridge
- Fix: "Load more" does not respect offline filters

##
- Fix: Trackable inventory not loaded while logging a cache

##
- Fix: Migration of user-defined caches during c:geo startup fails => removed it for the time being
- Fix: Finished Wherigo tasks not marked as finished or failed































