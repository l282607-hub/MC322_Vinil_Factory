# GroovePress Vinyl Works

Projeto de MC322: simulacao de uma fabrica de discos de vinil pelo terminal.

- Jeorde Antonio - RA 295164
- Leo Bertoli - RA 282607

## Tarefa 3

Tres estrategias de selecao de pedidos, enums para os estados, auditoria de
maquinas e produtos, cenarios Ideal/Apocaliptico, desgaste e armazem por lote.
As decisoes e os parametros estao em [justificativa.txt](justificativa.txt).

## Compilar e executar

Requer um JDK (Java 8 ou superior). Execute na pasta do projeto.

CMD:

```bat
javac -encoding UTF-8 -d bin src\*.java
java -cp bin Main
```

PowerShell:

```powershell
javac -encoding UTF-8 -d bin (Get-ChildItem src -Filter *.java | ForEach-Object FullName)
java -cp bin Main
```

Linux/macOS:

```sh
javac -encoding UTF-8 -d bin src/*.java
java -cp bin Main
```

Para repetir os sorteios, use uma semente: `java -cp bin Main 322`.
Escolha o cenario inicial e navegue pelos submenus. Zero volta ou encerra.
Em quantidade de PVC, tanto `1,5` quanto `1.5` sao aceitos.

## Fluxo

Pedido -> estrategia -> prensa -> embalagem -> inspecao -> armazem.
Rejeitados devolvem metade do PVC; apenas aprovados abatem a demanda.
A auditoria mostra saude das maquinas e risco dos discos.
Maquinas quebradas ficam paradas ate encerrar a simulacao; reparo nao foi
implementado. Os dados nao sao salvos entre execucoes.

## Conferencia automatizada

Os testes usam apenas Java, sem bibliotecas externas:

```bat
javac -encoding UTF-8 -cp bin -d bin tests\TesteTarefa3.java
java -cp bin TesteTarefa3
```

No Linux/macOS, troque a barra do caminho por `tests/TesteTarefa3.java`.

## Entrega

Criar a release `tarefa3-295164-282607` com o codigo completo e
`justificativa.txt`. Depois, enviar o link direto da release no formulario
da disciplina. As pastas `bin/` e arquivos `.class` nao precisam ser enviados.
