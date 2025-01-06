Nowość: Zintegrowany odtwarzacz Wherigo (beta) – zobacz pozycję w menu na ekranie głównym.<br> (Możesz zechcieć [skonfigurować element szybkiego uruchamiania](cgeo-setting://quicklaunchitems_sorted) lub [dostosować dolną nawigację](cgeo-setting://custombnitem) dla łatwiejszego dostępu, co wymaga włączenia ustawień rozszerzonych).

### Mapa
- Nowość: Zapisuj motyw mapy osobno dla każdego dostawcy kafelków (UnifiedMap)
- Nowość: Podświetl wybraną skrytkę/punkt nawigacji (UnifiedMap)
- Nowość: Dodano separator między źródłami mapy offline i online
- Nowość: Wsparcie Mapsforge jako alternatywy dla VTM w UnifiedMap, zobacz [Ustawienia => Źródła mapy => Mapa ujednolicona](cgeo-setting://useMapsforgeInUnifiedMap)
- Zmiana: Opcja „Pokaż wykres wysokości” przeniesiona do menu po długim naciśnięciu (UnifiedMap)
- Zmiana: Użycie nowego algorytmu cieniowania dla map offline Mapsforge
- Nowość: Wsparcie cieniowania dla map offline Mapsforge
- Nowość: Wsparcie cieniowania dla map UnifiedMap VTM (wymaga połączenia online)
- Naprawiono: Wyszukiwanie adresu nie uwzględniało trybu na żywo (UnifiedMap)
- Zmiana: Funkcję „podążaj za moją lokalizacją” przeniesiono do mapy, dając więcej miejsca na przycisk „tryb na żywo”
- Zmiana: Długie przytrzymanie jest teraz bardziej w stylu c:geo
- Zmiana: Funkcje zarządzania danymi offline (pobieranie map, sprawdzanie brakujących danych prowadzenia i cieniowania wzniesień) przeniesione do menu wyboru mapy => „Zarządzaj danymi offline”
- Naprawiono: Mapa nie aktualizuje zmienionych skrzynek

### Szczegóły skrytki
- Nowość: Jeszcze nie istniejące zmienne używane w projekcji zostaną utworzone na liście zmiennych
- Nowość: Zezwalaj na duże liczby całkowite we wzorach
- Nowość: Wsparcie większej liczby konstelacji dla zmiennych we wzorach
- Naprawiono: Jeśli w notatce osobistej było wiele zdjęć to były dodawane do karty obrazów
- Naprawiono: Obsługa projekcji w punktach nawigacji i notatkach osobistych
- Nowość: Długie dotknięcie daty w logowaniu pobiera poprzednią datę dziennika
- Naprawiono: Resetowanie skrytki do oryginalnych współrzędne nie usuwało flagi „zmienione współrzędne”
- Nowość: Potwierdź nadpisanie dziennika w szybkim logu offline
- Nowość: Aktualizacja status skrytki przy wysyłaniu logu
- Nowość: Kolorowany widok źródła HTML szczegółów skrytki
- Naprawiono: checksum(0) zwraca nieprawidłową wartość
- Naprawiono: Edycja logów usuwa status „znajomych”

### Ogólne
- Zmiana: Użyj wysokości nad średnim poziomem morza (jeśli to możliwe, tylko Android 14+)
- Nowość: Zezwalaj na wiele poziomów hierarchii na listach skrytek
- Nowość: Dedykowane ikony dla typów wydarzeń blockparty geocaching.com i wydarzeń HQ
- Nowość: Ustaw preferowany rozmiar obrazu dla obrazów ze skrytek i przedmiotów podróżnych z geocaching.com
- Naprawiono: Opcja „Otwórz w przeglądarce” nie działała dla wpisów przedmiotów podróżnych
- Nowość: Opcja zarządzania pobranymi plikami (mapy, motywy, routing i dane cieniowania)
- Nowość: Opcja usuwania skrytki ze wszystkich list (= oznacz jako do usunięcia)
- Naprawiono: Resetowanie współrzędnych nie było wykrywane przez c:geo dla niezapisanych skrytek
- Nowość: Zezwalaj na czyszczenie filtra, jeśli żaden filtr nie jest zapisany
- Naprawiono: Podczas rozpoczęcia pobierania pocket query do nowo utworzonej listy wyskokiwało okienko z informacją „Pusta lista”
- Zmiana: Własne skrytki z logami offline pokazują znacznik logów offline
- Nowość: Konfigurowalny format daty (np. dzienniki skrytek), zobacz [Ustawienia => Wygląd => Format daty](cgeo-settings://short_date_format)
- Nowość: Dotknięcie nazwy serwisu geocachingowego na ekranie głównym prowadzi do odpowiednich ustawień konfiguracyjnych
- Nowość: Dodatkowe emotikony dla ikon skrytek
- Zmiana: Filtrowanie skrytek „Specjalne” obejmuje wydarzenia typu mega, giga, uroczystości społeczności, uroczystości sztabu, block party i labirynty
- Zmiana: Filtrowanie skrytek „Inne” zawiera GCHQ, APE i nieznane typy
- Naprawiono: Długość historii i ustawienia zbliżeniowe współdzieliły wartości suwaka
- Naprawiono: Strona z wpisami przedmiotów podróżnych pokazywała pola czas/współrzędne dla przedmiotów podróżnych, które tego nie obsługują
- Naprawiono: Kilka awarii
