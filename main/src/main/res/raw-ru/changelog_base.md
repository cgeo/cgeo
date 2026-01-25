Edge to Edge: Due to Play Store policies we have updated the Android API level this version of c:geo targets + we have changed some of the screen layout routines. Это может привести к некоторым нежелательным побочным эффектам, особенно в новых версиях Android. Если у вас возникнут какие-либо проблемы с этой версией c:geo, пожалуйста, сообщите об этом на [GitHub](https://github.com/cgeo/cgeo) или по электронной почте [support@cgeo.org](mailto:support@cgeo.org)

Legacy Maps: As announced with 2025.07.17 and 2025.12.01 releases, we have finally removed the legacy implementations for our maps. You will be switched to our new UnifiedMap automatically and should notice no differences except a couple of new features, some of which are
- Поворот карты для карт на основе OpenStreetMap (онлайн + офлайн)
- Всплывающее окно кластера для Google Maps
- Возможность убрать ненужные вам источники карт
- Диаграмма высот для маршрутов и треков
- Переключение между списками непосредственно с карты
- "Режим вождения" для карт на основе OpenStreetMap
- Long-tap on track / individual route for further options

### Карта
- Новое: При оптимизации маршрута кэшируются данные расчетов
- Новое: При включении режима реального времени путевые точки текущей установленной цели остаются видимыми
- Новое: Длительное нажатие на навигационную линию открывает карту высот (ЕдинаяКарта)
- Новое: Показать сгенерированные точки на карте
- Новое: Загрузка тайников упорядочена по расстоянию
- Исправлено: Дублирование отдельных пунктов маршрута
- Новое: Поддержка темы Motorider (только VTM)
- New: NoMap tile provider (don't show map, just caches etc.)
- Change: Max distance to connect points on history track lowered to 500m (configurable)

### Детали тайника
- Новое: Обнаружение дополнительных символов в формулах: –, ⋅, ×
- Новое: Сохранение временной метки для собственных записей при обновлении тайника
- Новое: Дополнительный мини-вид компаса (смотрите Настройки => Детали тайника => Показать направление в описании тайника)
- Новое: Показывать записи владельцев на вкладке "Свои записи и записи друзей"
- Изменение: На вкладке "Свои записи и записи друзей" отображалось количество записей для этой вкладки вместо глобальных счетчиков
- Изменение: Улучшен заголовок на вкладках переменных и путевых точек
- Исправлено: Отображалось два элемента «удалить запись»
- Исправлено: Сбой c:geo во время просмотра деталей тайника при повороте экрана
- Изменение: Более компактное расположение для "добавления новой путевой точки"
- Новое: Возможность загружать изображения для тайников geocaching.com в "неизмененном" размере
- Новое: Просмотр переменных может быть отфильтрован
- Новое: Визуализация переполнения вычисленных координат в списке путевых точек
- Новое: Пункт меню в списке путевых точек, позволяющий отмечать определенные типы путевых точек как посещенные
- New: Placeholders for trackable logging (geocache name, geocache code, user)
- Change: Removed the link to outdated WhereYouGo player. Integrated Wherigo player is now default for Wherigos.
- Fix: Missing quick toggle in guided mode of waypoint calculator

### Wherigo player
- Новое: Оффлайн перевод для Wherigo
- Новое: Улучшено управление кнопками
- New: Status auto-save
- New: Option to create shortcout to Wherigo player on your mobile's home screen

### Общее
- Новое: возможность поделиться информацией после записи о посещении тайника
- Изменение: Не отображать параметры "нуждается в востановлении" или "нуждается в архивировании" для собственных тайников
- Исправление: Восстановление резервной копии могло приводить к дублированию файлов трека во внутреннем хранилище и последующих резервных копиях
- Изменение: Удалены ссылки на Twitter
- Новое: Удаление треков (при очистке и при восстановлении резервной копии), которые не отслеживаются программой, но остаются в файловой системе и занимают место
- New: Added mappings for some missing OC cache types
- Новое: Функции добавления/исключения тайников в списке наблюдения
- Новое: Предложение автономного перевода с помощью приложений Google Переводчик или DeepL (если они установлены)
- Новое: Удаление элементов из истории поиска
- Изменение: Удален GCVote (сервис закрыт)
- Новое: Раскрашенная панель инструментов на страницах с деталями тайников
- Новое: Выбор нескольких списков закладок/запросов для загрузки
- Новое: Предварительный просмотр списков закладок
- Изменение: C:geo теперь работает только на устройствах Android 8 и выше
- New: Default quick buttons for new installations
- Fix: Titles in range input dialogs cut off
- Fix: Notification for nightly update points to regular APK even for FOSS variant
- New: "Ignore year" option for date filters
- New: Make remote URI clickable in pending downloads
- Change: Use system-settings as default theme for new installations
- New: GPX export: Write GSAK Lat/LonBeforeCorrect annotations when exporting original waypoints
- New: Show undo bar when deleting caches from list from map
