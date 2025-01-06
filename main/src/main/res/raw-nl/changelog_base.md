Nieuw: Geïntegreerde Wherigo speler (beta) - zie menu optie op het home screen.<br> (Mogelijk wil je een [snelkoppeling configureren](cgeo-setting://quicklaunchitems_sorted) of [navigatie onderin aanpassen](cgeo-setting://custombnitem) voor eenvoudig gebruik, hiervoor is het nodig eerst de uitgebreide instellingen aan te zetten.)

### Kaart
- Nieuw: Kaartthema opslaan per tegelprovider (UnifiedMap)
- Nieuw: Markeer geselecteerde cache/waypoint (UnifiedMap)
- Nieuw: Voeg scheidingsteken toe tussen offline en online kaart bronnen
- Nieuw: Ondersteun Mapsforge als alternatief voor VTM in UnifiedMap, zie [Instellingen => Map Bronnen => Unified Map](cgeo-setting://useMapsforgeInUnifiedMap)
- Wijziging: 'Toon hoogtekaart' verplaatst naar het lange tik menu (UnifiedMap)
- Wijziging: Gebruik nieuwe reliëfweergave algoritme voor Mapsforge offline kaarten
- Nieuw: Reliëfweergave ondersteuning voor UnifiedMap Mapsforge offline kaarten
- Nieuw: Reliëfweergave ondersteuning voor UnifiedMap VTM kaarten (vereist online verbinding)
- Oplossing: Zoeken naar adres overweegt geen live modus (UnifiedMap)
- Wijziging: "volg mijn locatie" verplaatst naar de kaart, geeft meer ruimte voor de "live modus" knop
- Wijziging: Maak de lang drukken speld meer in de stijl van c:geo
- Wijziging: Offline data management functies (download kaarten, controleer op ontbrekende routering / hoogte gegevens) verplaatst naar map selectie menu => "Beheer offline gegevens"
- Oplossing: Kaart wordt niet bijgewerkt met gewijzigde caches

### Cache details
- Nieuw: Nog niet bestaande variabelen die gebruikt worden in projectie worden gemaakt in de variabele lijst
- Nieuw: Sta grote getallen toe in formules
- Nieuw: Ondersteun meer constellaties voor variabelen in formules
- Fix: Meerdere afbeeldingen in persoonlijke notitie niet toegevoegd aan het tabblad afbeeldingen
- Oplossing: Behandeling van projecties in waypoints en persoonlijke notities
- Nieuw: Lange tik op datum in het logboek haalt vorige log datum op
- Oplossing: Het resetten van een cache naar oorspronkelijke coördinaten verwijderd niet de "aangepaste coördinaten" vlag
- Nieuw: Bevestig het overschrijven van log op snelle offline log
- Nieuw: Update cache status bij het versturen van een log
- Nieuw: Gekleurde HTML-bronweergave van cache details
- Oplossing: checksum(0) retourneert foutieve waarde
- Oplossing: Log wijzigen verwijderd "vrienden" status

### Algemeen
- Wijziging: Gebruik hoogte boven het gemiddelde zeeniveau (indien beschikbaar, alleen Android 14+)
- Nieuw: Meerdere hiërarchische levels in cache lijsten toestaan
- Nieuw: Unieke pictogrammen voor geocaching.com blockparty en HQ gebeurtenistypes
- Nieuw: Stel voorkeur in voor afbeeldingsgrootte voor afbeeldingen geladen vanuit geocaching.com caches en trackables
- Oplossing: "Open in browser" werkt niet voor trackable logs
- Nieuw: Optie om gedownloade bestanden te beheren (kaarten, thema's, routering en reliëfweergave gegevens)
- Nieuw: Optie om een cache uit alle lijsten te verwijderen (= markeer als te verwijderen)
- Oplossing: Herstel coördinaten niet gedetecteerd door c:geo voor niet opgeslagen caches
- Nieuw: Sta het wissen van het filter toe als er geen benoemd filter is opgeslagen
- Oplossing: "Lege lijst" bevestiging popt up bij het starten van een pocket query download in een nieuw gemaakte lijst
- Wijziging: Eigen caches met offline logs tonen offline log indicator
- Nieuw: Configureerbare datumnotatie (bijv: cache logs), zie [Instellingen => Uiterlijk => Datumformaat](cgeo-settings://short_date_format)
- Nieuw: Leid informatie over connectoren op het startscherm om naar een connector specifiek voorkeurenscherm
- Nieuw: Extra emojis voor cachepictogrammen
- Wijziging: Cache type filter "Specials" bevat gebeurtenissen van type mega, giga, community celebration, HQ celebration, block party en maze
- Wijziging: Cache type filter "Anders" bevat GCHQ, APE en onbekende types
- Oplossing: Geschiedenis lengte en nabijheidsinstellingen delen schuifregelaar waarden
- Oplossing: Trackable logpagina toont tijd/coördinaat invoervelden voor trackables die dit niet ondersteunen
- Oplossing: Enkele crashes
