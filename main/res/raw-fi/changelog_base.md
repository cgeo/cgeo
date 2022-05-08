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
- New: Allow inclusion of caches without D/T in filter
- Korjaa: Lajittele kätkölista uudelleen sijainnin muuttuessa kun lista lajitellaan etäisyyden mukaan

### Kartta

- New: Map theming for Google Maps
- New: Map scaling options for OpenStreetMap (see theme options)
- Muutos: Asetukset => Kartta => Pitkä napautus kartalla ottaa nyt käyttöön/pois käytöstä myös kartan pitkä napautuksen (liittyy luoda uusia reittipisteitä nykyiselle kätkölle)
- Muutos: Älä näytä etäisyysympyrää arkistoiduille kätköille
- Korjaa: Kaatuminen tietyissä olosuhteissa käyttäen OpenStreetMapsia
- Fix: Routing becoming unresponsive when many routing tiles are installed

### Yleinen

- Uusi: Suorita varmuuskopiot automaattisesti (valinnainen)
- Korjaus: Jatka valmiiden latausten tuontia
- Uusi: Lisätty konfiguroitavissa olevat pikakäynnistyspainikkeet aloitusnäyttöön, katso Asetukset => Ulkoasu
- Uusi: Päivitetty sisäinen reititys BRouter v1.6.3
