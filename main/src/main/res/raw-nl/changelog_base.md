(alleen nightly: "nightly" banner van het logo tijdelijk verwijderd tijdens het afstemmen van het ontwerp)

### UnifiedMap-routekaart en kennisgeving van veroudering van "oude" kaarten
c:geo heeft al een tijdje een geheel nieuwe kaartimplementatie met de naam "UnifiedMap". Deze zal uiteindelijk de oude implementaties van Google Maps en Mapsforge (OpenStreetMap) vervangen. Dit is een waarschuwing van afschaffing om je te informeren over de verdere routekaart.

UnifiedMap is ongeveer een jaar geleden gepubliceerd. Het ondersteunt nog steeds Google Maps en OpenStreetMap (online + offline), maar op een volledig herwerkte technische manier en met veel spannende nieuwe functies die de "old" kaarten niet ondersteunen, waarvan sommige zijn
- Map rotation for OpenStreetMap based maps (online + offline)
- Cluster popup for Google Maps
- Hide map sources you don't need
- Elevation chart for routes and tracks
- Switch between lists directly from map
- "Driving mode" for OpenStreetMap based maps

UnfiedMap is sinds geruime tijd stabiel gebleken, dus zullen we de oude implementatie van de kaart verwijderen om de onderhoudsinspanningen van c:geo te verminderen.

Routekaart:
- "Old" maps are in deprecation mode now - we won't fix bugs for it anymore.
- UnifiedMap will be made default for all users in fall of 2025.
- "Old" map implementations will be removed in spring 2026.

Tot dan kun je schakelen tussen de verschillende implementaties in instellingen => kaartbronnen.

### Kaart
- New: Show geofences for lab stages (UnifiedMap) - enable "Circles" in map quick settings to show them
- New: Option to set circles with individual radius to waypoints ("geofence" context menu option)
- Fix: Map view not updated when removing cache from currently shown list
- Fix: Number of cache in list chooser not updated on changing list contents
- Change: Keep current viewport on mapping a list, if all caches fit into current viewport
- New: Follow my location in elevation chart (UnifiedMap)
- New: Enable "move to" / "copy to" actions for "show as list"
- New: Support Elevate Winter theme in map downloader
- New: Adaptive hillshading, optional high quality mode (UnifiedMap Mapsforge)
- New: Redesigned routes/tracks quick settings dialog
- New: Long tap on map selection icon to select previous tile provider (UnifiedMap)
- New: Allow setting display name for offline maps in companion file (UnifiedMap)
- New: Long tap on "enable live button" to load offline caches
- New: Offline hillshading for UnifiedMap (VTM variant)
- New: Support for background maps (UnifiedMap)

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
