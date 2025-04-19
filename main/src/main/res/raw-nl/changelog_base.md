### UnifiedMap roadmap & "old" maps deprecation notice
c:geo has an all-new map implementation called "UnifiedMap" since some time, which will ultimately replace the old implementations of Google Maps and Mapsforge (OpenStreetMap). This is a deprecation notice to inform you about the further roadmap.

UnifiedMap got published about a year ago. It still supports Google Maps and OpenStreetMap (online + offline), but in a completely reworked technical way, and with a lot of exciting new features that the "old" maps do not support, some of which are
- Map rotation for OpenStreetMap based maps (online + offline)
- Cluster popup for Google Maps
- Hide map sources you don't need
- Elevation chart for routes and tracks
- Switch between lists directly from map
- "Driving mode" for OpenStreetMap based maps

UnfiedMap has proven to be stable since quite some time, thus we will remove the old map implementations to reduce the efforts for maintaining c:geo.

Roadmap:
- "Old" maps are in deprecation mode now - we won't fix bugs for it anymore.
- UnifiedMap will be made default for all users in fall of 2025.
- "Old" map implementations will be removed in spring 2026.

Until then, you can switch between the different implementations in settings => map sources.

### Kaart
- Nieuw: Toon geofences voor lab stadia (UnifiedMap) - "Circles" inschakelen in kaart snelle instellingen om ze te tonen
- Nieuw: Optie om cirkels met individuele straal in te stellen op waypoints ("geofence" contextoptie)
- Oplossing: Kaartweergave niet bijgewerkt bij het verwijderen van cache van de huidig getoonde lijst
- Fix: Number of cache in list chooser not updated on changing list contents
- Wijziging: Houd de huidige viewport op de kaart van een lijst, als alle caches passen in de huidige viewport
- Nieuw: Volg mijn locatie in de hoogte kaart (UnifiedMap)
- Nieuw: "verplaatsen naar" / "kopiëren naar" acties voor "weergeven als lijst" inschakelen
- Nieuw: Ondersteun verhoging Winter thema in kaart download
- Nieuw: Adaptieve Hoogtelijnen, optionele hoge kwaliteit modus (UnifiedMap Mapsforge)
- Nieuw: Herontworpen routes/track van het dialoogvenster snelle instellingen
- Nieuw: Lange tik op kaart selectie pictogram om vorige tegel provider te selecteren (UnifiedMap)
- Nieuw: Instellen van naam toestaan voor offline kaarten in een begeleidend bestand (UnifiedMap)
- Nieuw: lang tikken op "live button inschakelen" om offline caches te laden

### Cache details
- Nieuw: Offline vertaling van tekst en logs (experimenteel)
- Nieuw: Optie om de cache te delen met gebruikersgegevens (coördinaten, persoonlijke notitie)
- Oplossing: Spraakservice onderbroken bij scherm rotatie
- Oplossing: Cache details: Lijsten voor cache niet bijgewerkt na het tikken op de lijstnaam en verwijderen uit die lijst
- Oplossing: Gebruikersnotitie gaat verloren door het vernieuwen van een lab avontuur
- Wijziging: Log-datum gerelateerde variabelen zullen de gekozen datum gebruiken in plaats van de huidige datum
- Nieuw: Lange logboekvermeldingen standaard inklappen

### Wherigo player
- Nieuw: Geïntegreerde Wherigo speler controle op ontbrekende inloggegevens
- Wijziging: Verwijderd Wherigo bug rapport (omdat fouten meestal cartridge-gerelateerd zijn, moet worden opgelost door cartridge eigenaar)
- New: Ability to navigate to a zone using compass
- New: Ability to copy zone center coordinates to clipboard
- New: Set zone center as target when opening map (to get routing and distance info for it)
- Nieuw: Ondersteuning voor het openen van lokale Wherigo-bestanden
- Change: Long-tap on a zone on map is no longer recognized. This allows users to do other stuff in map zone area available on long-tap, eg: create user-defined cache

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
- New: Added button to delete log templates
- Oplossing: Importeren van lokale map krijgt willekeurige mapnaam
