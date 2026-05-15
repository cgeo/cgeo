##
Time to update! If you are still using Android 7 or older, this might be the last c:geo update for you! With our next feature release of c:geo we will drop support for Android 5-7 to reduce our maintenance load and to be able to update some external components used by c:geo which we are currently still holding back. We will still be supporting Android 8 up to Android 16 then (and newer versions when they will be published), a span of more than eight years of Android history.

- Fix: Crash in cache infosheet

##
 - Change: Wherigo files cannot be downloaded currently, display mitigation instructions
 - Fix: Log delete reason does not enforce lengh limit
 - New: Extended logging for crashes in download manager
 - Fix: Waypoint infosheet can become too long, buttons unreachable
 - Fix: Some location info gets truncated
 - Fix: Internal routing no longer working, only straight line shown
 - Fix: Some folder creation issues

Note: If you are using internal routing, you will need to execute the following step once after installing this release: Go to c:geo home screen, open "Manage offline data" - "Update routing data", and let c:geo install the updated files. (Reason: BRouter routing data file structure has changed and all routing data files must comply to the same version.)

##
- Oprava: U některých jazyků webových stránek selhává analýza řetězce s umístěním kešky
- Oprava: Selhání otevření sledovatelné položky ze seznamu sledovatelných položek
- Řešení: Výběru položky ze seznamu může bránit klávesnice
- Oprava: Uživatelem definovaný poskytovatel dlaždic nepodporuje další parametry URL
- Oprava: Inventář / sledovatelné položky keše se již nenačítají
- Změna: Aktualizován interní user-agent za účelem vyřešení některých problémů se stahováním
- Oprava: Zobrazení podrobností o sledovatelném předmětu způsobí jeho odstranění z inventáře kešky

##
- Oprava: Dialogové okno pro stažení offline překladu se zobrazovalo v instalacích bez podpory offline překladů
- Oprava: Změna formátu souřadnic v informačním listu ke kešce/trasovému bodu
- Oprava: Zkrácené datum logu v seznamu logů (v závislosti na formátu data a velikosti písma)
- Oprava: Za určitých podmínek se nezjistily časy událostí
- Oprava: Odkaz v seznamu není za určitých podmínek funkční
- Oprava: Akce související se sledovatelnými položkami se někdy zaměňují

##
- Změna: Maximální počet sledovatelných GC navštěvujících v rámci jednoho logu kešky, byl snížen na 100 (na žádost geocaching.com, aby se snížilo zatížení jejich serverů způsobené extrémními milovníky trackables)
- Oprava: Některé možné bezpečnostní výjimky, když uživatel neudělil určitá práva (např.: oznámení)
- Oprava: Neúplné kruhy kešky při nízkých úrovních přiblížení (pouze VTM)
- Oprava: Selhání při opětovném načítání trasových bodů za určitých podmínek načítání
- Oprava: Filtr data události nefunguje za určitých podmínek
- Oprava: Maximální limit řádků logu nefunguje spolehlivě v nastavení „neomezený“
- Oprava: Selhání při otevření mapy za určitých podmínek
- Oprava: Nezobrazovat mapu, pokud Wherigo nemá viditelné zóny
- Oprava: Za určitých podmínek pád na kartě s podrobnostmi o kešce
- Oprava: Vyhledávání na mapě s neplatnými souřadnicemi
- Oprava: Některé překlady nerespektují interní nastavení jazyka c:geo

##
- Změna: Sjednocená mapa je nastavena jako výchozí mapa pro všechny (v rámci našeho plánu přechodu na Sjednocenou mapu) Prozatím můžete přepnout zpět v „nastavení“ – „zdroje map“. Odstranění starých map je naplánováno na jaro 2026 v našem pravidelném vydání.
- Oprava: Při opětovném vstupu do obrazovky offline logu, se resetuje zaškrtávací políčko Oblíbené
- Oprava: Pole pro zadání poloměru Geofence zobrazuje desetinné číslo
- Oprava: Synchronizace osobních poznámek nefunguje
- Změna: Nová ikona pro import GPX trasy/cesty v rychlém nastavení trasy/cesty

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































