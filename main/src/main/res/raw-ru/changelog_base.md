### Карта
- Новое: источник OSM карт osm.paws.cz
- Новое: Включено чтение flopp.net GPX-файлов как треков
- Исправлено: Отсутствовал символ маршрутизации после 'добавить в маршрут'
- Исправлено: Отсутствовал расчет маршрута для добавленных точек
- Новое: Добавлена поддержка темы 'Voluntary MF5' OpenAndroMaps
- New: Add support for GeoJSON data
- Change: Use last known map position as fallback (when no GPS available and "follow my location" inactive)

### Детали тайника
- Новое: Новая более проработанная галерея изображений
- Исправлено: Восстановление позиции в списке путевых точек после обновления или удаления путевой точки
- Исправлено: Перемещение вниз при создании новой путевой точки
- Новое: Распознавание переменных, введенных в заметки пользователя
- Новое: Отображение кнопки Лаборатории Приключений в логических тайниках, если необходимо
- Исправлено: Удаление описания точки, не синхронизировалось для заметок на сервере
- Fix: Waypoint list not updated after scan

### Общее
- Изменение уровня API (compileSDK 32)
- Обновление некоторых зависимых библиотек
- Изменение: Использован другой механизм Android для получения загрузок (для лучшей совместимости с Android 12+)
- Новое: Имя списка пресетов с именем файла GPX при импорте
- New: Allow import of GPX track files that do not provide a xmlns namespace tag
- New: Add monochrome launcher icon for Android 13
- New: Display geocaching.com member status on home screen
- Change: GPX-Import: Use name as geocode for 'unknown' connector
- Fix: Allow filtering for archived caches in owner search
- Fix: Line breaks sometimes missing in logbook view directly after posting a log
- Fix: Preview button displayed only with filter setting "show all" in PQ list
