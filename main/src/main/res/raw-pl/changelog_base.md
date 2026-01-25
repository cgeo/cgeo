Edge to Edge (od krawędzi do krawędzui): Ze względu na zasady Sklepu Play, zaktualizowaliśmy poziom API Androida w tej wersji celów c:geo + zmieniliśmy niektóre z zasad układu ekranu. Mogą pojawić się pewne niepożądane efekty uboczne, zwłaszcza na nowszych wersjach Androida. Jeśli napotkasz jakiekolwiek problemy z tą wersją c:geo, prosimy o zgłoszenie na [GitHub](https://github.com/cgeo/cgeo) lub poprzez e-mail [support@cgeo.org](mailto:support@cgeo.org)

Starsze mapy (Legacy): Jak zapowiedziano w wydaniach 2025.07.17 i 2025.12.01, usunęliśmy wreszcie stare wersje naszych map. Zostaniesz automatycznie przełączony na naszą nową mapę UnifiedMap i nie powinieneś zauważyć większych różnic poza kilkoma nowymi funkcjami, z których są
- Obracanie mapy dla map opartych na OpenStreetMap (online i offline)
- Wyskakujące okienko klastra dla Google Maps
- Ukrywanie źródeł map, których nie potrzebujesz
- Wykres wysokości tras i ścieżek
- Przełączanie pomiędzy listami bezpośrednio z mapy
- „Tryb jazdy” dla map opartych na OpenStreetMap
- Długie dotknięcie na śladzie / indywidualnej trasa daje dostęp do dalszych opcji

### Mapa
- Nowość: Optymalizacja trasy buforuje obliczone dane
- Nowość: Włączenie trybu na żywo utrzymuje punkty orientacyjne aktualnie ustawionego celu
- Nowość: Długie dotknięcie na linii nawigacji otwiera wykres wysokości (UnifiedMap)
- Nowość: Pokaż wygenerowane punkty nawigacji na mapie
- Nowość: Pobierz skrytki uporządkowane według odległości
- Naprawiono: Dublowanie poszczególnych pozycji trasy
- Nowość: Wsparcie dla motywu Motorider (Tylko VTM)
- Nowość: Dostawca NoMap (nie pokazuj mapy, po prostu skrytki itp.)
- Zmiana: Maksymalna odległość do połączenia punktów na historii śladu obniżona do 500 metrów (konfigurowalne)

### Szczegóły skrytki
- Nowość: Wykrywaj dodatkowe znaki w formułach: –, ⋅, ×
- Nowość: Zachowaj znacznik czasu z własnych logów przy odświeżeniu skrytki
- Nowość: Opcjonalny widok kompasu (zobacz ustawienia => szczegóły skrytki => Pokaż kierunek w widoku szczegółów skrytki)
- Nowość: Pokaż wpisy właścicieli na karcie „wpisy przyjaciół/własne”
- Zmiana: Karta „Wpisy przyjaciół/własne” pokazuje liczbę wpisów dla tej karty, a nie globalne liczniki
- Zmiana: Ulepszony nagłówek w zakładkach „Zmienne” i „Punkty nawigacji”
- Naprawiono: Podwójne wyświetlanie elementu „Usuń wpis”
- Naprawiono: Awaria c:geo podczas obracania ekranu w szczegółach skrytki
- Zmiana: Bardziej kompaktowy układ dla „dodawania nowego punktu nawigacji”
- Nowość: Możliwość załadowania obrazów dla skrytek geocaching.com o rozmiarze „niezmienione”
- Nowość: Widok zmiennych może być filtrowany
- Nowość: Wizualizuj przepełnienie obliczonych współrzędnych na liście punktów nawigacji
- Nowość: Wpis menu na liście punktów nawigacji do oznaczania niektórych typów punktów nawigacji jako odwiedzone
- Nowość: Teksty zastępcze przy logowaniu przedmiotów podróżnych (nazwa skrytki, kod skrytki, użytkownik)
- Zmiana: Usunięto link do przestarzałego odtwarzacza WhereYouGo. Zintegrowany odtwarzacz Wherigo jest teraz domyślny dla skrytek Wherigo.
- Naprawiono: Brak szybkiego przełącznika w kalkulatorze punktów nawigacyjnych

### Odtwarzacz Wherigo
- Nowość: Tłumaczenie offline dla Wherigo
- Nowość: Ulepszona obsługa przycisku
- Nowość: Automatyczne zapisywanie statusu
- Nowość: Opcja tworzenia skrótu do odtwarzacza Wherigo na ekranie głównym Twojego telefonu

### Ogólne
- Nowość: Opcja udostępniania po dokonaniu wpisu w dzienniku skrytki
- Zmiana: Nie pokazuj opcji „wymaga konserwacji” lub „wymaga zarchiwizowania” dla własnych skrytek
- Naprawiono: Przywracanie kopii zapasowej może duplikować pliki śladów w pamięci wewnętrznej i kolejnych kopiach zapasowych
- Zmiana: Usunięte odwołania do Twittera
- Nowość: Usuń osierocone pliki ścieżek przy czyszczeniu i przywracaniu kopii zapasowej
- Nowość: Ostrzeżenie przy próbie dodania zbyt wielu skrytek do zakładek
- Nowość: Funkcje listy obserwowanej/nieobserwowanej
- Nowość: Oferuj tłumaczenie offline z aplikacjami Google Translate lub DeepL (jeśli zainstalowane)
- Nowość: Usuń elementy z historii wyszukiwania
- Zmiana: Usunięto GCVote (usługa przestała działać)
- Nowość: Kolorowany pasek narzędzi na stronach szczegółów skrytki
- Nowość: Wybierz wiele zakładek / pocket queries do pobrania
- Nowość: Podgląd list zakładek
- Zmiana: Zwiększenie minimalnej wymaganej wersję Androida do Androida 8
- Nowość: Domyślne szybkie przyciski dla nowych instalacji
- Naprawiono: Tytuły w oknie dialogowym zakresu były przycięte
- Naprawiono: Powiadomienie o nocnych aktualizacjach punktów do zwykłego APK nawet dla wariantu FOSS
- Nowość: Opcja „Ignoruj rok” dla filtrów dat
- Nowość: Zdalne URI w elementach oczekujących na pobranie są teraz klikalne
- Zmiana: Użyj ustawień systemowych jako domyślnego motywu dla nowych instalacji
- New: GPX export: Write GSAK Lat/LonBeforeCorrect annotations when exporting original waypoints
- New: Show undo bar when deleting caches from list from map
