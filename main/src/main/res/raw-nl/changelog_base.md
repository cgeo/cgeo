Als gevolg van Play Store beleid hebben we het Android-API niveau bijgewerkt met deze versie van c:geo doelen + we hebben een aantal van de scherm-lay-out routines veranderd. Dit kan enkele ongewenste bijwerkingen hebben, vooral op nieuwere Android-versies. Als je problemen ondervindt met deze versie van c:geo, rapporteer dan op [GitHub](https://github.com/cgeo/cgeo) of via e-mail aan [support@cgeo.org](mailto:support@cgeo.org)

### Kaart
- Nieuw: Route optimalisatie caches berekende data
- Nieuw: Inschakelen van live modus houdt waypoints zichtbaar van huidig ingesteld doel
- Nieuw: Lang tikken op de navigatielijn opent de hoogtegrafiek (UnifiedMap)
- Nieuw: Toon gegenereerde waypoints op kaart
- Nieuw: Download caches gesorteerd op afstand
- Oplossing: Verdubbeling van individuele route items
- Nieuw: Ondersteuning voor Motorider thema (alleen VTM)
- Nieuw: Ondersteuning voor transparante achtergrond weergave van offline kaarten (alleen VTM)
- Nieuw: NoMap tegel provider (kaart niet tonen, gewoon caches etc.)
- Change: Max distance to connect points on history track lowered to 500m (configurable)

### Cache details
- Nieuw: Aanvullende tekens in formules detecteren: –, ⋅, ×
- Nieuw: Behoud tijdstempel van eigen logs bij het vernieuwen van een cache
- Nieuw: Optioneel kompas mini weergave (zie instellingen => cache details => Toon richting in cache detail weergave)
- Nieuw: Toon eigenaar's logs op "vrienden/eigenaar" tabblad
- Wijziging: "Vrienden/eigenaar" tab toont het aantal logs voor dat tabblad in plaats van globale tellers
- Wijziging: Verbeterde header in variabele en waypoint tabbladen
- Oplossing: Twee "verwijder log" items getoond
- Oplossing: c:geo crasht in cache details wanneer het scherm wordt gedraaid
- Wijziging: Meer compacte lay-out voor "toevoegen nieuw waypoint"
- Nieuw: Optie om afbeeldingen voor geocaching.com caches te laden in "onveranderde" grootte
- Nieuw: Variabelenweergave kan worden gefilterd
- Nieuw: Visualiseer berekende coördinaten overloop in de lijst van waypoints
- Nieuw: Menu optie in de waypointlijst om bepaalde waypoint types als bezocht te markeren
- Nieuw: Plaatshouders voor trackable logging (geocache naam, geocache code, gebruiker)
- Change: Removed the link to outdated WhereYouGo player. Integrated Wherigo player is now default for Wherigos.
- Fix: Missing quick toggle in guided mode of waypoint calculator

### Wherigo speler
- Nieuw: Offline vertaling voor Wherigos
- Nieuw: Verbeterde knoppen verwerking
- Nieuw: Status automatisch opslaan
- New: Option to create shortcout to Wherigo player on your mobile's home screen

### Algemeen
- Nieuw: Delen optie na het loggen van een cache
- Wijziging: Toon geen "need maintenance" of "need archived" opties voor eigen caches
- Oplossing: Het herstellen van een back-up kan het bijhouden van bestanden in de interne opslag en volgende back-ups dupliceren
- Wijziging: Verwijzingen naar Twitter verwijderd
- Nieuw: Verwijder verweesde trackbestanden bij opschonen en back-up herstellen
- Nieuw: Waarschuwing bij poging om te veel caches toe te voegen aan een bookmarklijst
- Nieuw: Watch/unwatch lijst functies
- Nieuw: Offlinevertaling aanbieden met Google Translate of DeepL apps (indien geïnstalleerd)
- Nieuw: Items uit zoekgeschiedenis verwijderen
- Wijziging: Verwijder GCVote (service beëindigd)
- Nieuw: Gekleurde werkbalk op de pagina met cache detailgegevens
- Nieuw: Selecteer meerdere bladwijzerlijsten / pocket queries om te downloaden
- Nieuw: Voorbeeld van bladwijzerlijsten
- Wijziging: Minimum vereiste Android versie naar Android 8 verhoogd
- Nieuw: Standaard snelle knoppen voor nieuwe installaties
- Oplossing: Titels in invoerdialoogvenster afgekapt
- Oplossing: Melding voor nightly update punten naar reguliere APK zelfs voor de FOSS-variant
- Nieuw: "Negeer jaar" optie voor datumfilters
