(tylko wersja nocna: Tymczasowo usunięto napis „nightly” z logo podczas dostrajania projektu)

### Zawiadomienie o wycofaniu mapy drogowej i „staych” map UnifiedMap
c:geo ma od pewnego czasu zupełnie nową implementację mapy o nazwie „UnifiedMap”, która ostatecznie zastąpi stare implementacje Google Maps i Mapsforge (OpenStreetMap). Jest to zawiadomienie o wycofaniu, aby poinformować Cię mapie drogowej.

UnifiedMap został opublikowany około roku temu. Nadal obsługuje Google Maps i OpenStreetMap (online + offline), ale w całkowicie przerobiony sposób techniczny. i z wieloma ekscytującymi nowymi funkcjami, których nie obsługują „stare” mapy, z których część to
- Map rotation for OpenStreetMap based maps (online + offline)
- Cluster popup for Google Maps
- Hide map sources you don't need
- Elevation chart for routes and tracks
- Switch between lists directly from map
- "Driving mode" for OpenStreetMap based maps

UnfiedMap okazała się stabilna od dłuższego czasu, dlatego usuniemy stare implementacje mapy, aby zmniejszyć wysiłek związany z utrzymaniem c:geo.

Plan działania:
- "Old" maps are in deprecation mode now - we won't fix bugs for it anymore.
- UnifiedMap will be made default for all users in fall of 2025.
- "Old" map implementations will be removed in spring 2026.

Do tego czasu możesz przełączać się między różnymi implementacjami w ustawieniach => źródła mapy.

### Mapa
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

### Szczegóły skrytki
- Nowość: Tłumaczenie offline tekstu opisu i logów (eksperymentalne)
- Nowość: Opcja udostępniania skrytki z danymi użytkownika (współrzędne, notatka osobista)
- Naprawiono: Usługa mowy była przerywana po obrócaniu ekranu
- Naprawiono: Szczegóły skrytki: Listy skrytek nie aktualizowały się po dotknięciu na nazwę listy przy usuwaniu tej skrytki z listy
- Naprawiono: Notatka użytkownika była tracona podczas odświeżania skrytki lab
- Zmiana: Teksty zastępcze związane z datą logowania będą używały wybranej daty zamiast bieżącej daty
- Nowość: Domyślne zwijanie długich wpisów dziennika

### Odtwarzacz Wherigo
- Nowość: Zintegrowane sprawdzanie danych logowania przez odtwarzacz Wherigo
- Zmiana: Usunięto raportowanie o błędzie Wherigo (ponieważ błędy są głównie związane z kartridżem, muszą być naprawione przez właściciela kartridża)
- Nowość: Możliwość przejścia do strefy przy użyciu kompasu
- Nowość: Możliwość kopiowania współrzędnych środka strefy do schowka
- Nowość: Ustaw środek strefy jako cel podczas otwierania mapy (aby uzyskać informacje o trasie i odległości dla nich)
- Nowość: Obsługa otwierania lokalnych plików Wherigo
- Zmiana: Długie dotknięcie strefy na mapie nie jest już rozpoznawane. Pozwala to użytkownikom robić inne rzeczy w obszarze strefy mapy dostępnym przy długim dotknięciu, np. utwórz skrytkę zdefiniowaną przez użytkownika
- Nowość: Wyświetlaj ostrzeżenie, jeśli wherigo.com zgłasza brak EULA (co prowadzi do nieudanego pobierania kartridża)

### Ogólne
- Nowość: Przeprojektowana strona wyszukiwania
- Nowość: Filtr stanu inwentarza
- Nowość: Obsługa współrzędnych w formacie DD,DDDDDDD
- Nowość: Pokaż ostatnio użytą nazwę filtra w oknie dialogowym
- Nowość: Kalkulator współrzędnych: Funkcja do zastępowania „x” symbolem mnożenia
- Naprawiono: Nieprawidłowa wysokość (nie używa średniej nad poziomem morza)
- Naprawiono: Ustawienie limitu szukania w pobliżu nie działało poprawnie dla małych wartości
- Naprawiono: Sortowanie list skrytek według odległości malejąco nie działało poprawnie
- Naprawiono: Skrytki lab były wyłączone przez filtr D/T nawet z aktywnym „dołącz niepewność”
- Naprawiono: Problemy z kolorami ikon menu w trybie jasnym
- Nowość: Dodaj „Usuń przeszłe wydarzenia” do listy „wszystkie”
- Nowość: Pokaż konektor dla „skrytek zdefiniowanych przez użytkownika” jako aktywny w filtrze źródłeł
- Nowość: Eksport GPX: eksport dzienników / przedmiotów podróżnych jest teraz opcjonalny
- Nowość: Dodano przycisk do usunięcia szablonów dzienników
- Naprawiono: Importowanie lokalnego pliku mapy przydzielało losową nazwę mapy
