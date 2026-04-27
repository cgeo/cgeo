##
Time to update! If you are still using Android 7 or older, this might be the last c:geo update for you! With our next feature release of c:geo we will drop support for Android 5-7 to reduce our maintenance load and to be able to update some external components used by c:geo which we are currently still holding back. We will still be supporting Android 8 up to Android 16 then (and newer versions when they will be published), a span of more than eight years of Android history.

 - Zmiana: Pliki Wherigo nie mogą być obecnie pobierane, zamiast tego wyświetlana jest dokładna instrukcja
 - Naprawiono: Powód usunięcia dziennika nie wymusza ograniczenia długości
 - Nowość: Rozszerzone logowanie błędów dla awarii w menedżerze pobierania
 - Naprawiono: Informacje o punkcie nawigacji czasami były zbyt długie, a przyciski nieosiągalne
 - Fix: Some location info gets truncated
 - Fix: Internal routing no longer working, only straight line shown

Note: If you are using internal routing, you will need to execute the following step once after installing this release: Go to c:geo home screen, open "Manage offline data" - "Update routing data", and let c:geo install the updated files. (Reason: BRouter routing data file structure has changed and all routing data files must comply to the same version.)

##
- Naprawiono: Parsowanie lokalizacji skrytki nie zawsze się udawało dla niektórych języków strony internetowej
- Naprawiono: Otwieranie przedmiotów podróżnych z listy obserwowanych nie udawało się
- Naprawiono: Klawiatura może blokować wybór listy
- Naprawiono: Dostawca kafelków zdefiniowany przez użytkownika nie obsługuje dodatkowych parametrów URL
- Naprawiono: Inwentarz / Przedmioty podróżne ze skrytki nie są wczytywane
- Zmiana: Zaktualizowano wpis user-agent w celu rozwiązania problemów z pobieraniem
- Naprawiono: Przeglądanie szczegółów przedmiotów podróżnych usuwa je z inwentarza skrytki

##
- Naprawiono: Okno dialogowe pobierania tłumaczenia offline wyświetlane w instalacjach bez obsługi tłumaczeń offline
- Naprawiono: Format współrzędnych zmieniający się w arkuszu informacji o skrytce/punkcie nawigacji
- Naprawiono: Data logowania była przycinana na liście logowań (w zależności od formatu daty i rozmiaru czcionki)
- Naprawiono: W pewnych sytuacjach nie był wykrywany czas wydarzenia
- Naprawiono: W pewnych sytuacjach linki w opisie skrytki były nieklikalne
- Naprawiono: Działania związane z logowaniem przedmiotów podróżnych czasami się mieszały

##
- Zmiana: Maksymalna liczba odwiedzin przedmiotów podróżnych GC na dziennik skrytki zmniejszona do 100 (wg żądania z geocaching.com, aby zmniejszyć obciążenie serwera spowodowane przez ekstremalnych miłośników „podróżników”)
- Naprawiono: Niektóre możliwe wyjątki dotyczące bezpieczeństwa, gdy użytkownik nie przyznał pewnych praw (np. powiadomienia)
- Naprawiono: Niekompletne okręgi wokół skrytek przy niskim poziomie powiększenia (tylko VTM)
- Naprawiono: Awaria przy przeładowaniu punktów nawigacji w niektórych sytuacjach
- Naprawiono: W niektórych sytuacjach filtr daty wydarzenia nie działał
- Naprawiono: Maksymalny limit logów nie działa niezawodnie przy ustawieniu „nieograniczona”
- Naprawiono: W określonych warunkach awaria przy otwarciu mapy
- Naprawiono: Mapa się nie wyświetała jeśli wherigo nie miało widocznych stref
- Naprawiono: W pewnych sytuacjach występował błąd na karcie obrazów w szczegółach skrytki
- Naprawiono: Wyszukiwanie na mapie z nieprawidłowymi współrzędnymi
- Naprawiono: Niektóre tłumaczenia nie są zgodne z wewnętrznymi ustawieniami języka w c:geo

##
- Zmiana: UnifiedMap ustawiona jako domyślna mapa dla każdego (jako część naszego harmonogramu dla UnifiedMap). Tymczasowo możesz to zmienić przez „Ustawienia” – „Źródła mapy”. W naszych regularnych wydaniach planowane jest usunięcie dotychczasowych map na wiosnę 2026 r.
- Naprawiono: Pole wyboru Ulubione było resetowane przy ponowym wejściu na ekran logowania offline
- Naprawiono: Pole do wpisywania promienia geofence pokazuje liczbę dziesiętną
- Naprawiono: Synchronizacja notatek osobistych nie działała
- Zmiana: Nowa ikona dla importu śladu GPX/trasy w szybkich ustawieniach śladu/trasy na mapie

##
- Naprawiono: Wartości ujemne na wykresie wysokości nie są skalowane
- Naprawiono: Współrzędne bliskie 0 uszkadzane przy eksporcie GPX
- Naprawiono: Kilka awarii
- Próba naprawienia: Aplikacja nie odpowiada podczas uruchamiania
- Próba naprawienia: Brak danych skrytek na mapie na żywo

##
- Naprawiono: Awaria podczas wyszukiwania słów kluczowych
- Naprawiono: Awaria na mapie
- Naprawiono: Tekst wskazówki nie jest już możliwy do zaznaczenia
- Naprawiono: Kilka problemów z Wherigo

##
- Naprawiono: Szyfrowanie/odszyfrowywanie podpowiedzi wymaga dodatkowego dotknięcia
- Naprawiono: Błąd Wherigo podczas czytania starych zapisanych gier
- Naprawiono: Logowanie wywołane w c:geo czasami nie było zapamiętane
- Naprawiono: Brak aktualizacji danych na żywo dla znalezionych i zarchiwizowanych skrytek
- Naprawiono: Punkty nawigacji na mapie offline nie są czasami wyświetlane

##
- Naprawiono: Niezaszyfrowane wskazówki skrytki (zmiana strony internetowej)
- Naprawiono: Skrytki Lab nie były wczytywane w aplikacji (zmiana strony internetowej, będziesz musiał zaktualizować zapisane Laby, aby móc wywoływać je z c:geo ponownie)
- Naprawiono: UnifiedMap VTM: Przełączanie 3D budynków nie działa dla połączonych map
- Naprawiono: Tłumaczenie offline: Czasami język opisu skrytki był wykrywany jako --

##
- Naprawiono: Awaria w module tłumaczenia
- Naprawiono: Błąd przy wykrywaniu logowania (zmiana strony)
- Naprawiono: Awaria przy pobieraniu wkładu Wherigo
- Naprawiono: „Wczytaj więcej” nie przestrzega filtrów offline

##
- Naprawiono: Inwentarz przedmiotów podróżnych nie był ładowany podczas logowania skrytki

##
- Naprawiono: Migracja skrzynek zdefiniowanych przez użytkownika podczas uruchamiania c:geo nie powiodła się => tymczasowo usunięto
- Naprawiono: Zakończone zadania Wherigo nie zostały oznaczone jako zakończone lub nieudane































