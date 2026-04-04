De ponta a ponta: Devido às políticas da Play Store, actualizámos o nível API do Android nesta versão dos alvos do c:geo e alterámos algumas das rotinas de esquema do ecrã. Isto pode trazer alguns efeitos colaterais indesejados, especialmente nas versões mais recentes do Android. Se tiver algum problema com esta versão do c:geo, por favor informe em [GitHub](https://github.com/cgeo/cgeo) ou por e-mail para [support@cgeo.org](mailto:support@cgeo.org)

Mapas antigos: Como anunciado nos lançamentos de 2025-07-17 e 2025-12-01, finalmente removemos as implementações antigas para os nossos mapas. O seu mapa será alterado automaticamente para o nosso novo Mapa Unificado e não deverá notar diferenças, excepto algumas das novas funcionalidades, algumas das quais são
- Rotação do mapa para mapas baseados em OpenStreetMap (online e offline)
- Janela pendente de agrupamento para o Google Maps
- Ocultar fontes de mapa de que não precisa
- Gráfico de altitude para rotas e percursos
- Alternar entre listas directamente do mapa
- "Modo de condução" para mapas baseados no OpenStreetMap
- Toque longo no percurso / rota individual para mais opções

### Mapa
- Novo: Optimização de rota armazena dados calculados
- Novo: Os pontos adicionais do destino definido permanecem visíveis se o modo em tempo real for ligado
- Novo: Toque longo na linha de navegação abre o gráfico de elevação (Mapa Unificado)
- Novo: Mostrar os pontos adicionais gerados no mapa
- Novo: Transferência de caches ordenadas por distância
- Correcção: duplicação de pontos individuais da rota
- Novo: Suporte para o tema Motorider (apenas VTM)
- Novo: Tipo de mapas: Sem Mapa (não mostra mapa, apenas caches etc.)
- Alteração: Distância máxima para conectar pontos no histórico do percurso reduzido para 500m (configurável)
- Novo: Permitir a importação de ficheiros KML como percursos (por exemplo: itinerário de TB)
- Novo: Possibilidade para definir o ícone da cache mesmo que ela ainda não esteja armazenada
- Novo: Caixa de informação para o gráfico de elevação que mostra a distância restante, a subida e a descida
- New: Display coordinates of waypoints in waypoint popup

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
- Novo: Espaços para registo de TB (nome da geocache, código da geocache, utilizador)
- Alteração: Removida a hiperligação desactualizada do leitor WhereYouGo. O leitor Wherigo interno é agora o padrão para Wherigos.
- Fix: Missing quick toggle in guided mode of waypoint calculator
- New: Aggregate functions with range support: add/sum, min/minimum, max/maximum, cnt/count, avg/average, multiply/product/prod

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
- Fix: Crahs in percentage favorite filter
- New: Make it easier to use simple lists as parent lists
- Change: Use local timezone (of device, not event) for calendar entries (instead of UTC)
