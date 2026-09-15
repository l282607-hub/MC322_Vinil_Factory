# GroovePress Vinyl Works

Projeto de MC322 (Programacao Orientada a Objetos) que simula, pelo terminal,
uma fabrica de discos de vinil.

## Integrantes

- Jeorde Antonio - RA 295164
- Leo Bertoli - RA 282607

## Tarefa 2 - Expansao e Complexidade

A planta agora tem:

- **3 formatos de disco** (subclasses de `Produto`): `LpAudiofiloDeluxe` (0.9),
  `LpStandard` (0.7) e `CompactoSete` (0.5), cada um com consumo de PVC e tempo
  de prensagem proprios;
- **Linha de 3 maquinas** (subclasses de `Maquina`): `PrensaHidraulica` ->
  `EmbaladoraCapas` -> `EstacaoInspecao`, com falhas aleatorias (`Random`);
- **Inspecao proporcional a qualidade**: discos premium sao reprovados com mais
  frequencia, e defeitos acumulados nas maquinas anteriores somam ao risco;
- **Demandas** das gravadoras, **budget** (debitado na compra de PVC e em cada
  operacao de maquina) e **armazem** de discos aprovados, todos em `ArrayList`;
- **Reciclagem**: discos reprovados sao triturados e 50% do PVC volta ao estoque.

## Estrutura

```
src/
  Main.java                 menu e alocacao do budget
  GerenciadorProducao.java  demandas, maquinas, armazem, budget
  Demanda.java
  MateriaPrima.java
  Produto.java              abstrata
  LpAudiofiloDeluxe.java
  LpStandard.java
  CompactoSete.java
  Maquina.java              abstrata
  PrensaHidraulica.java
  EmbaladoraCapas.java
  EstacaoInspecao.java
justificativa.txt
```

## Compilar e executar

```bash
javac -d bin $(find src -name "*.java")
java -cp bin Main
```

No Windows (PowerShell):

```powershell
javac -d bin (Get-ChildItem -Recurse src -Filter *.java | ForEach-Object FullName)
java -cp bin Main
```

O programa aceita apenas entradas numericas.
