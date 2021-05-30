### Kehittynyt suodatusjärjestelmä (kokeellinen)
- Esittelyssä uusi suodatusjärjestelmä c:geoon, joka tukee joustavia, yhdistettävissä olevia ja varastoitavia suodattimia
- Saatavilla sekä kätkölistoissa että karttanäkymässä
- Tämä ominaisuus on työn alla, pysy kuulolla!
- Vanhat suodatusjärjestelmät ovat edelleen käytettävissä (jonkin aikaa), mutta uusi järjestelmä korvaa ne jossain vaiheessa.

### Kartta
- Uusi: Luodessa käyttäjän määrittämää kätköä kun karttaa on pyydetty luettelosta esille: Tarjoa käyttäjälle uuden kätkön tallentamista nykyiseen luetteloon (käyttäjän määrittämien kätköjen oletuslistan sijaan)
- Change: Additionally show cache name in poup details

### Kätkön tiedot
- Uusi: Käytä Google-kääntäjän ponnahdusikkunaa sovelluksessa
- Uusi: Salli kätkön kuvakkeen muokkaus kätkön tietojen ponnahdusikkunan kautta pitkällä painalluksella (vain tallennetut kätköt)

### Lataaja
- Muutos: Ladattu tiedosto korvaa automaattisesti olemassa olevat tiedostot, joilla on sama nimi
- Muutos: Jos kartta vaatii tietyn teeman, jota ei ole vielä asennettu, c:geo lataa ja asentaa teeman automaattisesti

### Muut
- Muutos: Olemme täysin luoneet uudelleen sisäisiä c:geon teemojen muodostuksen teknisiä ratkaisuja, jotta voimme paremmin hyödyntää Androidin moderneja komponentteja. Tämä aiheuttaa joitakin sivuvaikutuksia, joista osa on tahattomia. Ilmoita virheistä tai bugeista [GitHub-sivullamme](https://www.github.com/cgeo/cgeo/issues) tai ottamalla yhteyttä tukeen.
- New: Support day / night mode from system (optional)
- New: Download bookmark lists from geocaching.com - see "Lists / pocket queries" in main menu
- New: Ignore capability for geocaching.su
- Change: Removed no longer maintained RMAPS navigation app
