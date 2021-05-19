## Beta-versio 2021.05.11-RC

### Geokätköilypalvelut
- Uusi: Lisätty yhdistin Adventure Lab -kätköihin - Näyttää Adventure Labsin perustiedot kartalla ja haussa (vain PM). Käytä kätkön tietosivulla olevaa linkkiä avataksesi Adventure Lab -sovelluksen pelataksesi seikkailua.

### Kätkön tiedot
- Uusi: Pitkä painallus reittipisteen koordinaattien päällä niiden kopioimiseksi
- Uusi: Vie ja tuo käyttäjän määrittämät kätköt tyhjillä koordinaateilla
- Uusi: Tuki löydetyn tilan muuttamiseen käyttäjän määrittelemille kätköille ja Lab-seikkailuille
- Uusi: Reittipisteiden jäsennys muistiinpanoista löytyvästä yhtälöstä
- Uusi: Lisätty laskettujen koordinaattien indikaattori reittipisteluettelossa


### Kartta
- Uusi: Automaattinen ladattujen kartta- ja teematiedostojen päivitysten tarkistus (valinnainen)
- Uusi: BRouter: Näytä info-viesti puuttuvista reititystiedoista
- Uusi: Vie yksittäinen reitti poluksi (lisäksi "Vie reittinä")

### Sisäinen reititysmoottori
- Uusi: Integroitu BRouter reititysmoottori - nyt voit käyttää joko ulkoista BRouter sovellusta tai integroitua reititysmoottoria
- Uusi: Integroitu reititysmoottori tukee automaattista puuttuvien reitityslaattojen latausta
- Uusi: Integroitu reititysmoottori tukee aitomaattista ladattujen reitityslaattojen päivityksiä
- Uusi: Integroitu reititysmoottori tukee eri reititysprofiilien valintaa


### Muu
- Muutos: "Lajittele yksittäinen reitti" sulkeutuu automaattisesti tallennuksen yhteydessä ja tarkistaa tallentamattomat muutokset käyttämällä takanuolta
- Korjaus: Pari teemaongelmaa, kuten: teemojen yhteenlinjaus Google Maps:iin ja asetuksiin "muu sovellus"
- Korjaus: Optimoi yleinen haku: Jos matkaajan vastaavaa seurantakoodia ei löydy, suorita sen jälkeen online-haku kätkön nimellä
- Korjaus: Vältä profiilikuvan näyttämistä liian leveänä jatyöntämällä "Päivitä / poista valtuutus" -toimintoa sivuun
- Korjaus: Korjaa muuntovirhe joissakin etäisyysasetuksissa imperiaalisille yksiköille
- Uusi: Virheenkorjausnäkymä odottaville latauksille
- Korjaus: Käyttäjän valitsemaa kansiota ei saada haltuun määrityksellä ohjattuna vanhemmilla laitteilla
- Korjaus: Karttateemojen skannaus ajetaan nyt taustatehtävänä käynnistettäessä
- Korjaus: Karttalähteen vaihtaminen asetusten kautta tunnistettiin vain täyden uudelleenkäynnistyksen jälkeen
- Korjaus: Kaatuminen käytettäessä "Näytä asetukset" tietyissä olosuhteissa
- Korjaus: Takanuoli kartan lataajassa palaa päänäytölle
- Korjaus: Vältä outoja ponnahdusikkunaviestejä yhdistäessä kuvaa lokiin
- Korjaus: Mahdollinen kaatuminen kartalla
