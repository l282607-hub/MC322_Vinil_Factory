# GroovePress Vinyl Works

Projeto de MC322: simulacao de uma fabrica de discos de vinil pelo terminal.

- Jeorde Antonio - RA 295164
- Leo Bertoli - RA 282607

## Tarefa 4: Gradle, Javadoc e tratamento de erros

O projeto agora e construido e executado pelo Gradle (com Gradle Wrapper),
todo o codigo esta documentado com Javadoc e as situacoes de erro viraram
excecoes de dominio tratadas no menu, sem encerrar o programa e sem deixar a
planta em estado inconsistente. A funcionalidade das Tarefas 2 e 3 foi
preservada. As decisoes estao em [justificativa.txt](justificativa.txt).

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
A auditoria mostra saude das maquinas e risco dos discos.
Maquinas quebradas ficam paradas ate encerrar a simulacao; reparo nao foi
implementado. Os dados nao sao salvos entre execucoes.

## Entrega

Criar a release `tarefa4-295164-282607` com o codigo completo e
`justificativa.txt`. Depois, enviar o link direto da release no formulario
da disciplina. As pastas `build/`, `.gradle/` e arquivos `.class` nao precisam
ser enviados.
