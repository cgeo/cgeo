## Bugfix Release

### Design
- Increase font size for text input fields
- Increase font size for some compass elements
- Use font color with higher contrast in waypoint tab
- Make quick offline log check mark visible again
- Increase font size for coordinate input fields
- Respect system font size settings also on older Android versions (5,6 and 7)

### Detaily kešky
- Fix missing cache title if cache opened via geocode or link (website change)
- Fix missing cache description on some caches

### Iné
- Show premium caches again in search results of basic members
- Fix further creation of user defined caches if some user defines caches have been loaded via GPX
- Use more common English abbreviation for traditional cache in cache type filter

## Feature Release 2021.08.15:

### Pokročilý systém filtrovania
- Predstavenie nového systému filtrovania pre aplikáciu c:geo, podpora flexibilných, kombinovateľných a ukladateľných filtrov
- K dispozícii v zoznamoch kešiek aj v zobrazení mapy
- Nová funkcia "Hľadať podľa filtra"

### Mapa
- Nové: Pri vytváraní novej používateľom definovanej kešky počas zobrazenia mapy zo zoznamu – ponúka používateľovi uložiť novú kešku do aktuálneho zoznamu (namiesto predvoleného zoznamu pre používateľom definované kešky)
- Nové: Oddelenie filtrov "Vlastné" a "Nájdené" v rýchlom nastavení mapy
- Zmena: Názov kešky sa zobrazuje aj v kontextovom okne podrobností

### Detaily kešky
- Nové: Využite kontextové okno prekladu v aplikácii Prekladač Google
- Nové: Umožňuje zmeniť priradenú ikonu v kontextovom okne detailov kešky kliknutím a podržaním (len uložené kešky)

### Preberač súborov
- Zmena: Preberanie sa teraz deje kompletne na pozadí, zobrazuje sa upozornenie
- Zmena: Úspešne prevzaté súbory automaticky nahradia existujúce súbory s rovnakým názvom
- Zmena: Ak mapa vyžadujú určitú tému, ktorá ešte nie je nainštalovaná, aplikácia c:geo automaticky prevezme a nainštaluje danú tému

### Iné
- Zmena: Kompletne sme prepracovali interné technické aspekty motívu aplikácie c: geo, aby sme mohli využívať niektoré modernejšie komponenty poskytované systémom Android. To so sebou prináša niekoľko vedľajších efektov, niektoré z nich sú neplánované. Nahláste akékoľvek chyby alebo diery na našej [stránke GitHub](https://www.github.com/cgeo/cgeo/issues), prípadne sa obráťte na podporu.
- Nové: Podpora denného/nočného režimu zo systému (voliteľné)
- Nové: Prevzatie zoznamov záložiek s geocaching.com – pozrite v ponuke položku "Zoznamy/Pocket query"
- Nové: Ignorovanie možností pre geocaching.su
- Zmena: Odstránená už neudržiavaná navigačná aplikácia RMAPS
- Oprava: Extrahovanie bodu trasy s rovnakým názvom ale odlišnými súradnicami z osobnej poznámky
- Oprava: Chyba pri extrahovaní osobnej poznámky pre bod trasy so vzorcom
- Oprava: Export vzorca do osobnej poznámky namiesto súradníc po vyplnení vzorca
- Oprava: Priečinok offline máp a tém nie je po preinštalovaní a obnovení zálohy v poriadku
- Oprava: Stopu/trasu nemožno aktualizovať
- Oprava: Chyba témy pre preberanie súborov v svetlej téme
