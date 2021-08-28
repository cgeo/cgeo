## Bugfix Release

### Design
- Remove transition animation when opening cache details
- Increase font size for text input fields
- Increase font size for some compass elements
- User font color with higher contrast in waypoint tab
- Make quick offline log check mark visible again
- Increase font size for coordinate input fields

### Cache details
- Fix missing cache title if cache opened via geocode or link (website change)
- Fix missing cache description on some caches

### Overig
- Show premium caches again in search results of basic members
- Fix further creation of user defined caches if some user defines caches have been loaded via GPX
- Use more common English abbreviation for traditional cache in cache type filter

## Feature Release 2021.08.15:

### Geavanceerd filtersysteem
- Introductie van een nieuw filtersysteem binnen c:geo, waarmee flexibele, combineerbare filters opgeslagen kunnen worden
- Beschikbaar in zowel cachelijsten als kaartweergave
- Nieuwe functie "Zoeken middels filter"

### Kaart
- Nieuw: Bij het maken van een door de gebruiker gedefinieerde cache tijdens het weergeven van een kaart uit een lijst: Bied gebruiker aan om een nieuwe cache op te slaan in de huidige lijst (in plaats van de standaardlijst voor caches die door de gebruiker zijn gedefinieerd)
- Nieuw: Scheid "eigen" en "gevonden" filters in snelle kaartinstellingen
- Wijziging: Ook cachenaam weergeven in de popup details

### Cache details
- Nieuw: Maak gebruik van Google translate in-app vertalingspopup
- Nieuw: Aanpassen van de toegewezen pictogram in cache details pop-up via lange tik (alleen opgeslagen caches)

### Downloader
- Wijziging: Downloads zullen nu volledig plaatsvinden in de achtergrond, er wordt een melding getoond
- Wijziging: Bestanden die met succes gedownload zijn zullen bestaande bestanden met dezelfde naam automatisch overschrijven
- Wijziging: Als een kaart een bepaald thema vereist dat nog niet is geïnstalleerd zal c:geo dit thema automatisch downloaden en installeren

### Overig
- Wijziging: We hebben de interne technische aspecten voor c:geo thema's volledig geherstructureerd om gebruik te kunnen maken van enkele modernere componenten die door Android aangeboden worden. Dit zal een aantal neveneffecten hebben, waarvan sommige onbedoeld zijn. Gelieve fouten of storingen te melden op onze [GitHub pagina](https://www.github.com/cgeo/cgeo/issues) of door contact op te nemen met support.
- Nieuw: Ondersteuning dag / nachtmodus van het systeem (optioneel)
- Nieuw: Download bladwijzerlijsten van geocaching.com - zie "Lijsten / pocket queries" in het hoofdmenu
- Nieuw: Negeer mogelijkheid voor geocaching.su
- Wijziging: De niet langer onderhouden RMAPS navigatie-app is verwijderd
- Opgelost: Waypoint extractie met dezelfde naam, maar verschillende coördinaten uit persoonlijke notitie
- Opgelost: Bug in het uitpakken van gebruikersnotitie voor waypoint met formule
- Opgelost: Exporteer formule naar PN in plaats van coördinaten voor voltooide formule
- Opgelost: Offline map en themamap onjuist na opnieuw installeren en herstellen van back-up
- Opgelost: Spoor/route kan niet worden bijgewerkt
- Opgelost: Thema-fout voor downloader in het lichte thema
