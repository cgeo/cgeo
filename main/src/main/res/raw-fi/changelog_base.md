### Kartta
- Uusi: "Muokkaa henkilökohtaista muistiinpanoa" kätkön inforuudulta
- Korjaus: Reittipisteitä ei ole suodatettu yhden kätkön kartoituksessa (UnifiedMap)
- Uusi: Tuki käyttäjän määrittämille laattojen tarjoajille
- Fix: Päivitä kartan tiedot asetukset dialogin avaamisen / sulkemisen jälkeen (UnifiedMap)
- Uusi: Vaihda rakennusten näyttö 2D/3D (UnifiedMap OSM kartta)
- New: Cache store/refresh from popup moved into background
- Muutos: Etsi koordinaatteja: Näytä suunta ja etäisyys kohteeseen eikä nykyiseen sijaintiin
- Uusi: Graafinen D/T ilmaisin kätköruudussa
- Fix: Compass rose hidden when filterbar is visible (UnifiedMap)
- Change: Removed map theme legends
- Fix: Multiple navigation selection popups on long-press

### Kätkön tiedot
- Uusi: Näytä kuvat jotka ovat linkitetty "henkilökohtaisiin muistiinpanoihin" Kuvat -välilehdessä
- Muutos: Yksinkertaisempi pitkän napautuksen toiminto kätkön yksityiskohdissa ja kulkijoiden yksityiskohdissa
- Uusi: Tasaisempi lokikuvien skaalaus
- Muutos: Muuta "muokkaa luetteloita" -kuvaketta kynästä listaan + kynään
- Fix: vanity function failing on long strings
- Fix: Wrong parsing priority in formula backup
- Change: Allow larger integer ranges in formulas (disallow usage of negation)
- New: Allow user-stored cache images on creating/editing log
- Fix: Spoiler images no longer being loaded (website change)

### Yleinen
- New: Switch to set found state of Lab Adventures either manually or automatically
- New: List selection dialog: Auto-group cache lists having a ":" in their name
- Change: Use OSM Nominatum as fallback geocoder, replacing MapQuest geocoder (which is no longer working for us)
- Change: Updated integrated BRouter to v1.7.5
- New: Read elevation info from track on import
- New: API to Locus now supporting cache size "virtual"
- Fix: Search results for a location no longer sorted by distance to target location
- Uusi: "Ratkaistut koordinaatit"- suodatin
- Change: Updated targetSDK to 34 to comply with upcoming Play Store requirements
- New: Added "none"-entry to selection of routing profiles
- Change: Improve description for "maintenance" function (remove orphaned data)
- New: Show warnings when HTTP error 429 occurs (Too many requests)
- Fix: Flickering on cache list refresh
- New: Allow display of passwords in connector configuration
- Fix: Search for geokretys no longer working when using trackingcodes

### Changes not included in current beta version
- New: Store map theme per tile provider (UnifiedMap)
- Change: Use elevation above mean sea level (if available, Android 14+ only)
- New: Highlight selected cache/waypoint (UnifiedMap)
- New: Allow multiple hierarchy levels in cache lists
