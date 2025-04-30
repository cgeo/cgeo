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

### Карта
- Новое: Отображение геозон этапов Adventure Lab (ЕдинаяКарта) - включите "Круги" в быстрых настройках карты, чтобы отобразить их
- Новое: Возможность устанавливать круги с индивидуальным радиусом для путевых точек (пункт контекстного меню "геозона")
- Исправлено: карта не обновлялась при удалении тайника из отображаемого в данный момент списка
- Fix: Number of cache in list chooser not updated on changing list contents
- Изменение: Сохраняется текущее окно просмотра при отображении списка, если все тайники помещаются в нём
- Новое: Следить за моим местоположением на карте высот (ЕдинаяКарта)
- Новое: Добавлены "переместить в" / "скопировать в" для "показать списком"
- Новое: Поддержка высот в Зимней теме (Winter theme) при скачивании карт
- Новое: Адаптивное затенение холмов, дополнительный режим высокого качества (Mapsforge в ЕдинаяКарта)
- Новое: Переработанный диалог быстрых настроек маршрутов/треков
- Новое: Длительное нажатие на значок выбора карты для выбора предыдущего провайдера карт (ЕдинаяКарта)
- Новое: Возможность настраивать отображаемое название для автономных карт в сопутствующем файле (ЕдинаяКарта)
- Новое: Долгий тап по "включить онлайн", чтобы скачать тайники
- New: Offline hillshading for UnifiedMap (VTM variant)
- New: Support for background maps (UnifiedMap)

### Детали тайника
- Новое: Офлайн-перевод текстов списков и записей (экспериментальная опция)
- Новое: Возможность поделиться пользовательскими данными (координаты, личные заметки) вместе с тайником
- Исправлено: Чтение текста прерывалось при повороте экрана
- Исправлено: Сведения о тайнике: списки тайников не обновлялись после нажатия на имя списка и удаления тайника из этого списка
- Исправлено: Заметка пользователя терялась при обновлении lab adventure (geocaching.com)
- Изменение: Подстановки, связанные с датой записи, будут использовать выбранную дату вместо текущей
- New: Collapse long log entries per default

### Wherigo player
- New: Integrated Wherigo player checking for missing credentials
- Change: Removed Wherigo bug report (as errors are mostly cartridge-related, need to be fixed by cartridge owner)
- New: Ability to navigate to a zone using compass
- New: Ability to copy zone center coordinates to clipboard
- New: Set zone center as target when opening map (to get routing and distance info for it)
- Новое: Поддержка открытия локальных файлов Wherigo
- Change: Long-tap on a zone on map is no longer recognized. This allows users to do other stuff in map zone area available on long-tap, eg: create user-defined cache
- New: Display warning if wherigo.com reports missing EULA (which leads to failing download of cartridge)

### Общее
- Новое: Переработана страница поиска
- Новое: Фильтр количества трекаблов на руках
- Новое: Поддержка координат в формате DD,DDDDDDD
- Новое: Отображение последнего использованного имени фильтра в диалоговом окне фильтра
- Новинка: Калькулятор координат: функция для замены "x" символом умножения
- Fix: Incorrect altitude (not using mean above sea level)
- Исправлено: Настройка ограничения расстояния до ближайшего объекта не работала должным образом при малых значениях
- Fix: Sorting of cache lists by distance descending not working correctly
- Исправлено: Тайники Adv Lab (Лаборатории Приключений) исключались фильтром D/T даже при активном "включать неопределенные"
- Исправлено: проблемы с цветом значков меню в светлом режиме
- New: Add "Remove past events" to list "all"
- New: Show connector for "user-defined caches" as active in source filter
- New: GPX export: exporting logs / trackables made optional
- New: Added button to delete log templates
- Исправлено: при импорте файла локальной карты имя карты выбиралось случайным образом
