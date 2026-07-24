##
Čas na aktualizaci! Pokud stále používáte Android 7 nebo starší, tato aktualizace c:geo je pro vás možná poslední! S naší další verzí aplikace c:geo ukončíme podporu pro Android 5-7, abychom snížili zátěž spojenou s údržbou a mohli aktualizovat některé externí komponenty používané aplikací c:geo, které v současné době stále udržujeme. Budeme stále podporovat Android 8 až do Android 16 (a novější verze, až budou publikovány), tedy rozpětí více než osmi let historie Androidu.

- Fix: Cache/waypoint popup opening delayed on some devices
- Fix: Edit cache description does not support copy & paste
- Fix: Some crashes and "app not responding"
- Fix: Deleting of trackable log fails (website change)

##
- Fix: Deleting of log images broken (website change)
- Change: Unify track and individual route loading buttons
- Fix: Cache attributes not detected correctly under certain conditions
- Fix: Logging caches (website change)
- Fix: Logging trackables (website change)

##
- Fix: Pocket query import broken (website change)

##
- Oprava: Chyba při přístupu k trasám
- Oprava: Pád na stránce trasového bodu
- Změna: Hledání "vlastních kešek" začíná s novými filtry
- Oprava: Neuložené fáze lab dobrodružství obnovovení stránky ztrácejí informaci o tom, že byly navštíveny
- Oprava: Opakující se výzva k aktualizaci dlaždic
- Oprava: Náhodné umístění při mapování seznamu (Google Maps)

##
- Oprava: Pád aplikace v informačním okně o kešce
- Oprava: Cartridge Wherigo již nelze stahovat (změna na webu)

##
 - Změna: Soubory Wherigo nelze v současné době stáhnout, zobrazit pokyny k řešení problému
 - Oprava: Důvod smazání logu nepřekračuje limit délky
 - Novinka: Rozšířené protokolování pro pády ve správci stahování
 - Oprava: Informační list o trasovém bodu může být příliš dlouhý, tlačítka jsou nedostupná
 - Oprava: Některé informace o poloze jsou zkráceny
 - Oprava: Interní směrování již nefunguje, pouze zobrazená přímka
 - Oprava: Některé problémy s vytvořením složky

Poznámka: Pokud používáte interní navigaci, budete po instalaci této verze muset jednou provést následující krok: Přejděte na úvodní obrazovku aplikace c:geo, otevřete položku „Spravovat offline data“ – „Aktualizovat navigační data“ a nechte aplikaci c:geo nainstalovat aktualizované soubory. (Důvod: Struktura souborů s daty pro směrování v BRouteru se změnila a všechny soubory s daty pro směrování musí odpovídat stejné verzi.)

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































