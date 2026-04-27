##
Time to update! If you are still using Android 7 or older, this might be the last c:geo update for you! With our next feature release of c:geo we will drop support for Android 5-7 to reduce our maintenance load and to be able to update some external components used by c:geo which we are currently still holding back. We will still be supporting Android 8 up to Android 16 then (and newer versions when they will be published), a span of more than eight years of Android history.

 - Alteração: Os ficheiros Wherigo não podem ser transferidos de momento. Visualizar instruções de mitigação
 - Correcção: O motivo da eliminação do registo não dava ênfase ao limite de comprimento
 - Novo: Registo alargado para falhas no gestor de transferências
 - Correcção: Informações do Ponto Adicional pode tornar-se muito longa, botões inacessíveis

##
- Correcção: A análise da localização da cache falha, para determinados idiomas do sítio web
- Correcção: A abertura de um TB a partir da lista de observação falhava
- Correcção: O teclado podia bloquear a seleção da lista
- Correcção: O fornecedor de blocos definido pelo utilizador não suportava parâmetros URL adicionais
- Correcção: Inventário / TBs (Trackables) numa cache não era(m) carregado(s)
- Alteração: O agente de utilizador interno foi actualizado para corrigir alguns problemas de transferências
- Correcção: visualização de detalhes do TB removia-o do inventário da cache

##
- Correcção: Janela de transferência de tradução off-line mostrada em instalações sem suporte a tradução offline
- Correcção: Alteração do formato das coordenadas na página de informações da cache/ponto adicional
- Correcção: Data de registo cortada na lista de registos (dependendo do formato da data e tamanho da fonte)
- Correcção: Horários dos eventos não eram detectados em certas condições
- Correcção: Ligação na listagem não era clicável sob certas condições
- Correcção: Por vezes, acções de registo para TBs (trackables) ficavam misturadas

##
- Alteração: Número máximo de rastreáveis (TB) a visitar por registo de cache reduzido para 100 (por solicitação de geocaching.com, para reduzir a carga do servidor causado por amantes de TB extremos)
- Correcção: Algumas possíveis excepções de segurança quando o utilizador não concedeu certas permissões (por exemplo: notificações)
- Correcção: Círculos incompletos de caches em níveis de zoom baixos (apenas VTM)
- Correcção: Falha ao recarregar pontos adicionais em certas condições de carga
- Correcção: O filtro de data do evento não funciona em determinadas condições
- Correcção: Limite máximo de linhas de registo não funcionava de forma confiável na configuração "ilimitado"
- Correcção: Encerramento inesperado em certas condições ao abrir mapa
- Correcção: O mapa não era mostrado caso a wherigo não tivesse zonas visíveis
- Correcção: Falha no separador dos detalhes da cache em certas condições
- Correcção: Pesquisas no mapa com coordenadas inválidas
- Correcção: Algumas traduções não respeitam as definições da linguagem interna do c:geo

##
- Mudança: Mapa Unificado definido como mapa padrão para qualquer pessoa (como parte do nosso roteiro para o Mapa Unificado) Pode voltar ao anterior em "Definições" - "Provedores de mapas", por enquanto. A remoção dos mapas antigos está prevista para a primavera de 2026, nos nossos lançamentos regulares.
- Correcção: A caixa de seleção de favorito é reposta ao regressar ao ecrã de registo offline
- Correcção: Caixa de introdução do raio do perímetro de resposta mostra um número decimal
- Correcção: Sincronização das notas pessoais não funcionava
- Alteração: Novo ícone para importar percurso/rota GPX nas definições rápidas do mapa

##
- Correcção: Valores negativos no gráfico de altitude não eram dimensionados
- Correcção: Coordenadas próximas de 0 ficavam inválidas na exportação para GPX
- Correcção: Alguns encerramentos abruptos
- Tentativa de correcção: Aplicação Não Responde no arranque
- Tentativa de correcção: Dados de geocache em falta no mapa ao vivo

##
- Correcção: Falha na pesquisa por palavra-chave
- Correcção: Bloqueio no mapa
- Correcção: Texto de dica não era seleccionável
- Correcção: Vários erros de Wherigo

##
- Correcção: Criptografar/Descriptografar uma dica obrigava, na primeira vez, a um toque extra
- Correcção: Falha Wherigo ao ler jogos gravados antigos
- Correcção: O registo a partir do c:geo por vezes não era recordado
- Correcção: Actualização de dados ao vivo em falta, para caches encontradas & arquivadas
- Correcção: Pontos adicionais no mapa offline por vezes não eram mostrados

##
- Correcção: Dicas de cache não encriptadas (alteração do site)
- Correcção: As Adventure Lab não eram carregadas, devido a alteração no site (para poder executá-las novamente a partir do c:geo, terá de actualizar as Adventure Lab armazenadas)
- Correcção: Mapa Unificado VTM: Alternar edifícios 3D não funciona para mapas combinados
- Correcção: Tradução offline: O idioma da descrição às vezes é detectado como --

##
- Correcção: Falha no módulo de tradução
- Correcção: Falha na detecção do início de sessão (devido a mudança no sítio Web)
- Correcção: Falha ao obter cartucho Wherigo
- Correcção: "Carregar mais" não respeitava os filtros offline

##
- Correcção: Inventário de Trackables não era carregado enquanto registava uma cache

##
- Correcção: Migração de caches definidas pelo utilizador durante a inicialização do c:geo falhava => removida a funcionalidade, por enquanto
- Correcção: As tarefas Wherigo finalizadas não eram marcadas como terminadas ou falhadas































