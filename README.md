# GroovePress Vinyl Works

Projeto de MC322: simulacao de uma fabrica de discos de vinil pelo terminal.

- Jeorde Antonio - RA 295164
- Leo Bertoli - RA 282607

## Tarefa 6: persistencia XML e testes JUnit

A planta pode ser salva e recuperada pelo menu, incluindo as partes de cada
maquina e as linhas de producao. A leitura valida o arquivo inteiro antes de
substituir a fabrica atual. A aplicacao continua usando somente a biblioteca
padrao do Java; JUnit 5 e uma dependencia exclusiva dos testes.

## Tarefa 5: composicao, construcao e acoplamento

Cada maquina agora constroi e controla um motor de acionamento, um sensor de
alinhamento e uma lista de atuadores de transporte. As maquinas ficam em
linhas de producao. A auditoria percorre linha, maquinas e suas partes.
Continuam presentes as estrategias, cenarios, estoque, reciclagem, Gradle e
tratamento de erros das tarefas anteriores.

O diagrama esta em [docs/estrutura-tarefa5.png](docs/estrutura-tarefa5.png),
com fontes editaveis em SVG e [draw.io](docs/estrutura-tarefa5.drawio).
As decisoes estao em [justificativa.txt](justificativa.txt), secao 9.

## Como construir e executar

Requer um JDK entre 17 e 24 ja instalado; recomendamos o JDK 17. O Gradle em
si nao precisa ser instalado: o Wrapper baixa a versao 8.14.3 na primeira
execucao, entao a primeira vez precisa de internet.

Windows (CMD ou PowerShell):

```bat
gradlew.bat build
gradlew.bat run
gradlew.bat test
gradlew.bat javadoc
```

Linux/macOS:

```sh
./gradlew build
./gradlew run
./gradlew test
./gradlew javadoc
```

Se aparecer `Permission denied` no Linux/macOS, o arquivo perdeu a permissao
de execucao no envio para o GitHub. Rode `chmod +x gradlew` uma vez, ou use
`sh gradlew build`.

| Tarefa | O que faz |
| --- | --- |
| `build` | compila, empacota e executa os testes JUnit |
| `run` | executa o programa interativo |
| `javadoc` | gera a documentacao em `build/docs/javadoc/index.html` |
| `test` | executa toda a suite JUnit 5 e gera o relatorio HTML |
| `verificar` | mantem o comando anterior como atalho para `test` |

O Wrapper 8.14.3 foi preservado. Caso o computador use Java 25 ou superior,
selecione um JDK compativel em `JAVA_HOME` antes de executar os comandos.
Por exemplo, no PowerShell: `$env:JAVA_HOME = 'C:\caminho\jdk-17'`.
O codigo e compilado com `--release 17`.

Para repetir os sorteios, passe uma semente: `./gradlew run --args="322"`.
Para uma saida sem as linhas de progresso do Gradle, use `--console=plain`.

## Estrutura

```
build.gradle            configuracao da construcao (sem regras de negocio)
settings.gradle         nome do projeto
gradlew, gradlew.bat    Gradle Wrapper
gradle/wrapper/         jar e propriedades do Wrapper
justificativa.txt       decisoes de projeto
src/main/java/br/unicamp/ic/mc322/
  Main.java            menu principal
  console/             leitura de entradas
  model/               dominio, componentes e enums
  service/             GerenciadorProducao e regras da planta
  strategy/            estrategias de selecao de pedidos
  persistence/         leitura, validacao e escrita XML
  exception/           excecoes da aplicacao
src/test/java/br/unicamp/ic/mc322/
  model/               TestePlanta (regressao das Tarefas 2 a 5)
  persistence/         PersistenciaPlantaTest
  MenuPersistenciaTest.java
docs/                  UML em PNG, SVG e draw.io
```

## Tratamento de erros

Tres niveis, do mais comum ao mais raro:

1. **Entrada invalida** (texto no lugar de numero, opcao inexistente, valor
   negativo, NaN): `LeitorConsole` explica o problema e pergunta de novo.
2. **Regra de negocio** (`PlantaException` e suas filhas): mensagem em caixa,
   pausa ate o ENTER e volta ao menu. Nada foi alterado.
3. **Falha inesperada** (defeito do programa): o usuario e avisado, o detalhe
   vai para `System.err` e o programa continua.

| Excecao | Quando |
| --- | --- |
| `DemandaInvalidaException` | pedido inexistente, tipo desconhecido, quantidade invalida, pedido ja concluido ou cancelado |
| `RecursoInsuficienteException` | falta de PVC ou de budget |
| `MaquinaIndisponivelException` | linha sem maquinas ou com maquina quebrada |
| `EntradaInvalidaException` | valor digitado que nao pode ser aceito |
| `PersistenciaException` | arquivo ausente, invalido ou falha de leitura/escrita |

## Fluxo

Pedido -> estrategia -> prensa -> embalagem -> inspecao -> armazem.
Rejeitados devolvem metade do PVC; apenas aprovados abatem a demanda.
A auditoria mostra linhas, maquinas, motor, sensor e todos os atuadores,
alem do risco dos discos no armazem. Saude estrutural e a menor condicao das
partes. Sensor desgastado aumenta a chance de falha; uma parte sem condicao
de operar para a maquina no proximo ciclo.
Maquinas quebradas ficam paradas; reparo nao foi implementado. O desgaste e
a quebra tambem sao preservados ao salvar e carregar.

## Linhas de producao

O menu 7 permite listar as linhas, montar outra linha, selecionar a linha
para os proximos lotes e consultar sua estrutura. A opcao de montagem usa
equipamentos novos, com os mesmos custos das etapas originais, e capacidade
de 30 discos por lote. O nome da linha identifica o setor; todos os formatos
de disco continuam compativeis. Estoque, budget, pedidos e armazem sao comuns
a planta, mas cada linha tem suas proprias maquinas e desgaste.

As colecoes retornadas sao copias imutaveis. Maquina nao retorna nenhuma de
suas partes: o restante do sistema usa ligar, desligar, processar e auditoria.
Uma linha vazia esta em montagem e nao pode produzir. Retirar uma maquina
desligada de uma linha nao destroi suas partes; ela pode ser reinstalada.
A mesma maquina nao pode estar instalada em duas linhas ao mesmo tempo.
Cada parte sofre seu proprio desgaste sorteado, sem uma ordem fixa de quebra.

## Escopo e Limitações

### Como salvar e carregar

Na primeira execucao, escolha o cenario para montar a fabrica inicial. Nenhum
arquivo e exigido nem carregado automaticamente: a recuperacao e uma escolha
explicita do usuario, evitando substituir sua sessao sem solicitar a operacao.

- **8 - Salvar estado da planta:** informe um caminho XML ou pressione ENTER
  para usar `fabrica.xml` no diretorio de execucao. O diretorio pai deve existir.
- **9 - Carregar estado da planta:** informe o arquivo salvo. A fabrica inteira
  e substituida apos a validacao, incluindo a linha selecionada e as estrategias.
- **0 - Sair:** encerra sem apagar o arquivo. Alteracoes feitas depois do ultimo
  salvamento ficam apenas na memoria; salve novamente para atualiza-las.

Para conferir entre execucoes: produza ou compre PVC, salve com 8, saia com 0,
inicie novamente e use 9. Nao e necessario escolher o mesmo cenario inicial:
o cenario salvo substitui o escolhido na nova sessao.

`PersistenciaPlanta` usa DOM para representar `planta > linhas > linha >
maquina > motor/sensor/atuadores`, alem de materia-prima, demandas e armazem.
Tipos concretos sao reconstruidos explicitamente, sem reflexao. As partes sao
criadas pela propria maquina e recebem os valores salvos; nenhuma instancia
de motor, sensor ou atuador e compartilhada. A ordem das maquinas na linha e
dos pedidos na fila e mantida. A linha ativa usa uma posicao validada.

A leitura verifica versao, estrutura, atributos obrigatorios, tipos conhecidos,
numeros finitos, intervalos, status, nomes de linha, IDs de produtos, lotes e
numeracao dos atuadores. Maquinas com partes zeradas precisam estar quebradas;
o motor ligado precisa concordar com o estado da maquina. Pedidos concluidos
tem quantidade restante zero; os demais estados tem quantidade positiva.
Elementos desconhecidos ou repeticoes de secoes unicas sao rejeitados. DTD e
entidades externas sao bloqueados. O arquivo nao e usado para executar codigo.

Todos os objetos sao reconstruidos antes da troca da fabrica. Uma falha deixa
estoque, budget, pedidos, maquinas, armazem e contador de produtos atuais intactos.
Uma nova gravacao escreve primeiro num arquivo temporario no mesmo diretorio;
somente depois substitui o destino por uma movimentacao atomica. Se o sistema
de arquivos nao permitir essa operacao, o salvamento informa a falha.

### Dados persistidos e dados em memoria

| Dados salvos em XML | Motivo |
| --- | --- |
| ID, nome, unidade, preco e quantidade do PVC | recuperar o estoque e o custo de novas compras |
| Tipo, nome, capacidade, custo e chance base das maquinas | manter a configuracao de producao |
| Status, falhas e limiar de manutencao | manter as condicoes operacionais e a auditoria |
| Saude e estado do motor, calibracao do sensor e integridade de cada atuador | preservar a composicao e o desgaste individual |
| Linhas, ordem e pertencimento das maquinas; linha ativa | reconstruir as relacoes de agregacao sem compartilhar maquinas |
| Demandas, quantidades restantes, todos os StatusDemanda e estimativas | manter a fila, o historico e as escolhas das estrategias |
| Budget, cenario e estrategia | continuar a simulacao com os mesmos recursos e regras |
| Discos aprovados: tipo, ID, lote, status e risco | preservar o armazem e sua auditoria |
| Ultimo lote e contador de produtos, inclusive rejeitados | evitar repetir identificadores e numeros de lote |

Consumo de PVC e qualidade dos discos aprovados sao determinados pelo tipo e
reconstruidos pelas constantes do formato. Nao sao duplicados no XML.
Ao carregar um arquivo antigo na mesma execucao, o contador global de produtos
nunca diminui, evitando reutilizar IDs ja emitidos nessa execucao.

Ficam apenas na memoria a posicao atual nos menus, as mensagens do terminal,
o custo transitorio do ultimo processamento da linha e o estado interno do
gerador aleatorio. Esses dados nao sao necessarios para recuperar os recursos
e as relacoes da planta. A semente pode repetir uma simulacao desde seu inicio;
carregar um arquivo nao retoma a sequencia exata de sorteios de onde ela parou.
Discos rejeitados continuam sendo reciclados e nao formam um historico individual:
seu efeito no estoque, no budget e no contador de produtos e persistido.

A simulacao e sequencial. Salvar pelo menu acontece entre lotes, sem discos
em transito. Todos os valores de StatusDemanda podem ser representados e
recuperados, mas nao ha retomada de um disco no meio de uma operacao interrompida.
O projeto continua trabalhando com um unico estoque de PVC, tres formatos de
disco e os tres tipos existentes de maquina. Nao ha reparo, banco de dados,
acesso concorrente ou conversor de outras versoes de XML. O diagrama da Tarefa 5
foi mantido como registro da composicao; os pacotes e a persistencia estao
descritos nesta secao.

### Suite de testes

Execute `./gradlew test` (Windows: `gradlew.bat test`). O comando `build`
tambem executa os testes. O relatorio HTML e gerado em
`build/reports/tests/test/index.html`; os resultados XML ficam em
`build/test-results/test`. Para forcar uma nova execucao, use
`./gradlew test --rerun-tasks`.

| Classe ou grupo | O que verifica |
| --- | --- |
| `TestePlanta`: pedidos e estrategias | transicoes de status, quantidades, prioridades e viabilidade financeira |
| `TestePlanta`: maquinas e composicao | funcionamento, falhas, desgaste independente, partes exclusivas e auditoria |
| `TestePlanta`: linhas | capacidade, custos, listas protegidas, exclusividade e transferencia de maquinas |
| `TestePlanta`: estoque e gerenciador | compras, consumo, reciclagem, capacidade dos lotes, aprovados e debitos |
| `TestePlanta`: simulacoes | conservacao de PVC, contabilidade e limites nos dois cenarios com 100 sementes em cada um |
| `TestePlanta`: erros e console | excecoes de dominio, preservacao do estado, entradas invalidas e continuidade do menu |
| `PersistenciaPlantaTest`: recuperacao | configuracao completa, tipos, partes, estados, linhas, pedidos, armazem e IDs |
| `PersistenciaPlantaTest`: continuidade | atualizacao do arquivo sem duplicar dados, reinstalacao e producao depois de carregar |
| `PersistenciaPlantaTest`: erros | arquivos vazios, ausentes, corrompidos, dados invalidos, referencias inconsistentes e falhas de E/S |
| `PersistenciaPlantaTest`: preservacao | falhas tardias nao consomem IDs nem mudam a planta; gravacao rejeitada preserva o arquivo anterior |
| `MenuPersistenciaTest` | salvar/carregar pelo menu, substituicao da fabrica e continuidade apos falhas |

Os grupos anteriores de `TestePlanta` foram mantidos e passaram a ser casos
JUnit 5. As assercoes usam JUnit; cada teste prepara sua planta e define as
sementes de que precisa. Os testes de persistencia usam `@TempDir`, separado
dos dados reais, e casos parametrizados para estados e entradas invalidas.
As entradas do menu sao simuladas, sem exigir teclado, e os fluxos padrao sao
restaurados ao final de cada teste. A execucao e sequencial, como a aplicacao.

## Entrega

Release prevista: `tarefa6-295164-282607`, contendo os fontes, testes,
configuracao Gradle, README, Javadocs no codigo e documentacao anterior.
Antes de publicar, executar `build`, `test` e `javadoc` e conferir o relatorio.
Os fontes ficam disponiveis nos arquivos automaticos de codigo-fonte do GitHub.
Nao incluir arquivos reais de dados, `build/`, `.gradle/` nem arquivos `.class`.
Depois da publicacao, usar o link direto da release no formulario da disciplina.
