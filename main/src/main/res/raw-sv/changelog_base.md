### Karta
- Nytt: "Redigera personlig anteckning" från cacheinformationsbladet
- Fix: Vägpunkter som inte filtreras vid kartläggning av en enstaka cache (UnifiedMap)
- Nytt: Stöd användardefinierade panelleverantörer
- Fix: Uppdatera kartdata efter att du öppnat / stängt inställningsdialogen (UnifiedMap)
- Nyhet: Växla visning av byggnader 2D/3D (UnifiedMap OSM-kartor)
- New: Cache store/refresh from popup moved into background
- Change: Search for coordinates: Show direction and distance to target and not to current position
- New: Graphical D/T indicator in cache info sheet
- Fix: Compass rose hidden when filterbar is visible (UnifiedMap)

### Cachedetaljer
- Nyhet: Visa bilder länkade i "personlig anteckning" i fliken Bilder
- Change: Simplify long-tap action in cache details and trackable details
- New: Smoother scaling of log images
- Ändra: Ändra "logg"-ikonen från penna till smiley-ikon
- Ändra: Ändra ikonen "redigera listor" från penna till lista + penna
- Fix: vanity function failing on long strings
- Fix: Wrong parsing priority in formula backup

### Allmänt
- New: Switch to set found state of Lab Adventures either manually or automatically
- Nyhet: Lista urvalsdialog: Auto-gruppera cache-listor med en ":" i deras namn
- Förändring: Använd OSM Nominatum som reserv-geocoder, ersätter MapQuest-geocoder (som inte längre fungerar för oss)
- Change: Updated integrated BRouter to v1.7.5
- Nytt: Läs höjdinformation från spår vid import
- Nyhet: API till Locus stöder nu cachestorleken "virtuell"
- Fix: Search results for a location no longer sorted by distance to target location
- New: "Corrected coordinates" filter
- Change: Updated targetSDK to 34 to comply with upcoming Play Store requirements
- New: Added "none"-entry to selection of routing profiles
- Change: Improve description for "maintenance" function (remove orphaned data)
- New: Show warnings when HTTP error 429 occurs (Too many requests)
- Fix: Flickering on cache list refresh

### Changes not included in current beta version
- New: Store map theme per tile provider (UnifiedMap)
- Change: Use elevation above mean sea level (if available, Android 14+ only)
