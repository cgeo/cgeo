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

### Mapa
- Nowość: Pokaż granice udzialania odpowiedzi (geofence) dla etapów skrytek lab (UnifiedMap) - włącz „Okręgi” w szybkich ustawieniach mapy, aby je pokazać
- Nowość: Opcja ustawiania okręgów z indywidualnym promieniem do punktów orientacyjnych („Geofence” w menu kontekstowym)
- Naprawiono: Widok mapy nie zaktualizował się podczas usuwania skrytki z aktualnie wyświetlanej listy
- Fix: Number of cache in list chooser not updated on changing list contents
- Zmiana: Zachowaj bieżący widok na mapowaniu listy, jeśli wszystkie skrytki mieszczą się w bieżącym widoku
- Nowość: Śledź moją lokalizację na wykresie wysokości (UnifiedMap)
- Nowość: Włączenie działań „przenieś do” / „skopiuj do” dla „Pokaż jako listę”
- Nowość: Wsparcie dla motywu Elevate Winter przy pobieraniu map
- Nowość: Adaptacyjne cieniowanie wzgórz, opcjonalny tryb wysokiej jakości (UnifiedMap Mapsforge)
- Nowość: Przeprojektowane okno dialogowe szybkich ustawień tras/śladów
- Nowość: Przytrzymaj dłużej ikonę wyboru mapy, aby wybrać poprzedniego dostawcę kafelków (UnifiedMap)
- Nowość: Zezwalaj na ustawianie nazwy wyświetlanej mapy offline w pliku towarzyszącym (UnifiedMap)
- Nowość: Przytrzymaj dłużej przycisk „włącz online", aby załadować skrytki zapisane lokalnie

### Szczegóły skrytki
- Nowość: Tłumaczenie offline tekstu opisu i logów (eksperymentalne)
- Nowość: Opcja udostępniania skrytki z danymi użytkownika (współrzędne, notatka osobista)
- Naprawiono: Usługa mowy była przerywana po obrócaniu ekranu
- Naprawiono: Szczegóły skrytki: Listy skrytek nie aktualizowały się po dotknięciu na nazwę listy przy usuwaniu tej skrytki z listy
- Naprawiono: Notatka użytkownika była tracona podczas odświeżania skrytki lab
- Zmiana: Teksty zastępcze związane z datą logowania będą używały wybranej daty zamiast bieżącej daty
- Nowość: Domyślne zwijanie długich wpisów dziennika

### Wherigo player
- Nowość: Zintegrowane sprawdzanie danych logowania przez odtwarzacz Wherigo
- Zmiana: Usunięto raportowanie o błędzie Wherigo (ponieważ błędy są głównie związane z kartridżem, muszą być naprawione przez właściciela kartridża)
- New: Ability to navigate to a zone using compass
- New: Ability to copy zone center coordinates to clipboard
- New: Set zone center as target when opening map (to get routing and distance info for it)
- Nowość: Obsługa otwierania lokalnych plików Wherigo
- Change: Long-tap on a zone on map is no longer recognized. This allows users to do other stuff in map zone area available on long-tap, eg: create user-defined cache

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
- New: Added button to delete log templates
- Naprawiono: Importowanie lokalnego pliku mapy przydzielało losową nazwę mapy
