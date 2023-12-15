### UnifiedMap
Welkom bij onze geheel nieuwe kaart implementatie, intern genaamd "UnifiedMap". Dit is het resultaat van bijna twee jaar werk van het c:geo-team voor een volledig nieuwe kaart implementatie. De reden dat we dit begonnen zijn is dat onze oude kaart implementaties steeds moeilijker werden om te onderhouden en (min of meer) gesynchroniseerd te houden qua functies, want sommige coderonderdelen zijn tien jaar oud (of meer).

Met UnifiedMap hebben we geprobeerd dezelfde gebruikerservaring te krijgen over alle verschillende kaarttypes (waar mogelijk), terwijl de interne architectuur wordt gemoderniseerd en geharmoniseerd.

UnifiedMap biedt in principe (bijna) alle functies aan die onze oude kaart implementaties hebben, maar geeft je een paar extra functies:

- Kaart rotatie voor OpenStreetMap gebaseerde kaarten (online en offline)
- Fractionele schaling voor OpenStreetMap gebaseerde kaarten
- Cluster popup voor Google Maps
- Verberg kaart bronnen die je niet nodig hebt
- Hoogte grafiek voor routes en tracks (tik op route)

UnifiedMap heeft inmiddels de bèta-status bereikt, dus hebben we besloten om deze tot onze standaardkaart te maken voor alle nightly build gebruikers.

Alles zou moeten werken, maar er kunnen (en zullen) nog steeds bugs zijn. In geval van nood kan je wisselen tussen oude en nieuwe kaart implementaties (zie instellingen - kaartbronnen), maar we willen graag dat je de nieuwe probeert. Rapporteer alle bugs die je vindt via support ([support@cgeo.org](mailto:support@cgeo.org)) of [c:geo op GitHub](github.com/cgeo/cgeo/issues). Alle feedback is welkom!

---

Meer wijzigingen:

### Kaart
- Nieuw: Markeer bestaande downloads in downloadbeheer
- Nieuw: Toon cache gevonden status op waypoint iconen
- Nieuw: Lang-tik optie om een cache met zijn waypoints met regels te verbinden

### Cache details
- Wijziging: Maak "Wissel praten" een daadwerkelijke schakelaar

### Algemeen
- Nieuw: tikken op downloader melding opent "openstaande downloads" weergave
- Wijziging: het gebruik van wallpaper als achtergrond vereist niet langer READ_EXTERNAL_STORAGE toestemming
