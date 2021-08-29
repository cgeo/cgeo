## Virheenkorjausversio

### Ulkoasu
- Lisää tekstin syöttökenttien kirjasimen kokoa
- Lisää fontin kokoa joillekin kompassin elementeille
- Käytä kirjasinväriä korkeammalla kontrastilla reittipiste- välilehdessä
- Tee offline-tilassa tehdyn kirjauksen merkki jälleen näkyväksi
- Lisää koordinaattien syöttökenttien kirjasimen kokoa
- Käytetään järjestelmän kirjasinkokoasetuksia myös vanhemmissa Android-versioissa (5,6 ja 7)

### Kätkön tiedot
- Korjattiin puuttuva kätkön otsikko, jos kätkö avataan geokoodin tai linkin kautta (verkkosivuston muutos)
- Korjattiin joidenkin kätköjen puuttuva kätkön kuvaus

### Muut
- Näytetään jälleen Premium-kätköt perusjäsenten hakutuloksissa
- Korjattiin käyttäjän määrittämien kätköjen edelleen luonti jos jotkut niistä on aiemmin ladattu GPX-tiedostosta
- Otetaan käyttöön yleisempi englanninkielinen lyhenne tradikätkölle kätkötyyppisuodattimessa

## Ominaisuusversio 2021.08.15:

### Edistynyt suodatusjärjestelmä
- Esittelyssä uusi suodatusjärjestelmä c:geoon, joka tukee joustavia, yhdistettävissä olevia ja varastoitavia suodattimia
- Saatavilla sekä kätkölistoissa että karttanäkymässä
- Uusi: "Etsi suodatin" -toiminto

### Kartta
- Uusi: Luodessa käyttäjän määrittämää kätköä kun karttaa on pyydetty luettelosta esille: Tarjoa käyttäjälle uuden kätkön tallentamista nykyiseen luetteloon (käyttäjän määrittämien kätköjen oletuslistan sijaan)
- Uusi: Erillinen "omat" ja "löydetyt" -suodattimet kartan pika-asetuksissa
- Muutos: Näytä myös kätkön nimi kätkön ponnahdusikkunassa

### Kätkön tiedot
- Uusi: Käytä Google-kääntäjän ponnahdusikkunaa sovelluksessa
- Uusi: Salli kätkön kuvakkeen muokkaus kätkön tietojen ponnahdusikkunan kautta pitkällä painalluksella (vain tallennetut kätköt)

### Lataaja
- Muutos: Lataukset tapahtuvat nyt täysin taustalla, ilmoitus näytetään
- Muutos: Ladattu tiedosto korvaa automaattisesti olemassa olevat tiedostot, joilla on sama nimi
- Muutos: Jos kartta vaatii tietyn teeman, jota ei ole vielä asennettu, c:geo lataa ja asentaa teeman automaattisesti

### Muut
- Muutos: Olemme täysin luoneet uudelleen sisäisiä c:geon teemojen muodostuksen teknisiä ratkaisuja, jotta voimme paremmin hyödyntää Androidin moderneja komponentteja. Tämä aiheuttaa joitakin sivuvaikutuksia, joista osa on tahattomia. Ilmoita virheistä tai bugeista [GitHub-sivullamme](https://www.github.com/cgeo/cgeo/issues) tai ottamalla yhteyttä tukeen.
- Uusi: Tuki järjestelmän päivä/yötilalle (valinnainen)
- Uusi: Lataa kirjanmerkkilistat geocaching.comista - katso "Listat / Pocket Queryt" päävalikosta
- Uusi: Poistetaan geocaching.su:n käyttö
- Muutos: Poistettu RMAPS navigointisovellus, jota ei ole enää pidetty yllä
- Korjaus: Tuo reittipiste samalla nimellä, mutta eri koordinaateilla henkilökohtaisesta muistiinpanosta
- Korjaus: Bugi yhtälön sisältävän reittipisteen tuonnissa muistiinpanosta
- Korjaus: Vie yhtälö henkilökohtaiseen muistiinpanoon koordinaattien sijaan, kun yhtälö on käytettävissä
- Korjaus: Offlinekarttojen ja teemojojen kansio virheellinen asennuksen jälkeen tehdyssä varmuuskopion palauttamisessa
- Korjaus: Jälkeä/reittiä ei voi päivittää
- Korjaus: Teemoitusvirhe lataajan vaaleassa teemassa
