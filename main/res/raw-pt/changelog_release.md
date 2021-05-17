## Versão Beta 2021.05.11-RC

### Serviços Geocaching
- Novo: Adicionada uma ligação para as Caches Adventure Lab - Mostra as coordenadas iniciais da Aventura, com informações básicas no mapa e pesquisas (apenas Membros Premium). Use a ligação na página de detalhes da cache para iniciar a aplicação Adventure Lab e ir para a Aventura.

### Detalhes da Cache
- New: Long click on waypoint coordinates to copy coordinates
- New: Export and import user defined caches with empty coordinates
- Novo: Suportada a alteração do estado de 'encontrada' nas caches definidas pelo utilizador e em geocaches Adventure Lab
- New: Parse formula for waypoints in personal cache note
- Novo: Adicionado indicador para coordenadas calculadas na lista de pontos adicionadas


### Mapa
- Novo: Verificação automática de actualizações do mapa e ficheiros de temas descarregados (opcional)
- Novo: BRouter: Mostrar mensagem de informação na falta de dados de encaminhamento
- Novo: Exportar rota individual como percurso (além de "Exportar como rota")

### Motor de encaminhamento integrado
- Novo: Motor de encaminhamento do BRouter integrado - agora pode usar a aplicação externa BRouter ou o mecanismo integrado de encaminhamento
- Novo: O mecanismo de encaminhamento integrado suporta a descarga automática de blocos de encaminhamento em falta
- Novo: O mecanismo de encaminhamento integrado suporta actualizações automáticas de blocos de encaminhamento descarregados
- Novo: O mecanismo de encaminhamento integrado suporta a selecção de diferentes perfis de encaminhamento


### Outro
- Alteração: "Ordenar rota individual" fecha automaticamente ao gravar e procura por mudanças não gravadas, usando a seta para trás
- Fix: A couple of theming issues, esp. aligned theming of Google Maps and settings to "rest of app"
- Fix: Optimize global search: If no trackable with matching tracking code is found, execute online cache name search afterwards
- Fix: Avoid avatar images being displayed too wide and pushing the "Update / remove authorization" functionalty aside
- Fix: Fix conversion error in some distance settings for imperial units
- New: Debug view for pending downloads
- Fix: Directory selected by user not taken over in wizard on older devices
- Fix: Scan for map themes now run as background task on startup
- Fix: Changing map source via settings being recognized after full restart only
- Correcção: Falha em "Ver configurações", sob certas condições
- Correcção: Seta para trás no descarregador de mapas volta ao ecrã principal
- Correcção: Evita mensagens pop-up estranhas ao anexar imagem ao registo
- Correcção: possível bloqueio no mapa
