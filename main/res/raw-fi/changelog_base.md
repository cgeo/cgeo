### Kätkön tiedot

- Uusi: Uudelleen suunniteltu koordinaattien laskin (tukevat toimintoja)
- Muutos: Muuttujat reittipisteen laskennassa ovat nyt kätkö-globaaleja
- Uusi: Muuttujien välilehti kätkön tiedoissa
- Uusi: Reittipisteiden luonti käyttäen kaavoja ja muuttujia tietyn alueen sisällä
- Uusi: Lokipohjat offline-lokeille
- Uusi: Lisää \[location\] lokimallien valikkoon
- Uusi: Salli lokitekstien valinta
- Fix: GC checker linkki johtaa silmukkaan tietyissä olosuhteissa Android 12
- Uusi: Lisätty geochecker-painike kuvauksen lopussa (tarvittaessa)
- Uusi: Lisätty 'kirjaa selaimessa' -vaihtoehto kätkövalikkoon

### Kätkölista

- Uusi: Lisätty vaihtoehto "on käyttäjän määrittämät reittipisteet" edistyneisiin tilasuodattimiin
- Uusi: Salli ilman D/T:tä olevien kätköjen sisällyttäminen suodattimeen
- Korjaa: Lajittele kätkölista uudelleen sijainnin muuttuessa kun lista lajitellaan etäisyyden mukaan

### Kartta

- Uusi: Google Mapsin karttateema
- Uusi: Kartan skaalaus vaihtoehtoja OpenStreetMapille (katso teeman vaihtoehdot)
- Muutos: Asetukset => Kartta => Pitkä napautus kartalla ottaa nyt käyttöön/pois käytöstä myös kartan pitkä napautuksen (liittyy luoda uusia reittipisteitä nykyiselle kätkölle)
- Muutos: Älä näytä etäisyysympyrää arkistoiduille kätköille
- Korjaa: Kaatuminen tietyissä olosuhteissa käyttäen OpenStreetMapsia
- Korjaus: Reititys tulee reagoimattomaksi, kun monet reitityslaatat on asennettu

### Yleinen

- Uusi: Suorita varmuuskopiot automaattisesti (valinnainen)
- Korjaus: Jatka valmiiden latausten tuontia
- Uusi: Lisätty konfiguroitavissa olevat pikakäynnistyspainikkeet aloitusnäyttöön, katso Asetukset => Ulkoasu
- Uusi: Päivitetty sisäinen reititys BRouter v1.6.3
- Uusi: Rajoita toistuvan paluunapin käytön tarvetta aloittamalla uusi aktiviteettipino siirryttäessä sovelluksen toiseen osaan
- Uusi: Lisää asetus salauksen purkamiseen oletusarvoisesti (sen sijaan että vain napauttamalla sitä)
- New: Support setting caches from unknown source as found locally
- Removed: Geolutin trackable service as it was discontinued
