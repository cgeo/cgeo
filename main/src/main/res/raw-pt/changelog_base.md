### Mapa
- Novo: Fonte de mapa OSM osm.paws.cz
- Novo: Activar a leitura de ficheiros GPX flopp.net como percursos
<<<<<<< HEAD
- Fix: Missing routing symbol after 'append to route'
- Fix: Missing route calculation for prepended points
- New: Add support for 'Voluntary MF5' OpenAndroMaps theme
- New: Add support for GeoJSON data
- Change: Use last known map position as fallback (when no GPS available and "follow my location" inactive)
- New: Refresh caches in route
- New: Individual coloring of tracks
=======
- Correcção: faltava o símbolo de encaminhamento, após 'anexar à rota'
- Correcção: Falta do cálculo de rota para pontos anteriores adicionados
- Novo: Adicionar suporte para o tema "MF5 Voluntário" de OpenAndroMaps
- Novo: Adicionado suporte para dados GeoJSON
>>>>>>> release

### Detalhes da cache
- Novo: Nova galeria de imagens mais sofisticada
- Correcção: Restaurar posição na lista de pontos adicionais, após actualizar ou excluir um deles
- Correcção: Mover para baixo ao criar um novo ponto adicional
- Novo: Reconhecer variáveis inseridas em notas do utilizador, nos pontos adicionais
- Novo: Exibir botão de AL nos detalhes da cache mistério se for detectada ligação para a aventura
- Correcção: Remoção da descrição num ponto adicional não sincronizada para os pontos adicionais no servidor
- Correcção: lista de Pontos Adicionais não era atualizada após a varredura

### Geral
- Alteração do nível da API (compileSDK 32)
- Actualização de algumas bibliotecas dependentes
- Alteração: Usa outro mecanismo do Android para receber transferências (para melhor compatibilidade com o Android 12+)
- Novo: Predefinir o nome das listas com o nome do ficheiro GPX ao importar
<<<<<<< HEAD
- New: Allow import of GPX track files that do not provide a xmlns namespace tag
- New: Add monochrome launcher icon for Android 13
- New: Display geocaching.com member status on home screen
- Change: GPX-Import: Use name as geocode for 'unknown' connector
- Fix: Allow filtering for archived caches in owner search
- Fix: Line breaks sometimes missing in logbook view directly after posting a log
- Fix: Preview button displayed only with filter setting "show all" in PQ list
- Fix: Several crashes
=======
- Novo: Permitir a importação de ficheiros de rotas GPX que não fornecem dados de xmlns
- Novo: Adicionado ícone monocromático da aplicação para Android 13
- Novo: Exibir estado de membro geocaching.com no ecrã inicial
- Alteração: Importação de GPX: Usar o nome como geocódigo para conector 'desconhecido'
- Correcção: Permitir filtragem para caches arquivadas, na pesquisa por proprietário
- Correcção: Por vezes faltam quebras de linha na visualização dos registos, logo após ser feito um registo
- Correcção: Vários encerramentos abruptos
>>>>>>> release
