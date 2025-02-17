Novo: Integrado o Wherigo (beta) — veja a entrada de menu no ecrã inicial.<br> (Talvez queira [configurar um botão de início rápido](cgeo-setting://quicklaunchitems_sorted) ou [personalizar a navegação na parte inferior](cgeo-setting://custombnitem) para facilitar o acesso, para isso precisa primeiro de activar as definições adicionais.)

### Mapa
- Novo: Armazenar tema do mapa por provedor de blocos (Mapa Unificado)
- Novo: Destacar cache/ponto adicional selecionado (Mapa Unificado)
- Novo: Adicionado um separador entre fontes de mapas offline e online
- Novo: Suporte para Mapsforge como alternativa à VTM no Mapa Unificado, consulte [Definições => Fontes do Mapa => Mapa Unificado](cgeo-setting://useMapsforgeInUnifiedMap)
- Alteração: 'Mostrar gráfico de altitude' movido para o menu de toque longo (Mapa Unificado)
- Alteração: Utilização do novo algoritmo de sombreado da altitude para os mapas offline Mapsforge
- Novo: Suporte de sombreado da altitude para os mapas offline do Mapa Unificado Mapsforge
- Novo: Suporte de sombreado da altitude para o Mapa Unificado dos mapas VTM (requer ligação online)
- Correcção: Pesquisa por endereço não é tida em conta no mapa em tempo real (Mapa Unificado)
- Alteração: "Seguir a minha localização" foi movido para o mapa, dando mais espaço para o botão "mapa em tempo real"
- Alteração: Pin de pressão longa com aparência do estilo do c:geo
- Alteração: Funções de gestão de dados offline (descarregar mapas, verificar rotas em falta, dados de sombreado de altitude) movidas para o menu de selecção do mapa => "Gerir dados offline"
- Correcção: Mapa não actualiza geocaches alteradas

### Detalhes da cache
- Novo: Variáveis usadas no gerador de projeções, ainda não existentes, são criadas na lista de variáveis
- Novo: Permitir números grandes em fórmulas
- Novo: Suporte para mais constelações para variáveis em fórmulas
- Correcção: Múltiplas imagens numa nota pessoal não eram adicionadas ao separador de imagens
- Correcção: manipulação de projeções em pontos adicionais e notas pessoais
- Novo: Toque longo na data do registo recupera a data do registo anterior
- Correção: Redefinir cache para as coordenadas originais não removia a indicação "coordenadas alteradas"
- Novo: Confirmar sobrescrever registo no registo rápido offline
- Novo: Actualização do estado da geocache ao enviar um registo
- Novo: Visualização dos detalhes das geocaches em HTML colorido
- Correcção: Soma de controlo (0) devolve valor errado
- Correcção: Edição de registo remove estado "Amigos"

### Geral
- Alteração: Usar altitude acima do nível médio do mar (se disponível, Android 14+ apenas)
- Novo: Permitir vários níveis de hierarquia nas listas de caches
- Novo: Ícones específicos para os tipos de eventos blockparty e HQ, de geocaching.com
- Novo: Defina o tamanho de imagem preferido para imagens de caches e TBs descarregadas de geocaching.com
- Correcção: "Abrir no navegador" não funcionava para registos de TBs
- Novo: Opção para gerir ficheiros descarregados (mapas, temas e dados de encaminhamento e sombreado da altitude)
- Novo: Opção para remover uma cache de todas as listas (= marcar para ser apagada)
- Correcção: Redefinição de coordenadas não era detectada pelo c:geo para caches não gravadas
- Novo: Permitir limpar filtros se nenhum filtro nomeado estiver armazenado
- Correcção: Uma confirmação de "Lista vazia" era mostrada quando iniciada a transferência de uma pocket query numa lista recém-criada
- Alteração: É mostrado o marcado de registo offline nas caches que lhe pertençam (quando tenham registos offline)
- Novo: Formato de data configurável (por ex.: registos de cache), consulte [Configurações => Aparência => Formato de data](cgeo-settings://short_date_format)
- Novo: Direccionamento do conector de informação do ecrã principal para o conector específico do ecrã de preferências
- Novo: Emojis adicionais para os ícones das caches
- Alteração: Tipo de filtro de cache "Especiais" inclui eventos dos tipos Mega, Giga, Community Celebration, Celebration HQ, Block Party e Maze
- Alteração: Tipo de filtro de cache "Outro" inclui GCHQ, APE e tipos desconhecidos
- Correcção: Tamanho do histórico e configurações de proximidade partilham valores de selector
- Correcção: Página de registo de travelbug mostra campos de introdução de tempo/coordenadas para travelbugs que não suportam isso
- Correcção: Vários encerramentos abruptos
- Correcção: Alguns ajustes em configurações tinham problemas com valores não inicializados
