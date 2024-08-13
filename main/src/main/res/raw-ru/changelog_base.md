### Карта
- Новое: "Редактировать личную заметку" в информации о тайнике
- Исправлено: путевые точки не фильтровались при отображении одного тайника (ЕдинаяКарта)
- Новое: Поддержка заданных пользователем сторонних карт
- Исправлено: Обновление данных карты после открытия/закрытия окна настроек (ЕдинаяКарта)
- New: Toggle display of buildings 2D/3D (UnifiedMap OSM maps)
- New: Cache store/refresh from popup moved into background
- Change: Search for coordinates: Show direction and distance to target and not to current position
- New: Graphical D/T indicator in cache info sheet
- Fix: Compass rose hidden when filterbar is visible (UnifiedMap)

### Детали тайника
- Новое: Отображение графических файлов на вкладке "Изображения", на которые даны ссылки в разделе "Личные заметки"
- Изменение: Упрощено действие при длительном нажатии в разделах детали и трекаблы тайников
- Новое: Более плавное масштабирование изображений при добавлении их в записи о взятии тайника
- Изменение: изменена иконка «редактировать списки» с карандаша на список + карандаш
- Fix: vanity function failing on long strings
- Fix: Wrong parsing priority in formula backup
- Change: Allow larger integer ranges in formulas (disallow usage of negation)
- New: Allow user-stored cache images on creating/editing log
- Fix: Spoiler images no longer being loaded (website change)

### Общее
- Новое: Переключение на настройку статуса нахождения тайника в Lab Adventures вручную или автоматически
- Новое: Диалог выбора списка: Списки тайников с автоматической группировкой, в названии которых есть ":"
- Изменение: Используйте OSM Nominatum в качестве резервного геокодера, заменив геокодер MapQuest (который у нас больше не работает)
- Change: Updated integrated BRouter to v1.7.5
- Новое: Чтение информации о высотах из трека при импорте
- New: API to Locus now supporting cache size "virtual"
- Fix: Search results for a location no longer sorted by distance to target location
- New: "Corrected coordinates" filter
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
