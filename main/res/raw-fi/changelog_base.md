### Yleinen
- Muutos: Esitellään alhaalla sijaitseva navigaatio c:geon useimmin käytettyihin toimintoon pääsemiseksi, korvaten vanhan päävalikon

### Kartta
- Korjaus: ladatessa useita polkuja sisältäviä GPX-tiedostoja näytetään ne erillisinä, yhdistämättöminä kappaleina
- Muutos: Ota reitin näyttö automaattisesti käyttöön ladattaessa GPX reittitiedosto
- Uusi: Salli useiden polkujen näyttäminen kerralla
- Uusi: D/T -symbolit kätkökuvakkeille (valinnainen)
- Uusi: Mahdollisuus tarkistaa puuttuvat reititystiedot nykyiselle näkymälle
- Uusi: Teeman selitteet teemoille Elevate, Elements ja Freizeitkarte
- Korjaa: Uudelleenmahdollista reititys ulkoisella BRouter sovellus versiossa 1.6.3
- Fix: Avoid map duplication by map downloader in certain conditions

### Kätkölista
- Uusi: Mahdollisuus valita seuraavat 20 kätköä
- Uutta: Attribuuttien yleisnäkymä (ks. Hallitse kätköjä => Attribuuttien yleisnäkymä)
- Uusi: Lisää tuonti kirjanmerkkiluetteloista (vain GC-premium)
- Uusi: Käänteinen lajittelujärjestys pitkällä lajittelupalkin napautuksella
- Muutos: Suorita myös automaattinen etäisyyden mukaan lajittelu listoille, jotka sisältävät yli 50 kätköä sisältävät kätkösarjat (jopa 500)
- Korjaus: Käytä lyhyempää aikakatkaisua nopean vierityksen mekanismille, jotta muille asettelun elementeille aiheutuu vähemmän häiriötä

### Kätkön tiedot
- Uusi: Välitä kätkön koordinaatit geocheckeriin (jos geochecker tukee sitä)
- Uusi: Värilliset attribuuttikuvakkeet (seuraavat attribuuttiryhmät)
- Korjaa: Ongelma kuvien avaamisessa gallerian välilehdeltä ulkoisissa sovelluksissa joissakin Samsungin laitteissa
- Fix: Missing log count (website change)

### Muu
- Uusi: Pikalataa geokoodit leikepöydällä olevasta tekstistä päänäytön hakukenttään
- Uusi: lisättiin tuki käyttäjän määrittämille lokimalleille
- Uusi: Tee asetukset => Näytä asetukset suodatettavaksi
- Uusi: Ota haku käyttöön asetuksissa
- Uusi: Lisätty GC Wizard hyödyllisten sovellusten luetteloon
- Uusi: Attribuuttien suodatin: Sallii valinnan, mistä palveluista attribuutit näytetään
- Uusi: Mahdollisuus rajoittaa etäisyyttä kätköjen lähihaussa (ks. Asetukset => Palvelut)
- Muutos: Poistettiin viivakoodiskanneri hyödyllisten sovellusten listalta ja päävalikosta
- Muutos: Poistettiin BRouter hyödyllisten sovellusten luettelosta (voit silti käyttää sekä ulkoista että sisäistä navigointia)
- Korjaus: Vältä toistuvia päivitysten karttojen/reitityslaattojen tarkistuksia välillä = 0
- Korjaus: Optimoi tuki salasanojen automaattiseen täyttämiseen ulkoisesta salasanasovelluksista asetuksissa
- Korjaus: Ota käyttöön vihjeet järjestelmille, jotka käyttävät vanhempaa kuin Androidin versio 8
- Fix: Crash on long-tap on trackable code in trackable details
- Fix: Fieldnotes upload (website change)
- Refactored settings to meet current Android specifications
- Updated MapsWithMe API

