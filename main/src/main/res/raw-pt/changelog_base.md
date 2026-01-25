Edge to Edge: Due to Play Store policies we have updated the Android API level this version of c:geo targets + we have changed some of the screen layout routines. Isto pode trazer alguns efeitos colaterais indesejados, especialmente nas versões mais recentes do Android. Se tiver algum problema com esta versão do c:geo, por favor informe em [GitHub](https://github.com/cgeo/cgeo) ou por e-mail para [support@cgeo.org](mailto:support@cgeo.org)

Legacy Maps: As announced with 2025.07.17 and 2025.12.01 releases, we have finally removed the legacy implementations for our maps. You will be switched to our new UnifiedMap automatically and should notice no differences except a couple of new features, some of which are
- Rotação do mapa para mapas baseados em OpenStreetMap (online e offline)
- Janela pendente de agrupamento para o Google Maps
- Ocultar fontes de mapa de que não precisa
- Gráfico de altitude para rotas e percursos
- Alternar entre listas directamente do mapa
- "Modo de condução" para mapas baseados no OpenStreetMap
- Long-tap on track / individual route for further options

### Mapa
- Novo: Optimização de rota armazena dados calculados
- Novo: Os pontos adicionais do destino definido permanecem visíveis se o modo em tempo real for ligado
- Novo: Toque longo na linha de navegação abre o gráfico de elevação (Mapa Unificado)
- Novo: Mostrar os pontos adicionais gerados no mapa
- Novo: Transferência de caches ordenadas por distância
- Correcção: duplicação de pontos individuais da rota
- Novo: Suporte para o tema Motorider (apenas VTM)
- New: NoMap tile provider (don't show map, just caches etc.)
- Change: Max distance to connect points on history track lowered to 500m (configurable)

### Detalhes da cache
- Novo: Detecção de caracteres adicionais nas fórmulas: –, ⋅, ×
- Novo: Mantém data/hora dos próprios registos ao actualizar uma cache
- Novo: Visualização opcional de mini bússola (ver configurações => detalhes da cache => Mostrar direcção na visualização de detalhes da cache)
- Novo: Mostrar registos do proprietário da cache no separador "amigos/meus"
- Alteração: O separador "Amigos/Meus" mostra a contagem de registos para aquele separador em vez da contagem total de registos
- Alteração: Melhorado o cabeçalho nos separadores de variáveis e pontos adicionais
- Correcção: O item "eliminar registo" aparecia repetidamente
- Correcção: O c:geo fechava inesperadamente nos detalhes da cache quando o ecrã era rodado
- Alteração: Interface mais compacta para "adicionar novo ponto adicional"
- Novo: Opção para carregar imagens para caches do geocaching.com em tamanho “inalterado”
- Novo: Visualização de variáveis pode ser filtrada
- Novo: Visualizar coordenadas calculadas que excedem os limites na lista de pontos adicionais
- Novo: Opção de marcar alguns tipos de pontos adicionais como visitados na lista de pontos adicionais
- New: Placeholders for trackable logging (geocache name, geocache code, user)
- Change: Removed the link to outdated WhereYouGo player. Integrated Wherigo player is now default for Wherigos.
- Fix: Missing quick toggle in guided mode of waypoint calculator

### Wherigo
- Novo: Tradução offline para Wherigos
- Novo: Melhorado o funcionamento dos botões
- New: Status auto-save
- New: Option to create shortcout to Wherigo player on your mobile's home screen

### Geral
- Novo: Opção de partilhar depois de registar uma cache
- Alteração: Não mostrar as opções "precisa de manutenção" ou "precisa de arquivamento" para as próprias caches
- Correcção: Restaurar uma cópia de segurança pode duplicar ficheiros de percursos no armazenamento interno e subsequentes cópias de segurança
- Alteração: Removidas as referências ao Twitter
- Novo: Apagar ficheiros de percurso órfãos na limpeza e restauro de cópia de segurança
- Novo: Aviso ao tentar adicionar muitas caches para uma lista de marcadores
- Novo: Funções observar/não observar numa lista
- Novo: Oferecer tradução sem rede com as aplicações do Google Tradutor ou do DeepL (se instalado)
- Novo: Excluir itens do histórico de pesquisa
- Alteração: Remover GCVote (serviço descontinuado)
- Novo: Barra de ferramentas colorida nas páginas de detalhes das caches
- Novo: Selecção de múltiplas listas de favoritos / pocket queries para transferência
- Novo: Pré-visualizar listas de marcadores
- Alteração: Elevada a versão mínima necessária do Android para o Android 8
- New: Default quick buttons for new installations
- Fix: Titles in range input dialogs cut off
- Fix: Notification for nightly update points to regular APK even for FOSS variant
- New: "Ignore year" option for date filters
- New: Make remote URI clickable in pending downloads
- Change: Use system-settings as default theme for new installations
- New: GPX export: Write GSAK Lat/LonBeforeCorrect annotations when exporting original waypoints
- New: Show undo bar when deleting caches from list from map
