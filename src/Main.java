import java.util.Scanner;

/**
 * GroovePress Vinyl Works - Tarefa 2 (MC322).
 * Ponto de entrada: monta a planta, aloca o budget e roda o menu.
 */
public class Main {
    private static final double BUDGET_INICIAL = 1000.00;
    private static final String[] TIPOS = {
            LpAudiofiloDeluxe.TIPO,
            LpStandard.TIPO,
            CompactoSete.TIPO
    };

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        MateriaPrima pvc = new MateriaPrima(1, "PVC Reciclado", 20.0, "kg", 3.00);
        GerenciadorProducao fabrica = new GerenciadorProducao(pvc, BUDGET_INICIAL);

        // Linha: prensa -> embaladora -> inspecao (ordem importa)
        fabrica.adicionarMaquina(new PrensaHidraulica("Hydraulic Vinyl Press", 50, 0.20, 4.00));
        fabrica.adicionarMaquina(new EmbaladoraCapas("Sleeve-O-Matic", 80, 0.15, 1.50));
        fabrica.adicionarMaquina(new EstacaoInspecao("Golden Ear Station", 60, 0.05, 2.50));

        // Demandas iniciais das gravadoras (o usuario pode alterar pelo menu)
        fabrica.registrarDemanda(LpAudiofiloDeluxe.TIPO, 2);
        fabrica.registrarDemanda(LpStandard.TIPO, 5);
        fabrica.registrarDemanda(CompactoSete.TIPO, 10);

        exibirIntroducao(fabrica);

        int opcao;
        do {
            exibirMenu(fabrica);
            opcao = lerInteiro(scanner);

            if (opcao >= 1 && opcao <= 3) {
                String tipo = TIPOS[opcao - 1];
                System.out.print("Nova quantidade demandada de " + tipo + ": ");
                fabrica.atualizarDemanda(tipo, lerInteiroNaoNegativo(scanner));
            } else if (opcao >= 4 && opcao <= 6) {
                fabrica.fabricarDemanda(TIPOS[opcao - 4]);
            } else if (opcao == 7) {
                fabrica.exibirArmazem();
            } else if (opcao == 8) {
                fabrica.exibirEstoque();
                fabrica.exibirDemandas();
                fabrica.exibirMaquinas();
            } else if (opcao == 9) {
                System.out.print("Quantidade de PVC a comprar (kg): ");
                fabrica.comprarMateriaPrima(lerDoublePositivo(scanner));
            } else if (opcao == 0) {
                System.out.println("\nEncerrando a GroovePress Vinyl Works. Ate a proxima prensagem!");
            } else {
                System.out.println("[ERRO] Opcao invalida.");
            }
        } while (opcao != 0);

        scanner.close();
    }

    private static void exibirIntroducao(GerenciadorProducao fabrica) {
        System.out.println("==============================================");
        System.out.println("        GROOVEPRESS VINYL WORKS  -  v2");
        System.out.println("   Fabrica de discos de vinil - linha completa");
        System.out.println("==============================================");
        System.out.println("Novidades desta versao:");
        System.out.println("  * 3 formatos de disco com qualidades distintas");
        System.out.println("  * Linha prensa -> embaladora -> inspecao");
        System.out.println("  * Falhas aleatorias e reciclagem de rejeitados");
        System.out.println("  * Demandas das gravadoras, budget e armazem");
        System.out.println();
        System.out.println("Formatos: " + LpAudiofiloDeluxe.TIPO + " (qualidade 0.9), "
                + LpStandard.TIPO + " (0.7), " + CompactoSete.TIPO + " (0.5)");
        System.out.println("Atencao: quanto maior a qualidade, mais rigorosa a inspecao!");
        System.out.println();
        fabrica.exibirEstoque();
        fabrica.exibirMaquinas();
        fabrica.exibirDemandas();
        System.out.println("\nDesenvolvido por:");
        System.out.println("  Jeorde Antonio - RA 295164");
        System.out.println("  Leo Bertoli    - RA 282607");
    }

    private static void exibirMenu(GerenciadorProducao fabrica) {
        System.out.println("\n==============================================");
        System.out.println("          GROOVEPRESS VINYL WORKS");
        System.out.println("==============================================");
        fabrica.exibirBudget();
        System.out.println("\n ATUALIZAR DEMANDAS");
        System.out.println("  1 - Atualizar demanda de " + LpAudiofiloDeluxe.TIPO);
        System.out.println("  2 - Atualizar demanda de " + LpStandard.TIPO);
        System.out.println("  3 - Atualizar demanda de " + CompactoSete.TIPO);
        System.out.println("\n FABRICAR");
        System.out.println("  4 - Fabricar " + LpAudiofiloDeluxe.TIPO);
        System.out.println("  5 - Fabricar " + LpStandard.TIPO);
        System.out.println("  6 - Fabricar " + CompactoSete.TIPO);
        System.out.println("\n CONSULTAR");
        System.out.println("  7 - Ver armazem");
        System.out.println("  8 - Ver estoque, demandas e maquinas");
        System.out.println("\n COMPRAR MATERIA-PRIMA");
        System.out.println("  9 - Comprar PVC reciclado");
        System.out.println("\n  0 - SAIR");
        System.out.print("\nESCOLHA: ");
    }

    // ---- Leitura validada (apenas entradas numericas) ---------------------

    private static int lerInteiro(Scanner scanner) {
        while (!scanner.hasNextInt()) {
            System.out.print("Entrada invalida. Digite um numero inteiro: ");
            scanner.next();
        }
        return scanner.nextInt();
    }

    private static int lerInteiroNaoNegativo(Scanner scanner) {
        int valor = lerInteiro(scanner);
        while (valor < 0) {
            System.out.print("Digite um numero inteiro maior ou igual a zero: ");
            valor = lerInteiro(scanner);
        }
        return valor;
    }

    private static double lerDoublePositivo(Scanner scanner) {
        while (true) {
            if (scanner.hasNextDouble()) {
                double valor = scanner.nextDouble();
                if (valor > 0) {
                    return valor;
                }
            } else {
                scanner.next();
            }
            System.out.print("Entrada invalida. Digite um numero maior que zero: ");
        }
    }
}
