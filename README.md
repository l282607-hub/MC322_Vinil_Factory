# GroovePress Vinyl Works

Projeto de MC322: simulacao de uma fabrica de discos de vinil pelo terminal.

- Jeorde Antonio - RA 295164
- Leo Bertoli - RA 282607

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

Requer um JDK entre 17 e 24 ja instalado (o Gradle em si nao precisa ser
instalado: o Wrapper baixa a versao 8.14.3 na primeira execucao, entao a
primeira vez precisa de internet).

Windows (CMD ou PowerShell):

```bat
gradlew.bat build
gradlew.bat run
gradlew.bat javadoc
```

Linux/macOS:

```sh
./gradlew build
./gradlew run
./gradlew javadoc
```

Se aparecer `Permission denied` no Linux/macOS, o arquivo perdeu a permissao
de execucao no envio para o GitHub. Rode `chmod +x gradlew` uma vez, ou use
`sh gradlew build`.

| Tarefa | O que faz |
| --- | --- |
| `build` | compila, empacota e roda a conferencia automatizada |
| `run` | executa o programa interativo |
| `javadoc` | gera a documentacao em `build/docs/javadoc/index.html` |
| `verificar` | roda somente a conferencia automatizada (`TestePlanta`) |

Para repetir os sorteios, passe uma semente: `./gradlew run --args="322"`.
Para uma saida sem as linhas de progresso do Gradle, use `--console=plain`.

## Estrutura

```
build.gradle            configuracao da construcao (sem regras de negocio)
settings.gradle         nome do projeto
gradlew, gradlew.bat    Gradle Wrapper
gradle/wrapper/         jar e propriedades do Wrapper
justificativa.txt       decisoes de projeto
src/main/java/          codigo da aplicacao
src/test/java/          conferencia automatizada (TestePlanta)
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

## Fluxo

Pedido -> estrategia -> prensa -> embalagem -> inspecao -> armazem.
Rejeitados devolvem metade do PVC; apenas aprovados abatem a demanda.
A auditoria mostra linhas, maquinas, motor, sensor e todos os atuadores,
alem do risco dos discos no armazem. Saude estrutural e a menor condicao das
partes. Sensor desgastado aumenta a chance de falha; uma parte sem condicao
de operar para a maquina no proximo ciclo.
Maquinas quebradas ficam paradas ate encerrar a simulacao; reparo nao foi
implementado. Os dados nao sao salvos entre execucoes.

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

## Entrega

Criar a release `tarefa5-295164-282607` com o codigo completo, o diagrama UML e
`justificativa.txt`. Depois, enviar o link direto da release no formulario
da disciplina. As pastas `build/`, `.gradle/` e arquivos `.class` nao precisam
ser enviados.
