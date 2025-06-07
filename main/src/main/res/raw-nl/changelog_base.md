(alleen nightly: "nightly" banner van het logo tijdelijk verwijderd tijdens het afstemmen van het ontwerp)

### UnifiedMap-routekaart en kennisgeving van veroudering van "oude" kaarten
c:geo heeft al een tijdje een geheel nieuwe kaartimplementatie met de naam "UnifiedMap". Deze zal uiteindelijk de oude implementaties van Google Maps en Mapsforge (OpenStreetMap) vervangen. Dit is een waarschuwing van afschaffing om je te informeren over de verdere routekaart.

UnifiedMap is ongeveer een jaar geleden gepubliceerd. Het ondersteunt nog steeds Google Maps en OpenStreetMap (online + offline), maar op een volledig herwerkte technische manier en met veel spannende nieuwe functies die de "old" kaarten niet ondersteunen, waarvan sommige zijn
- Kaart rotatie voor OpenStreetMap gebaseerde kaarten (online + offline)
- Cluster popup voor Google Maps
- Verberg kaartbronnen die je niet nodig hebt
- Hoogtegrafiek voor routes en tracks
- Schakel tussen lijsten direct van kaart
- "Rijden modus" voor OpenStreetMap gebaseerde kaarten

UnfiedMap is sinds geruime tijd stabiel gebleken, dus zullen we de oude implementatie van de kaart verwijderen om de onderhoudsinspanningen van c:geo te verminderen.

Routekaart:
- "Oud" kaarten zijn verouderd - we lossen geen bugs meer op.
- UnifiedMap zal de standaard worden voor alle gebruikers in de herst van 2025.
- "Oud" kaart implementaties zullen in het najaar van 2026 worden verwijderd.

Tot dan kun je schakelen tussen de verschillende implementaties in instellingen => kaartbronnen.

### Kaart
- Nieuw: Toon geofences voor lab stadia (UnifiedMap) - "Circles" inschakelen in kaart snelle instellingen om ze te tonen
- Nieuw: Optie om cirkels met individuele straal in te stellen op waypoints ("geofence" contextoptie)
- Oplossing: Kaartweergave niet bijgewerkt bij het verwijderen van cache van de huidig getoonde lijst
- Oplossing: Aantal caches in lijst kiezer niet bijgewerkt bij wijziging van lijst inhoud
- Wijziging: Houd de huidige viewport op de kaart van een lijst, als alle caches passen in de huidige viewport
- Nieuw: Volg mijn locatie in de hoogte kaart (UnifiedMap)
- Nieuw: "verplaatsen naar" / "kopiëren naar" acties voor "weergeven als lijst" inschakelen
- Nieuw: Ondersteun verhoging Winter thema in kaart download
- Nieuw: Adaptieve Hoogtelijnen, optionele hoge kwaliteit modus (UnifiedMap Mapsforge)
- Nieuw: Herontworpen routes/track van het dialoogvenster snelle instellingen
- Nieuw: Lange tik op kaart selectie pictogram om vorige tegel provider te selecteren (UnifiedMap)
- Nieuw: Instellen van naam toestaan voor offline kaarten in een begeleidend bestand (UnifiedMap)
- Nieuw: lang tikken op "live button inschakelen" om offline caches te laden
- Nieuw: Offline Reliëfweergave voor UnifiedMap (VTM-variant)
- Nieuw: Ondersteuning voor achtergrondkaarten (UnifiedMap)
- Oplossing: Compacte pictogrammen komen niet terug naar grote pictogrammen bij zoomen in automatische modus (UnifiedMap)
- Nieuw: Lange tik acties in cache infosheet: GC code, cache-titel, coördinaten, persoonlijke notitie/hint
- Wijziging: Schakelt lang tikken voor cache infosheet uit om kort tikken voor emoji selector om conflict op te lossen

### Cache details
- Nieuw: Offline vertaling van tekst en logs (experimenteel)
- Nieuw: Optie om de cache te delen met gebruikersgegevens (coördinaten, persoonlijke notitie)
- Oplossing: Spraakservice onderbroken bij scherm rotatie
- Oplossing: Cache details: Lijsten voor cache niet bijgewerkt na het tikken op de lijstnaam en verwijderen uit die lijst
- Oplossing: Gebruikersnotitie gaat verloren door het vernieuwen van een lab avontuur
- Wijziging: Log-datum gerelateerde variabelen zullen de gekozen datum gebruiken in plaats van de huidige datum
- Nieuw: Lange logboekvermeldingen standaard inklappen

### Wherigo speler
- Nieuw: Geïntegreerde Wherigo speler controle op ontbrekende inloggegevens
- Wijziging: Verwijderd Wherigo bug rapport (omdat fouten meestal cartridge-gerelateerd zijn, moet worden opgelost door cartridge eigenaar)
- Nieuw: Mogelijkheid om naar een zone te navigeren met behulp van kompas
- Nieuw: Mogelijkheid om coördinaten op het klembord te kopiëren
- Nieuw: Stel zone center in als doel bij het openen van de kaart (om informatie over route en afstand te krijgen)
- Nieuw: Ondersteuning voor het openen van lokale Wherigo-bestanden
- Wijziging: Lang-tik op een zone op de kaart wordt niet langer herkend. Dit staat gebruikers toe om andere dingen te doen in het gebied van de kaart dat beschikbaar is op een lange tik, bijvoorbeeld een gebruiker gedefinieerde cache te maken
- Nieuw: Toon waarschuwing als wherigo.com rapporten over een gebruikersovereenkomst ontbreekt (dit leidt tot een mislukte download van cartridge)

### Algemeen
- Nieuw: opnieuw ontworpen zoekpagina
- Nieuw: Inhoud telling filter
- Nieuw: Ondersteuning voor coördinaten in DD,DDDDDDD formaat
- Nieuw: Toon laatst gebruikte filternaam in filter dialoogvenster
- Nieuw: Coördinaatberekenaar: Functie om "x" te vervangen door het vermenigvuldigingssymbool
- Oplossing: Onjuiste hoogte (gemiddelde boven zeeniveau werd niet gebruikt)
- Oplossing: Nabijgelegen afstandslimiet instelling werkt niet goed voor kleine waarden
- Oplossing: Sorteren van cachelijsten op afstand aflopend werken niet correct
- Oplossing: Lab caches uitgesloten door D/T filter zelfs met actieve "inclusief onzeker"
- Oplossing: Kleurproblemen met menupictogrammen in lichte modus
- Nieuw: Voeg "Verwijder voorgaande gebeurtenissen" toe om "Alle" te tonen
- Nieuw: Toon connector voor "user-defined caches" als actief in bronfilter
- Nieuw: GPX export: export logs/trackables optioneel gemaakt
- Nieuw: knop toegevoegd om log sjablonen te verwijderen
- Oplossing: Importeren van lokale map krijgt willekeurige mapnaam
- Oplossing: Kaart-downloader biedt beschadigde (0 bytes) bestanden aan om te downloaden
