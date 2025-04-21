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
- Novo: Mostrar perímetro de resposta para os pontos de AdventureLab (Mapa Unificado) — active "círculos" nas configurações rápidas do mapa para mostrá-los
- Novo: Opção para definir círculos com raio individual para pontos adicionais (opção de menu "perímetro de resposta")
- Correcção: Visualização do mapa não era actualizada ao remover a cache da lista actualmente mostrada
- Fix: Number of cache in list chooser not updated on changing list contents
- Alteração: Manter a visualização actual no mapeamento de uma lista, se todas as caches couberem na visualização actual
- New: Follow my location in elevation chart (UnifiedMap)
- New: Enable "move to" / "copy to" actions for "show as list"
- Novo: Suporte para o tema Elevate Winter no gestor de transferências de mapas
- Novo: Sombreado adaptativo de altitude, modo de alta qualidade opcional (Mapa Unificado Mapsforge)
- Novo: Redesenhado menu de definições rápidas de percursos/rotas
- Novo: Toque longo no ícone de seleção de mapa para selecionar o provedor de blocos anterior (Mapa Unificado)
- Novo: Permitir a configuração do nome para mapas offline no ficheiro complementar (Mapa Unificado)
- New: Long tap on "enable live button" to load offline caches
- New: Offline hillshading for UnifiedMap (VTM variant)

### Detalhes da cache
- New: Offline translation of listing text and logs (experimental)
- Novo: Opção para partilhar geocache com os dados do utilizador (coordenadas, nota pessoal)
- Correcção: Serviço de fala interrompido na rotação do ecrã
- Correcção: Detalhes da cache: Listas para a cache não eram actualizadas depois de tocar no nome da lista e remover essa cache dessa lista
- Fix: User note gets lost on refreshing a lab adventure
- Change: Log-date related placeholders will use chosen date instead of current date
- New: Collapse long log entries per default

### Wherigo player
- New: Integrated Wherigo player checking for missing credentials
- Change: Removed Wherigo bug report (as errors are mostly cartridge-related, need to be fixed by cartridge owner)
- New: Ability to navigate to a zone using compass
- New: Ability to copy zone center coordinates to clipboard
- New: Set zone center as target when opening map (to get routing and distance info for it)
- Novo: Suporte para abrir ficheiros Wherigo locais
- Change: Long-tap on a zone on map is no longer recognized. This allows users to do other stuff in map zone area available on long-tap, eg: create user-defined cache
- New: Display warning if wherigo.com reports missing EULA (which leads to failing download of cartridge)

### Geral
- Novo: Página de pesquisa redesenhada
- Novo: Filtro de contagem de inventário
- Novo: Suporte para formato de coordenadas DD,DDDDDDD
- Novo: Mostrar o último nome de filtro usado na caixa de diálogo do filtro
- New: Coordinate calculator: Function to replace "x" with multiplication symbol
- Fix: Incorrect altitude (not using mean above sea level)
- Correcção: Configuração de limite de distância próxima não funcionava correctamente para valores pequenos
- Fix: Sorting of cache lists by distance descending not working correctly
- Fix: Lab caches excluded by D/T filter even with active "include uncertain"
- Correcção: problemas de cor com os ícones do menu no modo claro
- New: Add "Remove past events" to list "all"
- New: Show connector for "user-defined caches" as active in source filter
- New: GPX export: exporting logs / trackables made optional
- New: Added button to delete log templates
- Correcção: Ao importar o ficheiro de mapa local é atribuído um nome aleatório ao mapa
