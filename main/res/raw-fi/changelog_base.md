### Yleiset
- Muutos: Esitellään alhaalla sijaitseva navigaatio c:geon useimmin käytettyihin toimintoon pääsemiseksi, korvaten vanhan päävalikon
- Refactored settings to current Android specifications (work in progress, please be patient)

### Kartta
- Korjaus: ladatessa useita polkuja sisältäviä GPX-tiedostoja näytetään ne erillisinä, yhdistämättöminä kappaleina
- Muutos: Ota reitin näyttö automaattisesti käyttöön ladattaessa GPX reittitiedosto

### Kätkölista
- Uusi: Mahdollisuus valita seuraavat 20 kätköä
- New: Attributes overview (see Manage Caches => Attributes overview)

### Kätkön tiedot
- New: Pass current cache coordinates to geochecker (if supported by geochecker)

### Muu
- New: Make Settings => View Settings filterable
- New: Added GC Wizard to useful apps list
- Change: Removed barcode scanner from useful apps list and from mainscreen
- Change: Removed BRouter from useful apps list (you can still use both external and internal navigation)
- Fix: Avoid repeated update checks for maps/routing tiles with interval=0
