##
- Time to update! If you are still using Android 7 or older, this might be the last c:geo update for you! With our next feature release of c:geo we will drop support for Android 5-7 to reduce our maintenance load and to be able to update some external components used by c:geo which we are currently still holding back. We will still be supporting Android 8 up to Android 16 then (and newer versions when they will be published), a span of more than eight years of Android history.
- Change: Maximum number of GC trackables visiting per cache log reduced to 100 (as per request from geocaching.com to reduce their server load caused by extreme trackable lovers)
- Fix: Some possible security exceptions when user has not granted certain rights (eg.: notifications)
- Fix: Cache circles incomplete on low zoom levels (VTM only)
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
- Oprava: Negativní hodnoty v nadmořské výškové mapě nejsou měřítkem
- Oprava: Souřadnice blízko 0 nefungovaly při exportu GPX
- Oprava: Několik pádů
- Zkuste opravit: ANR při spuštění
- Zkuste opravit: Chybějící data kešek na živé mapě

##
- Oprava: pád při hledání klíčových slov
- Oprava: Pád v mapě
- Oprava: Text nápovědy již není volitelný
- Oprava: Několik problémů s Wherigo

##
- Oprava: Šifrování/dešifrování nápovědy vyžaduje nejprve další klepnutí
- Oprava: Pád Wherigo při čtení starých uložených her
- Oprava: Přihlašování do c:geo někdy není zapamatováno
- Oprava: Chybí aktualizace živých dat pro nalezené & archivované kešky
- Oprava: Trasové body v offline mapě se někdy nezobrazují

##
- Oprava: Nešifrované nápovědy kešky (změna webu)
- Oprava: Labky se nenačítají v aplikaci (změna webu, budete muset aktualizovat uložené labky, abyste je mohli z c:geo opět vyvolat)
- Oprava: Sjednocená mapa VTM: Přepnutí 3D budov nefunguje pro kombinované mapy
- Oprava: Offline překlad: Vyhledávání jazyka někdy detekováno jako --

##
- Oprava: Chyba v překladatelském modulu
- Oprava: Detekce přihlášení selhala (změna webu)
- Oprava: Chyba při načítání cartridge Wherigo
- Oprava: "Načíst více" nerespektuje offline filtry

##
- Oprava: Sledovatelný inventář se při zaznamenávání kešky nenačítá

##
- Oprava: Migrace uživatelsky definovaných kešek při spuštění c:geo selhala => je prozatím odstraněna
- Oprava: Dokončené úlohy Wherigo nebyly označeny jako dokončené nebo neúspěšné
























