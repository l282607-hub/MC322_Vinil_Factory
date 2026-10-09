import java.util.Scanner;

/**
 * Console da GroovePress. Cuida apenas da navegacao e da apresentacao; as
 * regras de fabricacao ficam no {@link GerenciadorProducao}.
 *
 * <h2>Tratamento de erros</h2>
 * <p>Os erros sao tratados em tres niveis, do mais comum ao mais raro:</p>
 * <ol>
 *   <li><strong>Entrada invalida</strong> (texto no lugar de numero, opcao
 *       inexistente): o {@link LeitorConsole} explica o problema e pergunta
 *       de novo.</li>
 *   <li><strong>Regra de negocio</strong> ({@link PlantaException}: falta de
 *       PVC ou budget, pedido inexistente, linha parada): a mensagem e
 *       mostrada e o usuario volta ao menu. A planta nao foi alterada.</li>
 *   <li><strong>Falha inesperada</strong> (qualquer outra
 *       {@link RuntimeException}, indicio de defeito no programa): e
 *       informada ao usuario e registrada em {@code System.err}, e o programa
 *       continua, pois o gerenciador mantem o estado consistente.</li>
 * </ol>
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 */
public class Main {
    private static final String[] TIPOS = {
        LpAudiofiloDeluxe.TIPO, LpStandard.TIPO, CompactoSete.TIPO
    };
    private static final int LARGURA_MENSAGEM = 58;

    /** Impede a criacao de instancias: a classe so oferece o ponto de entrada. */
    private Main() {
    }

    /**
     * Ponto de entrada do programa.
     *
     * @param args argumento opcional: uma semente inteira para repetir os
     *             sorteios, por exemplo {@code 322}
     */
    public static void main(String[] args) {
        // Semente opcional para repetir uma simulacao: ./gradlew run --args="322"
        if (args.length > 0) {
            try {
                Maquina.definirSemente(Long.parseLong(args[0]));
            } catch (NumberFormatException e) {
                System.out.println("Semente invalida: '" + args[0] + "'. Use um numero inteiro, por exemplo 322.");
                return;
            }
        }
        try (Scanner scanner = new Scanner(System.in)) {
            LeitorConsole leitor = new LeitorConsole(scanner);
            System.out.println("GROOVEPRESS VINYL WORKS - ANTES DE BAIXAR A AGULHA");
            System.out.println("1 - Ideal: estudio em dia (R$ 10000, falhas e desgaste baixos)");
            System.out.println("2 - Apocaliptico: turne do caos (R$ 160, falhas e desgaste altos)");
            int escolha = leitor.lerOpcao(1, 2);
            if (escolha == LeitorConsole.FIM_DA_ENTRADA) {
                return;
            }
            Cenario cenario = escolha == 1 ? Cenario.IDEAL : Cenario.APOCALIPTICO;
            GerenciadorProducao fabrica = montarFabrica(cenario);

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
                int opcao = leitor.lerOpcao(0, 6);
                switch (opcao) {
                    case 1: menuDemandas(leitor, fabrica); break;
                    case 2: menuProducao(leitor, fabrica); break;
                    case 3: menuConsultas(leitor, fabrica); break;
                    case 4: comprarPvc(leitor, fabrica); break;
                    case 5: menuEstrategias(leitor, fabrica); break;
                    case 6: menuAuditoria(leitor, fabrica); break;
                    default: executando = false;
                }
            }
            System.out.println("Agulha levantada. Ate a proxima prensagem!");
        }
    }

    /** Monta a planta com PVC, linha de maquinas e os pedidos iniciais. */
    private static GerenciadorProducao montarFabrica(Cenario cenario) {
        MateriaPrima pvc = new MateriaPrima(1, "PVC Reciclado", 20.0, "kg", 3.0);
        GerenciadorProducao fabrica = new GerenciadorProducao(
                pvc, cenario, new EstrategiaFilaGravadoras());
        fabrica.adicionarMaquina(new PrensaHidraulica("Hydraulic Vinyl Press", 50, 0.20, 4.0));
        fabrica.adicionarMaquina(new EmbaladoraCapas("Sleeve-O-Matic", 80, 0.15, 1.5));
        fabrica.adicionarMaquina(new EstacaoInspecao("Golden Ear Station", 60, 0.05, 2.5));
        fabrica.registrarDemanda(LpAudiofiloDeluxe.TIPO, 2);
        fabrica.registrarDemanda(LpStandard.TIPO, 5);
        fabrica.registrarDemanda(CompactoSete.TIPO, 10);
        return fabrica;
    }

    private static void cabecalho(GerenciadorProducao fabrica, String titulo) {
        System.out.println("\n============================================================");
        System.out.println(" GROOVEPRESS | " + titulo);
        System.out.println(" Cenario: " + fabrica.getCenario().getDescricao());
        System.out.println(" Estrategia: " + fabrica.getNomeEstrategia());
        fabrica.exibirBudget();
        System.out.println("============================================================");
    }

    private static void menuDemandas(LeitorConsole leitor, GerenciadorProducao fabrica) {
        while (true) {
            cabecalho(fabrica, "PEDIDOS DAS GRAVADORAS");
            System.out.println("1 - Registrar/atualizar quantidade pendente");
            System.out.println("2 - Listar pedidos e estimativas");
            System.out.println("0 - Voltar");
            int opcao = leitor.lerOpcao(0, 2);
            if (opcao <= 0) { return; }
            if (opcao == 2) {
                fabrica.exibirDemandas();
            } else {
                final int tipo = escolherTipo(leitor);
                if (tipo <= 0) { continue; }
                System.out.println("Quantidade pendente (inteiro positivo; 0 para voltar):");
                final int quantidade = leitor.lerOpcao(0, Integer.MAX_VALUE);
                if (quantidade > 0) {
                    executarComTratamento(leitor, () -> fabrica.atualizarDemanda(TIPOS[tipo - 1], quantidade));
                }
            }
        }
    }

    private static void menuProducao(LeitorConsole leitor, GerenciadorProducao fabrica) {
        while (true) {
            cabecalho(fabrica, "SALA DE PRENSAGEM");
            System.out.println("1 - Proximo pedido pela estrategia ativa");
            System.out.println("2 - Fabricar um formato especifico");
            System.out.println("0 - Voltar");
            int opcao = leitor.lerOpcao(0, 2);
            if (opcao <= 0) { return; }
            if (opcao == 1) {
                executarComTratamento(leitor, fabrica::executarProximaProducao);
            } else {
                final int tipo = escolherTipo(leitor);
                if (tipo > 0) {
                    executarComTratamento(leitor, () -> fabrica.fabricarDemanda(TIPOS[tipo - 1]));
                }
            }
        }
    }

    private static void menuConsultas(LeitorConsole leitor, GerenciadorProducao fabrica) {
        while (true) {
            cabecalho(fabrica, "ESTOQUES");
            System.out.println("1 - Armazem de discos prontos");
            System.out.println("2 - Estoque de PVC");
            System.out.println("0 - Voltar");
            int opcao = leitor.lerOpcao(0, 2);
            if (opcao <= 0) { return; }
            if (opcao == 1) { fabrica.exibirArmazem(); }
            else { fabrica.exibirEstoque(); }
        }
    }

    private static void comprarPvc(LeitorConsole leitor, GerenciadorProducao fabrica) {
        System.out.println("Quantidade de PVC em kg (0 para voltar):");
        final double quantidade = leitor.lerQuantidade();
        if (quantidade > 0) {
            executarComTratamento(leitor, () -> fabrica.comprarMateriaPrima(quantidade));
        }
    }

    private static void menuEstrategias(LeitorConsole leitor, GerenciadorProducao fabrica) {
        cabecalho(fabrica, "ORDEM DA PRENSAGEM");
        System.out.println("1 - Fila das gravadoras: primeiro pedido pendente");
        System.out.println("2 - Grande tiragem: maior quantidade pendente");
        System.out.println("3 - Mais discos na loja: mais unidades com o budget atual");
        System.out.println("0 - Voltar");
        int opcao = leitor.lerOpcao(0, 3);
        switch (opcao) {
            case 1: fabrica.setEstrategia(new EstrategiaFilaGravadoras()); break;
            case 2: fabrica.setEstrategia(new EstrategiaGrandeTiragem()); break;
            case 3: fabrica.setEstrategia(new EstrategiaMaisDiscos()); break;
            default: return;
        }
        System.out.println("Estrategia ativa: " + fabrica.getNomeEstrategia());
    }

    private static void menuAuditoria(LeitorConsole leitor, GerenciadorProducao fabrica) {
        while (true) {
            cabecalho(fabrica, "AGULHA FINA");
            System.out.println("1 - Relatorio geral");
            System.out.println("2 - Detalhar maquinas");
            System.out.println("3 - Detalhar discos do armazem");
            System.out.println("0 - Voltar");
            int opcao = leitor.lerOpcao(0, 3);
            if (opcao <= 0) { return; }
            if (opcao == 1) { fabrica.gerarAuditoriaGeral(); }
            else if (opcao == 2) { fabrica.exibirMaquinas(); }
            else { fabrica.exibirArmazem(); }
        }
    }

    private static int escolherTipo(LeitorConsole leitor) {
        for (int i = 0; i < TIPOS.length; i++) {
            System.out.println((i + 1) + " - " + TIPOS[i]);
        }
        System.out.println("0 - Voltar");
        return leitor.lerOpcao(0, TIPOS.length);
    }

    // ---- Tratamento de erros ----------------------------------------------

    /**
     * Executa uma operacao da planta e trata o que der errado, para que o
     * usuario sempre volte ao menu.
     *
     * @param leitor   usado para pausar ate o usuario ler a mensagem de erro
     * @param operacao operacao a executar
     */
    private static void executarComTratamento(LeitorConsole leitor, Runnable operacao) {
        try {
            operacao.run();
        } catch (PlantaException e) {
            // Regra de negocio: o gerenciador nao alterou nada antes de lancar.
            mostrarCaixa("ERRO", e.getMessage(),
                    "Nenhuma alteracao foi realizada no estoque, no budget nem nos pedidos.");
            leitor.aguardarEnter();
        } catch (RuntimeException e) {
            // Rede de seguranca: so chega aqui um defeito do programa. Nao e
            // ocultado: o usuario e avisado e o detalhe vai para System.err.
            mostrarCaixa("FALHA INESPERADA",
                    "Ocorreu um problema interno (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ").",
                    "A planta foi mantida em estado consistente e voce pode continuar usando o sistema.");
            e.printStackTrace(System.err);
            leitor.aguardarEnter();
        }
    }

    /** Mostra uma mensagem de erro em caixa, no formato pedido no enunciado. */
    private static void mostrarCaixa(String titulo, String mensagem, String situacao) {
        System.out.println("\n========================================");
        System.out.println(titulo);
        System.out.println("========================================");
        System.out.println(quebrarLinhas(mensagem, LARGURA_MENSAGEM));
        System.out.println();
        System.out.println(quebrarLinhas(situacao, LARGURA_MENSAGEM));
        System.out.println();
    }

    /** Quebra o texto em linhas de no maximo {@code largura} caracteres, sem cortar palavras. */
    private static String quebrarLinhas(String texto, int largura) {
        StringBuilder resultado = new StringBuilder();
        int colunaAtual = 0;
        for (String palavra : texto.split(" ")) {
            if (colunaAtual > 0 && colunaAtual + 1 + palavra.length() > largura) {
                resultado.append('\n');
                colunaAtual = 0;
            } else if (colunaAtual > 0) {
                resultado.append(' ');
                colunaAtual++;
            }
            resultado.append(palavra);
            colunaAtual += palavra.length();
        }
        return resultado.toString();
    }
}
