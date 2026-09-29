import java.util.Scanner;

/** Console da GroovePress. As regras de fabricacao ficam no gerenciador. */
public class Main {
    private static final String[] TIPOS = {
        LpAudiofiloDeluxe.TIPO, LpStandard.TIPO, CompactoSete.TIPO
    };

    public static void main(String[] args) {
        // Semente opcional para repetir uma simulacao: java -cp bin Main 322
        if (args.length > 0) {
            try {
                Maquina.definirSemente(Long.parseLong(args[0]));
            } catch (NumberFormatException e) {
                System.out.println("Semente invalida. Use um numero inteiro.");
                return;
            }
        }
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("GROOVEPRESS VINYL WORKS - ANTES DE BAIXAR A AGULHA");
            System.out.println("1 - Ideal: estudio em dia (R$ 10000, falhas e desgaste baixos)");
            System.out.println("2 - Apocaliptico: turne do caos (R$ 160, falhas e desgaste altos)");
            int escolha = lerOpcao(scanner, 1, 2);
            if (escolha == -1) {
                return;
            }
            Cenario cenario = escolha == 1 ? Cenario.IDEAL : Cenario.APOCALIPTICO;
            MateriaPrima pvc = new MateriaPrima(1, "PVC Reciclado", 20.0, "kg", 3.0);
            GerenciadorProducao fabrica = new GerenciadorProducao(
                    pvc, cenario, new EstrategiaFilaGravadoras());
            fabrica.adicionarMaquina(new PrensaHidraulica("Hydraulic Vinyl Press", 50, 0.20, 4.0));
            fabrica.adicionarMaquina(new EmbaladoraCapas("Sleeve-O-Matic", 80, 0.15, 1.5));
            fabrica.adicionarMaquina(new EstacaoInspecao("Golden Ear Station", 60, 0.05, 2.5));
            fabrica.registrarDemanda(LpAudiofiloDeluxe.TIPO, 2);
            fabrica.registrarDemanda(LpStandard.TIPO, 5);
            fabrica.registrarDemanda(CompactoSete.TIPO, 10);

            boolean executando = true;
            while (executando) {
                cabecalho(fabrica, "MESA DE CONTROLE");
                System.out.println("1 - Pedidos das gravadoras");
                System.out.println("2 - Sala de prensagem");
                System.out.println("3 - Consultar estoques");
                System.out.println("4 - Comprar PVC");
                System.out.println("5 - Trocar estrategia");
                System.out.println("6 - Auditoria");
                System.out.println("0 - Sair");
                int opcao = lerOpcao(scanner, 0, 6);
                switch (opcao) {
                    case 1: menuDemandas(scanner, fabrica); break;
                    case 2: menuProducao(scanner, fabrica); break;
                    case 3: menuConsultas(scanner, fabrica); break;
                    case 4:
                        System.out.println("Quantidade de PVC em kg (0 para voltar):");
                        double quantidade = lerQuantidade(scanner);
                        if (quantidade > 0) {
                            fabrica.comprarMateriaPrima(quantidade);
                        }
                        break;
                    case 5: menuEstrategias(scanner, fabrica); break;
                    case 6: menuAuditoria(scanner, fabrica); break;
                    default: executando = false;
                }
            }
            System.out.println("Agulha levantada. Ate a proxima prensagem!");
        }
    }

    private static void cabecalho(GerenciadorProducao fabrica, String titulo) {
        System.out.println("\n============================================================");
        System.out.println(" GROOVEPRESS | " + titulo);
        System.out.println(" Cenario: " + fabrica.getCenario().getDescricao());
        System.out.println(" Estrategia: " + fabrica.getNomeEstrategia());
        fabrica.exibirBudget();
        System.out.println("============================================================");
    }

    private static void menuDemandas(Scanner scanner, GerenciadorProducao fabrica) {
        while (true) {
            cabecalho(fabrica, "PEDIDOS DAS GRAVADORAS");
            System.out.println("1 - Registrar/atualizar quantidade pendente");
            System.out.println("2 - Listar pedidos e estimativas");
            System.out.println("0 - Voltar");
            int opcao = lerOpcao(scanner, 0, 2);
            if (opcao <= 0) { return; }
            if (opcao == 2) {
                fabrica.exibirDemandas();
            } else {
                int tipo = escolherTipo(scanner);
                if (tipo <= 0) { continue; }
                System.out.println("Quantidade pendente (inteiro positivo; 0 para voltar):");
                int quantidade = lerOpcao(scanner, 0, Integer.MAX_VALUE);
                if (quantidade > 0) {
                    fabrica.atualizarDemanda(TIPOS[tipo - 1], quantidade);
                }
            }
        }
    }

    private static void menuProducao(Scanner scanner, GerenciadorProducao fabrica) {
        while (true) {
            cabecalho(fabrica, "SALA DE PRENSAGEM");
            System.out.println("1 - Proximo pedido pela estrategia ativa");
            System.out.println("2 - Fabricar um formato especifico");
            System.out.println("0 - Voltar");
            int opcao = lerOpcao(scanner, 0, 2);
            if (opcao <= 0) { return; }
            if (opcao == 1) {
                fabrica.executarProximaProducao();
            } else {
                int tipo = escolherTipo(scanner);
                if (tipo > 0) {
                    fabrica.fabricarDemanda(TIPOS[tipo - 1]);
                }
            }
        }
    }

    private static void menuConsultas(Scanner scanner, GerenciadorProducao fabrica) {
        while (true) {
            cabecalho(fabrica, "ESTOQUES");
            System.out.println("1 - Armazem de discos prontos");
            System.out.println("2 - Estoque de PVC");
            System.out.println("0 - Voltar");
            int opcao = lerOpcao(scanner, 0, 2);
            if (opcao <= 0) { return; }
            if (opcao == 1) { fabrica.exibirArmazem(); }
            else { fabrica.exibirEstoque(); }
        }
    }

    private static void menuEstrategias(Scanner scanner, GerenciadorProducao fabrica) {
        cabecalho(fabrica, "ORDEM DA PRENSAGEM");
        System.out.println("1 - Fila das gravadoras: primeiro pedido pendente");
        System.out.println("2 - Grande tiragem: maior quantidade pendente");
        System.out.println("3 - Mais discos na loja: mais unidades com o budget atual");
        System.out.println("0 - Voltar");
        int opcao = lerOpcao(scanner, 0, 3);
        switch (opcao) {
            case 1: fabrica.setEstrategia(new EstrategiaFilaGravadoras()); break;
            case 2: fabrica.setEstrategia(new EstrategiaGrandeTiragem()); break;
            case 3: fabrica.setEstrategia(new EstrategiaMaisDiscos()); break;
            default: return;
        }
        System.out.println("Estrategia ativa: " + fabrica.getNomeEstrategia());
    }

    private static void menuAuditoria(Scanner scanner, GerenciadorProducao fabrica) {
        while (true) {
            cabecalho(fabrica, "AGULHA FINA");
            System.out.println("1 - Relatorio geral");
            System.out.println("2 - Detalhar maquinas");
            System.out.println("3 - Detalhar discos do armazem");
            System.out.println("0 - Voltar");
            int opcao = lerOpcao(scanner, 0, 3);
            if (opcao <= 0) { return; }
            if (opcao == 1) { fabrica.gerarAuditoriaGeral(); }
            else if (opcao == 2) { fabrica.exibirMaquinas(); }
            else { fabrica.exibirArmazem(); }
        }
    }

    private static int escolherTipo(Scanner scanner) {
        for (int i = 0; i < TIPOS.length; i++) {
            System.out.println((i + 1) + " - " + TIPOS[i]);
        }
        System.out.println("0 - Voltar");
        return lerOpcao(scanner, 0, TIPOS.length);
    }

    /** Leitura por linha evita misturar nextInt/nextLine e trata fim da entrada. */
    private static int lerOpcao(Scanner scanner, int minimo, int maximo) {
        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) { return -1; }
            try {
                int valor = Integer.parseInt(scanner.nextLine().trim());
                if (valor >= minimo && valor <= maximo) { return valor; }
            } catch (NumberFormatException e) {
                // Repete a pergunta sem encerrar o programa.
            }
            System.out.println("Digite um inteiro entre " + minimo + " e " + maximo + ".");
        }
    }

    private static double lerQuantidade(Scanner scanner) {
        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) { return -1; }
            try {
                double valor = Double.parseDouble(scanner.nextLine().trim().replace(',', '.'));
                if (Double.isFinite(valor) && valor >= 0) { return valor; }
            } catch (NumberFormatException e) {
                // Aceita tanto virgula quanto ponto como separador decimal.
            }
            System.out.println("Digite uma quantidade valida, maior ou igual a zero.");
        }
    }
}
