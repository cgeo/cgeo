### Zunifikowana mapa
Witamy w naszej nowej implementacji mapy, wewnętrznie o nazwie „Zunifikowana mapa”. Jest to wynik prawie dwuletniej pracy zespołu c:geo nad całkowicie nową implementacją map. Powodem, dla którego zaczęliśmy to robić, było to, że nasze stare implementacje map stawały się coraz trudniejsze w utrzymaniu i zapewnieniu (mniej lub bardziej) synchronizacji pod względem funkcji, a niektóre części kodu miały dziesięć lat (lub więcej).

W Zunifikowanej mapie staraliśmy się uzyskać takie same wrażenia użytkownika na wszystkich różnych typach map (tam, gdzie to możliwe), jednocześnie modernizując i ujednolicając wewnętrzną architekturę.

Zunifikowana mapa oferuje (prawie) wszystkie funkcje naszych starych implementacji mapy, ale oferuje kilka dodatkowych funkcji:

- Obracanie mapy dla map opartych na OpenStreetMap (online i offline)
- Ułamkowe skalowanie map opartych na OpenStreetMap
- Wyskakujące okienko klastra dla Google Maps
- Ukrywanie źródeł map, których nie potrzebujesz
- Wykres ukształtowania dróg i ścieżek (dotknij trasę)

Zunifikowana mapa osiągnęła już stan beta, dlatego zdecydowaliśmy się uczynić ją domyślną mapą dla wszystkich użytkowników wersji Nightly.

Wszystko powinno działać, ale nadal mogą (i będą) występować pewne błędy. W razie potrzeby możesz przełączać się między starą i nową implementacją mapy (patrz ustawienia - źródła map), ale naprawdę chcielibyśmy, abyś wypróbował nową. Prosimy o zgłaszanie wszelkich błędów, które znalazłeś do pomocy technicznej ([support@cgeo.org](mailto:support@cgeo.org)) lub [c:geo na GitHub](github.com/cgeo/cgeo/issues). Każda opinia jest mile widziana!

---

Więcej zmian:

### Mapa
- Nowość: Podświetl istniejące pliki w menedżerze pobierania
- Nowość: Pokaż stan znalezień skrytek na ikonach punktu nawigacji

### Szczegóły skrytki
- Zmiana: Ustawienie „Przełącz mówienie” jako rzeczywisty przełącznik

### Ogólne
- Nowość: Dotknięcie powiadomienia o pobieraniu otwiera widok „oczekujące pobierania”
- Zmiana: Używanie tapety jako tła nie wymaga już uprawnień READ_EXTERNAL_STORAGE
