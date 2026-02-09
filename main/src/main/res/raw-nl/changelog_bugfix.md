##
Time to update! If you are still using Android 7 or older, this might be the last c:geo update for you! With our next feature release of c:geo we will drop support for Android 5-7 to reduce our maintenance load and to be able to update some external components used by c:geo which we are currently still holding back. We will still be supporting Android 8 up to Android 16 then (and newer versions when they will be published), a span of more than eight years of Android history.

- Fix: Offline translation download dialog shown in installations without offline translation support
- Fix: Coordinate format changing in cache/waypoint info sheet
- Fix: Log date cut off in list of logs (depending on date format and font size)
- Fix: Event times not detected in certain conditions

##
- Wijziging: Maximum aantal GC trackables die per cachelogboek op bezocht gezet kunnen worden, is teruggebracht tot 100 (op verzoek van geocaching.com om hun serverbelasting veroorzaakt door extreme trackable lovers te verminderen)
- Oplossing: Enkele mogelijke beveiligingsuitzonderingen wanneer de gebruiker bepaalde permissies niet heeft toegekend (bv. meldingen)
- Oplossing: Cache cirkels onvolledig op lage zoomniveaus (VTM alleen)
- Oplossing: Crash bij het herladen van waypoints in bepaalde condities
- Oplossing: Event datum filter werkt niet onder bepaalde omstandigheden
- Oplossing: Maximale logregellimiet werkt niet betrouwbaar met de instelling "onbeperkt"
- Fix: Crash on opening map under certain conditions
- Fix: No map shown if wherigo has no visible zones
- Fix: Crash on cache details' image tab under certain conditions
- Fix: Map searches with invalid coordinates
- Fix: Some translations do not respect c:geo-internal language setting

##
- Wijziging: UnifiedMap ingesteld als standaard kaart voor iedereen (als onderdeel van onze roadmap naar UnifiedMap) Je kunt voorlopig terugschakelen in "instellingen" - "kaartbronnen". Het verwijderen van oudere kaarten is gepland voor de lente van 2026 in onze reguliere releases.
- Oplossing: Favorieten checkbox wordt gereset bij het opnieuw openen van offline logboekscherm
- Oplossing: Geofencradius invoervak toont decimaal nummer
- Oplossing: Synchronisatie van persoonlijke notities werkt niet
- Wijziging: Nieuw pictogram voor GPX track/route import in kaart track/route snelle instellingen

##
- Oplossing: Negatieve waarden in de hoogte grafiek niet geschaald
- Fix: Coördinaten in de buurt van 0 gebroken in GPX export
- Oplossing: Enkele crashes
- Poging tot reparatie: ANR bij opstarten
- Poging tot reparatie: Ontbrekende geocache gegevens op de live kaart

##
- Oplossing: Crash in trefwoord zoeken
- Oplossing: Crash op kaart
- Oplossing: Hint tekst niet langer selecteerbaar
- Oplossing: Verschillende Wherigo problemen

##
- Oplossing: Versleutelen en decoderen van een hint vereist een extra tik in eerste instantie
- Oplossing: Wherigo crash bij het lezen van oude opgeslagen spellen
- Oplossing: Logging van c:geo word soms niet onthouden
- Fix: Ontbrekende live data update voor gevonden & gearchiveerde caches
- Oplossing: waypoints in offline kaart worden niet soms weergegeven

##
- Oplossing: Onversleutelde cache hints (website wijziging)
- Oplossing: Lab Avonturen laden niet in de app (website verandering, je zal opgeslagen lab avonturen moeten bijwerken om ze weer aan te kunnen roepen vanuit c:geo)
- Oplossing: UnifiedMap VTM: Wisselen van 3D-gebouwen werkt niet voor gecombineerde kaarten
- Oplossing: Offline vertaling: Listing taal soms gedetecteerd als --

##
- Oplossing: Crash in vertaalmodule
- Oplossing: Login detectie mislukt (website wijziging)
- Oplossing: Crash bij het ophalen van Wherigo cartridge
- Oplossing: "Meer laden" respecteert de offline filters niet

##
- Oplossing: Trackable inventaris niet geladen tijdens het loggen van een cache

##
- Oplossing: Migratie van door de gebruiker gedefinieerde caches tijdens c:geo opstarten mislukt => heeft het tijdelijk verwijderd
- Oplossing: Voltooide Wherigo taken niet gemarkeerd als voltooid of mislukt


























