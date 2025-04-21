### Zawiadomienie o wycofaniu mapy drogowej i „staych” map UnifiedMap
c:geo ma od pewnego czasu zupełnie nową implementację mapy o nazwie „UnifiedMap”, która ostatecznie zastąpi stare implementacje Google Maps i Mapsforge (OpenStreetMap). Jest to zawiadomienie o wycofaniu, aby poinformować Cię mapie drogowej.

UnifiedMap został opublikowany około roku temu. Nadal obsługuje Google Maps i OpenStreetMap (online + offline), ale w całkowicie przerobiony sposób techniczny. i z wieloma ekscytującymi nowymi funkcjami, których nie obsługują „stare” mapy, z których część to
- Obracanie mapy dla map opartych na OpenStreetMap (online i offline)
- Wyskakujące okienko klastra dla Google Maps
- Ukrywanie źródeł map, których nie potrzebujesz
- Wykres wysokości tras i ścieżek
- Przełącz pomiędzy listami bezpośrednio z mapy
- „Tryb jazdy” dla map opartych na OpenStreetMap

UnfiedMap okazała się stabilna od dłuższego czasu, dlatego usuniemy stare implementacje mapy, aby zmniejszyć wysiłek związany z utrzymaniem c:geo.

Plan działania:
- „Stare” mapy są teraz w trybie wycofywania – nie naprawimy już w nich błędów.
- UnifiedMap stanie się domyślne dla wszystkich użytkowników jesienią 2025 r.
- „Stare” implementacje mapy zostaną usunięte wiosną 2026 r.

Do tego czasu możesz przełączać się między różnymi implementacjami w ustawieniach => źródła mapy.

### Mapa
- Nowość: Pokaż granice udzialania odpowiedzi (geofence) dla etapów skrytek lab (UnifiedMap) - włącz „Okręgi” w szybkich ustawieniach mapy, aby je pokazać
- Nowość: Opcja ustawiania okręgów z indywidualnym promieniem do punktów orientacyjnych („Geofence” w menu kontekstowym)
- Naprawiono: Widok mapy nie zaktualizował się podczas usuwania skrytki z aktualnie wyświetlanej listy
- Naprawiono: Liczba skrytek w wyborze listy nie była aktualizowana po zmianie zawartości listy
- Zmiana: Zachowaj bieżący widok na mapowaniu listy, jeśli wszystkie skrytki mieszczą się w bieżącym widoku
- Nowość: Śledź moją lokalizację na wykresie wysokości (UnifiedMap)
- Nowość: Włączenie działań „przenieś do” / „skopiuj do” dla „Pokaż jako listę”
- Nowość: Wsparcie dla motywu Elevate Winter przy pobieraniu map
- Nowość: Adaptacyjne cieniowanie wzgórz, opcjonalny tryb wysokiej jakości (UnifiedMap Mapsforge)
- Nowość: Przeprojektowane okno dialogowe szybkich ustawień tras/śladów
- Nowość: Przytrzymaj dłużej ikonę wyboru mapy, aby wybrać poprzedniego dostawcę kafelków (UnifiedMap)
- Nowość: Zezwalaj na ustawianie nazwy wyświetlanej mapy offline w pliku towarzyszącym (UnifiedMap)
- Nowość: Przytrzymaj dłużej przycisk „włącz online", aby załadować skrytki zapisane lokalnie
- New: Offline hillshading for UnifiedMap (VTM variant)

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
