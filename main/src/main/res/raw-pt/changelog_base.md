New: Integrated Wherigo player (beta) - see menu entry on home screen.<br> (You may want to [configure a quick launch item](cgeo-setting://pref_quicklaunchitems) or [customize bottom navigation](cgeo-setting://pref_custombnitem) for easier access, need to enable extended settings first.)

### Mapa
- Novo: Armazenar tema do mapa por provedor de blocos (Mapa Unificado)
- Novo: Destacar cache/ponto adicional selecionado (Mapa Unificado)
- Novo: Adicionado um separador entre fontes de mapas offline e online
- Novo: Suporte para Mapsforge como alternativa à VTM no Mapa Unificado, consulte [Definições => Fontes do Mapa => Mapa Unificado](cgeo-setting://useMapsforgeInUnifiedMap)
- Change: 'Show elevation chart' moved to long tap menu (UnifiedMap)
- Change: Use new hillshading algorithm for Mapsforge offline maps
- New: Hillshading support for UnifiedMap Mapsforge offline maps
- New: Hillshading support for UnifiedMap VTM maps (requires online connection)
- Fix: Address search not considering live mode (UnifiedMap)
- Change: "follow my location" moved to the map, giving more space for "live mode" button

### Detalhes da cache
- Novo: Variáveis usadas no gerador de projeções, ainda não existentes, são criadas na lista de variáveis
- Novo: Permitir números grandes em fórmulas
- Novo: Suporte para mais constelações para variáveis em fórmulas
- Correcção: Múltiplas imagens numa nota pessoal não eram adicionadas ao separador de imagens
- Fix: Handling of projections in waypoints and personal notes
- New: Long tap on date in logging retrieves previous log date
- Fix: Resetting cache to original coordinates does not remove "changed coordinates" flag

### Geral
- Alteração: Usar elevação acima do nível médio do mar (se disponível, Android 14+ apenas)
- Novo: Permitir vários níveis de hierarquia nas listas de caches
- Novo: Ícones específicos para os tipos de eventos blockparty e HQ, de geocaching.com
- Novo: Defina o tamanho de imagem preferido para imagens de caches e TBs descarregadas de geocaching.com
- Fix: "Open in browser" not working for trackable logs
- New: Option to manage downloaded files (maps, themes, routing and hillshading data)
- New: Option to remove a cache from all lists (= mark it as to be deleted)
- Fix: Reset coordinates not detected by c:geo for unsaved caches
