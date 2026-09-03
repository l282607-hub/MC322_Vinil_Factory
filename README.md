# GroovePress Vinyl Works

Projeto da Tarefa 1 de MC322 que simula, pelo terminal, uma linha simples de
producao de discos de vinil.

## Integrantes

- Jeorde Antonio - RA 295164
- Leo Bertoli - RA 282607

## Requisitos

- Java 25

## Compilacao e execucao

A partir da raiz do repositorio, execute:

```bash
javac -d bin $(find src -name "*.java")
java -cp bin Main
```

No Prompt de Comando do Windows, os comandos equivalentes sao:

```cmd
javac -d bin src\*.java
java -cp bin Main
```

O programa aceita entradas numericas para iniciar uma producao, consultar o
estoque, adicionar PVC ao estoque ou encerrar a simulacao.
