### Plano estratégico do Mapa Unificado & aviso de obsolescência mapas "antigos"
O c:geo tem já há algum tempo uma implementação de mapas nova chamada "Mapa Unificado", que irá no final substituir as implementações do Google Maps e Mapsforge (OpenStreetMap). Isto é um aviso de obscelecência para te informar acerca do próximo plano estratégico.

O Mapa Unificado foi disponibilizado à cerca de um ano. Ele ainda suporta o Google Maps e o OpenStreetMap (online + offline), mas de uma forma técnica completamente remodelada. e com uma série de novas e empolgantes funcionalidades que os mapas "antigos" não suportam, algumas das quais
- Rotação do mapa para mapas baseados em OpenStreetMap (online e offline)
- Janela pendente de agrupamento para o Google Maps
- Ocultar fontes de mapa de que não precisa
- Gráfico de altitude para rotas e percursos
- Alternar entre listas directamente do mapa
- "Modo de condução" para mapas baseados no OpenStreetMap

O Mapa Unificado provou ser estável há já bastante tempo, assim removeremos as antigas implementações do mapa para reduzir os esforços de manutenção do c:geo.

Roteiro:
- Mapas "Antigos" agora estão em modo de depreciação — não vamos corrigir mais erros para eles.
- O Mapa Unificado será definido como padrão para todos os utilizadores no outono de 2025.
- As implementações de mapa "Antigos" serão removidas na primavera de 2026.

Até lá, pode alternar entre as diferentes implementações nas definições => fontes de mapas.

### Mapa
- Novo: Mostrar perímetro de resposta para os pontos de AdventureLab (Mapa Unificado) — active "círculos" nas configurações rápidas do mapa para mostrá-los
- Novo: Opção para definir círculos com raio individual para pontos adicionais (opção de menu "perímetro de resposta")
- Correcção: Visualização do mapa não era actualizada ao remover a cache da lista actualmente mostrada
- Correcção: Número de caches no selector de lista não era actualizado ao alterar o conteúdo da lista
- Alteração: Manter a visualização actual no mapeamento de uma lista, se todas as caches couberem na visualização actual
- Novo: "Siga a minha localização" no gráfico de altitude (Mapa Unificado)
- Novo: Permitir as acções "mover para" / "copiar para" em "mostrar como lista"
- Novo: Suporte para o tema Elevate Winter no gestor de transferências de mapas
- Novo: Sombreado adaptativo de altitude, modo de alta qualidade opcional (Mapa Unificado Mapsforge)
- Novo: Redesenhado menu de definições rápidas de percursos/rotas
- Novo: Toque longo no ícone de seleção de mapa para selecionar o provedor de blocos anterior (Mapa Unificado)
- Novo: Permitir a configuração do nome para mapas offline no ficheiro complementar (Mapa Unificado)
- Novo: Toque longo no botão "Activar em tempo real" para carregar caches offline
- Novo: Sombreado de altitude offline para o Mapa Unificado (variante VTM)
- Novo: Suporte para mapas de plano de fundo (Mapa Unificado)
- Correcção: Ícones compactos não voltam para ícones grandes no zoom no modo automático (Mapa Unificado)
- Novo: Acções de toque longo no separador de informações da cache: código GC, título, coordenadas, nota pessoal/dica
- Alteração: Altera o separador de informações da cache para o seletor de emoji para um toque curto para resolver colisão

### Detalhes da cache
- Novo: Tradutor offline de texto da listing e registos (experimental)
- Novo: Opção para partilhar geocache com os dados do utilizador (coordenadas, nota pessoal)
- Correcção: Serviço de fala interrompido na rotação do ecrã
- Correcção: Detalhes da cache: Listas para a cache não eram actualizadas depois de tocar no nome da lista e remover essa cache dessa lista
- Correcção: a nota pessoal era perdida ao actualizar uma AL (Adventure Lab)
- Alteração: Data de registo relacionada com os marcadores de posição usarão a data escolhida em vez da actual
- Novo: Encurtar registos longos por defeito

### Wherigo
- Novo: Verificação integrada de falta de credenciais para Wherigo
- Alteração: Relatório Wherigo removido (pois os erros estão principalmente relacionados com cartuchos, precisam ser corrigidos pelo dono de cartucho)
- Novo: Capacidade de navegar para uma zona usando a bússola
- Novo: Capacidade de copiar coordenadas do centro da zona para a área de transferência
- Novo: Definir o centro da zona como alvo ao abrir o mapa (para obter informações de encaminhamento e distância para ele)
- Novo: Suporte para abrir ficheiros Wherigo locais
- Alteração: Toque longo numa zona do mapa já não é reconhecido. Isso permite que os utilizadores façam outras coisas na área da zona do mapa, disponíveis no toque longo, por exemplo: criar uma cache definido pelo utilizador
- Novo: Mostra um aviso se wherigo.com reportar a falta de EULA (que leva a falha na transferência do cartucho)

### Geral
- Novo: Página de pesquisa redesenhada
- Novo: Filtro de contagem de inventário
- Novo: Suporte para formato de coordenadas DD,DDDDDDD
- Novo: Mostrar o último nome de filtro usado na caixa de diálogo do filtro
- Novo: Calculadora de coordenadas: Função para substituir "x" pelo símbolo de multiplicação
- Correcção: Altitude incorreta (não está a usar a média acima do nível do mar)
- Correcção: Configuração de limite de distância próxima não funcionava correctamente para valores pequenos
- Correcção: Ordenação das listas de caches por distância decrescente não funcionava correctamente
- Correcção: Lab caches excluídas pelo filtro D/T mesmo com a opção activa "incluir incertas"
- Correcção: problemas de cor com os ícones do menu no modo claro
- Novo: Adicionar "Remover eventos passados" à lista "todos"
- Novo: Mostrar conector para "caches definidos pelo utilizador" como activo no filtro de origem
- Novo: Exportar GPX: tornado opcional a exportação de registos / trackables
- Novo: Adicionado botão para apagar modelos de registo
- Correcção: Ao importar o ficheiro de mapa local é atribuído um nome aleatório ao mapa
- Correcção: Motor de transferência do mapa a disponibilizar ficheiros com erro (0 bytes) para transferência
- Novo: Adicionados mapeamentos para alguns tipos de cache OC ausentes
- Novo: Mover listas "utilizadas recentemente" na caixa de diálogo de selecção de lista para o topo ao pressionar o botão "utilizadas recentemente"
- Novo: Partilha de geo-códigos a partir da lista de caches
- Alteração: "Navegação (carro)", etc. utiliza o parâmetro "q=" em vez do parâmetro desactualizado "ll="
