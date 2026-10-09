import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * Conferencia automatizada da planta, sem bibliotecas externas.
 *
 * <p>Cobre as regras das Tarefas 2 e 3 (pedidos, estrategias, maquinas,
 * estoque, simulacoes) e o tratamento de erros da Tarefa 4: excecoes de
 * dominio, atomicidade (um erro nao altera o estado), validacao de entradas e
 * continuidade do programa apos um erro. Executada por
 * {@code ./gradlew verificar}, que tambem faz parte de {@code ./gradlew build}.</p>
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 */
public class TestePlanta {
    private static int verificacoes;

    /**
     * Executa todos os grupos de teste; lanca {@link AssertionError} na
     * primeira falha, o que faz a tarefa do Gradle falhar.
     *
     * @param args ignorados
     * @throws Exception se a captura da saida falhar
     */
    public static void main(String[] args) throws Exception {
        testarDemandas();
        testarEstrategias();
        testarMaquinas();
        testarEstoque();
        testarGerenciador();
        testarSimulacoes();
        testarExcecoesDeDominio();
        testarAtomicidade();
        testarLeitorConsole();
        testarMenuContinuaAposErro();
        System.out.println("OK: " + verificacoes + " verificacoes.");
    }

    private static void conferir(boolean condicao, String mensagem) {
        verificacoes++;
        if (!condicao) { throw new AssertionError(mensagem); }
    }

    /** Confere que a operacao lanca a excecao esperada e devolve a excecao. */
    private static <T extends RuntimeException> T esperar(Class<T> tipo, Runnable operacao, String mensagem) {
        verificacoes++;
        try {
            operacao.run();
        } catch (RuntimeException e) {
            if (tipo.isInstance(e)) { return tipo.cast(e); }
            throw new AssertionError(mensagem + " (lancou " + e.getClass().getSimpleName() + ")", e);
        }
        throw new AssertionError(mensagem + " (nao lancou excecao)");
    }

    private static Demanda pedido(String tipo, int quantidade, double pvc, double custo) {
        Demanda demanda = new Demanda(tipo, quantidade);
        demanda.configurarEstimativas(pvc, custo);
        return demanda;
    }

    private static void testarDemandas() {
        Demanda d = pedido("LP", 3, 0.2, 8);
        conferir(d.getStatus() == StatusDemanda.PENDENTE, "Novo pedido pendente");
        conferir(Math.abs(d.calcularConsumoEstimado() - 0.6) < 1e-9, "Consumo estimado");
        conferir(d.calcularCustoEstimado() == 24, "Custo estimado");
        conferir(d.ehFinanceiramenteViavel(24), "Viabilidade integral");
        conferir(!d.ehFinanceiramenteViavel(23), "Orcamento insuficiente");
        d.iniciarProducao();
        conferir(d.getStatus() == StatusDemanda.EM_PRODUCAO, "Inicio");
        d.atender(1);
        conferir(d.estaPendente() && d.getQuantidadeProdutos() == 2, "Lote parcial");
        d.iniciarProducao();
        d.atender(2);
        conferir(d.isAtendida(), "Conclusao");
        final Demanda concluida = d;
        esperar(DemandaInvalidaException.class, concluida::iniciarProducao, "Nao reiniciar concluida");
        d = pedido("Compacto", 2, 0.1, 8);
        d.cancelar();
        final Demanda cancelada = d;
        esperar(DemandaInvalidaException.class, () -> cancelada.atender(2), "Nao concluir cancelada");
        conferir(d.getStatus() == StatusDemanda.CANCELADA, "Cancelada continua cancelada");
        conferir(d.calcularQuantidadeViavel(7.99) == 0, "Nao arredondar budget para cima");
        esperar(DemandaInvalidaException.class, () -> new Demanda("LP", 0), "Pedido sem quantidade");
        esperar(DemandaInvalidaException.class, () -> new Demanda(null, 1), "Pedido sem tipo");
    }

    private static void testarEstrategias() {
        Demanda a = pedido("Deluxe", 2, 0.4, 8);
        Demanda b = pedido("Standard", 5, 0.2, 8);
        Demanda c = pedido("Compacto", 10, 0.1, 8);
        List<Demanda> pedidos = Arrays.asList(a, b, c);
        EstrategiaProducao fila = new EstrategiaFilaGravadoras();
        EstrategiaProducao maior = new EstrategiaGrandeTiragem();
        EstrategiaProducao maximo = new EstrategiaMaisDiscos();
        conferir(fila.selecionarDemanda(pedidos, 16) == a, "FIFO");
        conferir(maior.selecionarDemanda(pedidos, 16) == c, "Maior tiragem");
        conferir(maximo.selecionarDemanda(pedidos, 16) == c, "Empate economiza PVC");
        conferir(maximo.selecionarDemanda(pedidos, 7) == null, "Sem budget");
        Demanda cara = pedido("Cara", 100, 0.1, 20);
        Demanda barata = pedido("Barata", 6, 0.2, 2);
        conferir(maximo.selecionarDemanda(Arrays.asList(cara, barata), 20) == barata,
                "Maximiza unidades financiaveis, nao tamanho nominal");
        a.cancelar();
        conferir(fila.selecionarDemanda(pedidos, 100) == b, "Ignora cancelada");
        b.iniciarProducao();
        conferir(fila.selecionarDemanda(pedidos, 100) == c, "Ignora em producao");
        c.iniciarProducao();
        c.atender(10);
        conferir(maior.selecionarDemanda(pedidos, 100) == null, "Ignora concluida");
        conferir(fila.selecionarDemanda(Collections.emptyList(), 100) == null, "Fila vazia");
        conferir(maximo.selecionarDemanda(Collections.emptyList(), 100) == null, "Maximo vazio");
    }

    private static void testarMaquinas() {
        Maquina ideal = new PrensaHidraulica("Teste", 50, 0.2, 4);
        Maquina caos = new PrensaHidraulica("Teste", 50, 0.2, 4);
        caos.configurarCenario(Cenario.APOCALIPTICO);
        conferir(ideal.getProbabilidadeFalha() < caos.getProbabilidadeFalha(), "Cenarios distintos");
        Produto disco = new CompactoSete();
        conferir(!ideal.processar(disco) && ideal.getSaude() == 100, "Desligada nao desgasta");
        Maquina.definirSemente(322);
        ideal.ligar();
        ideal.processar(disco);
        conferir(ideal.getSaude() < 100 && ideal.getSaude() > 99.5, "Desgaste ideal");
        conferir(ideal.getProbabilidadeFalha() > 0.02, "Falha cresce com desgaste");
        caos.ligar();
        PrintStream console = System.out;
        try {
            System.setOut(new PrintStream(new ByteArrayOutputStream()));
            for (int i = 0; i < 100 && caos.estaLigada(); i++) { caos.processar(disco); }
        } finally { System.setOut(console); }
        conferir(caos.getStatus() == StatusMaquina.QUEBRADA && caos.getSaude() == 0, "Quebra");
        conferir(caos.precisaManutencao(), "Manutencao");
        caos.ligar();
        conferir(!caos.estaLigada() && !caos.processar(disco), "Quebrada nao opera");
        ideal.setLimiarManutencao(100);
        conferir(ideal.precisaManutencao(), "Limiar configuravel");
        disco.aumentarProbabilidadeFalha(1);
        conferir(disco.precisaManutencao(), "Auditoria do risco");
        disco.setLote(1);
        conferir(disco.gerarRelatorioDiagnostico().contains("lote 001"), "Lote no diagnostico");
    }

    private static void testarEstoque() {
        MateriaPrima pvc = new MateriaPrima(1, "PVC", 0.3, "kg", 3);
        conferir(pvc.consumir(0.1) && pvc.consumir(0.2), "Tolerancia decimal do estoque");
        conferir(pvc.getQuantidade() == 0, "Sem estoque negativo");
        conferir(!pvc.consumir(Double.NaN) && !pvc.consumir(-1), "Consumo invalido");
        pvc.adicionarEstoque(Double.POSITIVE_INFINITY);
        conferir(pvc.getQuantidade() == 0, "Estoque finito");
    }

    /** Linha controlada isola a contabilidade das falhas aleatorias. */
    private static class MaquinaTeste extends Maquina {
        private final boolean aprova;
        MaquinaTeste(boolean aprova) { super("Linha de teste", 2, 0, 8); this.aprova = aprova; }
        public String getTipo() { return "Teste"; }
        public boolean processar(Produto produto) {
            if (!estaLigada()) { return false; }
            produto.setStatus(aprova ? StatusProduto.APROVADO : StatusProduto.REJEITADO);
            registrarUso();
            return aprova;
        }
    }

    private static void testarSimulacoes() throws Exception {
        PrintStream console = System.out;
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(saida, true, "UTF-8"));
            for (Cenario cenario : Cenario.values()) {
                for (int semente = 0; semente < 100; semente++) {
                    Maquina.definirSemente(semente);
                    MateriaPrima pvc = new MateriaPrima(1, "PVC", 20, "kg", 3);
                    GerenciadorProducao g = new GerenciadorProducao(pvc, cenario,
                            new EstrategiaMaisDiscos());
                    Maquina prensa = new PrensaHidraulica("Prensa", 50, 0.2, 4);
                    Maquina embalagem = new EmbaladoraCapas("Embalagem", 80, 0.15, 1.5);
                    Maquina inspecao = new EstacaoInspecao("Inspecao", 60, 0.05, 2.5);
                    List<Maquina> linha = Arrays.asList(prensa, embalagem, inspecao);
                    for (Maquina maquina : linha) { g.adicionarMaquina(maquina); }
                    g.registrarDemanda(CompactoSete.TIPO, 100);
                    int inicio = Produto.getTotalProdutosFabricados();
                    for (int lote = 0; lote < 10; lote++) {
                        try {
                            g.executarProximaProducao();
                        } catch (PlantaException e) {
                            // Linha quebrada ou recursos esgotados: esperado no cenario apocaliptico.
                            conferir(e.getMessage() != null && !e.getMessage().isEmpty(), "Mensagem de erro");
                        }
                    }
                    int tentativas = Produto.getTotalProdutosFabricados() - inicio;
                    conferir(Math.abs(cenario.getBudgetInicial() - g.getBudget() - 8 * tentativas) < 1e-8,
                            "Contabilidade por tentativa, cenario " + cenario + ", semente " + semente);
                    // Com consumo de 0,1 kg e reciclagem de 50%, a perda de PVC
                    // permite obter independentemente o numero de aprovados.
                    double aprovadosCalculados = (20 - pvc.getQuantidade()) / 0.05 - tentativas;
                    int aprovados = (int) Math.round(aprovadosCalculados);
                    conferir(Math.abs(aprovadosCalculados - aprovados) < 1e-7
                                    && aprovados >= 0 && aprovados <= 100,
                            "Conservacao de materia-prima");
                    conferir(g.getBudget() >= 0 && pvc.getQuantidade() >= 0, "Recursos nao negativos");
                    saida.reset();
                    g.exibirArmazem();
                    conferir(saida.toString("UTF-8").contains("Total no armazem: " + aprovados + " |"),
                            "Armazem coincide com consumo e reciclagem");
                    for (Maquina maquina : linha) {
                        conferir(maquina.getSaude() >= 0 && maquina.getSaude() <= 100
                                        && !maquina.estaLigada(),
                                "Saude limitada e maquina desligada ao terminar");
                        conferir(maquina.getProbabilidadeFalha() >= 0
                                        && maquina.getProbabilidadeFalha() <= 1,
                                "Probabilidade limitada");
                    }
                    saida.reset();
                }
            }
        } finally {
            System.setOut(console);
        }
    }

    private static void testarGerenciador() throws Exception {
        PrintStream console = System.out;
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(saida, true, "UTF-8"));
            MateriaPrima pvc = new MateriaPrima(1, "PVC", 1, "kg", 3);
            final GerenciadorProducao g = new GerenciadorProducao(pvc, 24);
            g.adicionarMaquina(new MaquinaTeste(true));
            g.setEstrategia(new EstrategiaFilaGravadoras());
            g.registrarDemanda(CompactoSete.TIPO, 3);
            g.executarProximaProducao();
            conferir(g.getBudget() == 8 && Math.abs(pvc.getQuantidade() - 0.8) < 1e-9,
                    "Capacidade limita lote e debitos");
            g.executarProximaProducao();
            conferir(g.getBudget() == 0 && Math.abs(pvc.getQuantidade() - 0.7) < 1e-9,
                    "Conclui lote restante");
            g.exibirDemandas();
            g.exibirArmazem();
            g.gerarAuditoriaGeral();
            String texto = saida.toString("UTF-8");
            conferir(texto.contains("Concluida"), "Pedido concluido no relatorio");
            conferir(texto.contains("Total no armazem: 3"), "Somente aprovados no armazem");
            conferir(texto.contains("lote 002"), "Lotes distintos");
            conferir(texto.contains("Componentes: 4"), "Auditoria inclui maquina e produtos");
            esperar(EntradaInvalidaException.class, () -> g.comprarMateriaPrima(Double.NaN), "Compra NaN");
            esperar(RecursoInsuficienteException.class, () -> g.comprarMateriaPrima(1), "Compra sem budget");
            conferir(g.getBudget() == 0, "Compra invalida ou sem budget nao debita");

            // Sem budget: pela fila o erro e informado e o pedido continua pendente.
            g.registrarDemanda(CompactoSete.TIPO, 1);
            esperar(RecursoInsuficienteException.class, g::executarProximaProducao, "Producao sem budget");
            saida.reset();
            g.exibirDemandas();
            conferir(saida.toString("UTF-8").contains("Pendente"), "Pedido segue pendente apos erro");
            // Pela estrategia financeira, nenhum pedido e elegivel: vale o cancelamento da Tarefa 3.
            g.setEstrategia(new EstrategiaMaisDiscos());
            g.executarProximaProducao();
            saida.reset();
            g.exibirDemandas();
            conferir(saida.toString("UTF-8").contains("Cancelada"), "Cancelamento sem budget");

            pvc = new MateriaPrima(1, "PVC", 1, "kg", 3);
            final GerenciadorProducao rejeita = new GerenciadorProducao(pvc, 16);
            rejeita.adicionarMaquina(new MaquinaTeste(false));
            rejeita.registrarDemanda(CompactoSete.TIPO, 2);
            rejeita.fabricarDemanda(CompactoSete.TIPO);
            conferir(Math.abs(pvc.getQuantidade() - 0.9) < 1e-9, "Reciclagem de 50%");
            conferir(rejeita.getBudget() == 0, "Rejeitados tambem custam");
            saida.reset();
            rejeita.exibirArmazem();
            conferir(saida.toString("UTF-8").contains("Total no armazem: 0"), "Rejeitados fora");

            pvc = new MateriaPrima(1, "PVC", 0, "kg", 3);
            final GerenciadorProducao semPvc = new GerenciadorProducao(pvc, 100);
            semPvc.adicionarMaquina(new MaquinaTeste(true));
            semPvc.registrarDemanda(CompactoSete.TIPO, 1);
            esperar(RecursoInsuficienteException.class, () -> semPvc.fabricarDemanda(CompactoSete.TIPO),
                    "Producao sem PVC");
            conferir(semPvc.getBudget() == 100, "Sem PVC nao gasta");
            semPvc.comprarMateriaPrima(1);
            conferir(semPvc.getBudget() == 97 && pvc.getQuantidade() == 1, "Compra repoe PVC");
            semPvc.registrarDemanda(CompactoSete.TIPO, 1);
            semPvc.fabricarDemanda(CompactoSete.TIPO);
            conferir(semPvc.getBudget() == 89, "Pedido pendente produzido apos a compra");
        } finally {
            System.setOut(console);
        }
    }

    // ---- Tarefa 4: excecoes de dominio e atomicidade ----------------------

    private static void testarExcecoesDeDominio() {
        conferir(new DemandaInvalidaException("x") instanceof PlantaException, "Hierarquia: demanda");
        conferir(new MaquinaIndisponivelException("x") instanceof PlantaException, "Hierarquia: maquina");
        conferir(new EntradaInvalidaException("x") instanceof PlantaException, "Hierarquia: entrada");
        RecursoInsuficienteException recurso = new RecursoInsuficienteException("m", "PVC", 2.0, 0.5);
        conferir(recurso instanceof PlantaException && recurso instanceof RuntimeException,
                "Hierarquia: recurso");
        conferir(recurso.getRecurso().equals("PVC") && recurso.getNecessario() == 2.0
                && recurso.getDisponivel() == 0.5 && recurso.getMessage().equals("m"), "Dados do recurso");
    }

    private static GerenciadorProducao planta(double pvcInicial, double budget, Maquina... linha) {
        GerenciadorProducao g = new GerenciadorProducao(
                new MateriaPrima(1, "PVC", pvcInicial, "kg", 3), budget);
        for (Maquina maquina : linha) { g.adicionarMaquina(maquina); }
        return g;
    }

    /** Resumo textual do estado visivel da planta, para comparar antes e depois de um erro. */
    private static String instantaneo(GerenciadorProducao g) throws Exception {
        PrintStream console = System.out;
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(saida, true, "UTF-8"));
            g.exibirBudget();
            g.exibirEstoque();
            g.exibirDemandas();
            g.exibirArmazem();
            g.exibirMaquinas();
        } finally {
            System.setOut(console);
        }
        return saida.toString("UTF-8");
    }

    private static void testarAtomicidade() throws Exception {
        PrintStream console = System.out;
        try {
            System.setOut(new PrintStream(new ByteArrayOutputStream(), true, "UTF-8"));

            GerenciadorProducao g = planta(20, 100, new MaquinaTeste(true));
            g.registrarDemanda(LpStandard.TIPO, 2);
            String antes = instantaneo(g);
            final GerenciadorProducao alvo = g;
            esperar(DemandaInvalidaException.class, () -> alvo.fabricarDemanda(CompactoSete.TIPO),
                    "Produzir pedido inexistente");
            esperar(DemandaInvalidaException.class, () -> alvo.fabricarDemanda("Disco de ouro"),
                    "Produzir tipo desconhecido");
            esperar(DemandaInvalidaException.class, () -> alvo.registrarDemanda("Disco de ouro", 1),
                    "Registrar tipo desconhecido");
            esperar(DemandaInvalidaException.class, () -> alvo.registrarDemanda(LpStandard.TIPO, 0),
                    "Registrar quantidade zero");
            esperar(DemandaInvalidaException.class, () -> alvo.atualizarDemanda(LpStandard.TIPO, -3),
                    "Atualizar quantidade negativa");
            esperar(EntradaInvalidaException.class, () -> alvo.comprarMateriaPrima(-1), "Comprar negativo");
            esperar(EntradaInvalidaException.class,
                    () -> alvo.comprarMateriaPrima(Double.POSITIVE_INFINITY), "Comprar infinito");
            esperar(RecursoInsuficienteException.class, () -> alvo.comprarMateriaPrima(1000),
                    "Comprar alem do budget");
            conferir(antes.equals(instantaneo(g)), "Erros nao alteram o estado da planta");

            // Operacao incompativel com o estado: pedido ja concluido.
            g.fabricarDemanda(LpStandard.TIPO);
            DemandaInvalidaException concluido = esperar(DemandaInvalidaException.class,
                    () -> alvo.fabricarDemanda(LpStandard.TIPO), "Produzir pedido concluido");
            conferir(concluido.getMessage().contains("Concluida"), "Mensagem cita o estado do pedido");
            esperar(DemandaInvalidaException.class, alvo::executarProximaProducao,
                    "Proxima producao sem pedidos pendentes");

            // Falta de PVC e de budget: informa o recurso e nao muda nada.
            GerenciadorProducao semPvc = planta(0.05, 100, new MaquinaTeste(true));
            semPvc.registrarDemanda(CompactoSete.TIPO, 1);
            String antesPvc = instantaneo(semPvc);
            RecursoInsuficienteException faltaPvc = esperar(RecursoInsuficienteException.class,
                    () -> semPvc.fabricarDemanda(CompactoSete.TIPO), "Falta de PVC");
            conferir(faltaPvc.getRecurso().equals("PVC") && faltaPvc.getNecessario() == 0.1
                    && faltaPvc.getDisponivel() == 0.05, "Dados da falta de PVC");
            conferir(antesPvc.equals(instantaneo(semPvc)), "Falta de PVC nao altera o estado");

            GerenciadorProducao semBudget = planta(20, 5, new MaquinaTeste(true));
            semBudget.registrarDemanda(CompactoSete.TIPO, 1);
            String antesBudget = instantaneo(semBudget);
            RecursoInsuficienteException faltaBudget = esperar(RecursoInsuficienteException.class,
                    () -> semBudget.fabricarDemanda(CompactoSete.TIPO), "Falta de budget");
            conferir(faltaBudget.getRecurso().equals("budget") && faltaBudget.getNecessario() == 8
                    && faltaBudget.getDisponivel() == 5, "Dados da falta de budget");
            conferir(antesBudget.equals(instantaneo(semBudget)), "Falta de budget nao altera o estado");

            // Linha sem maquinas ou com maquina quebrada.
            GerenciadorProducao vazia = planta(20, 100);
            esperar(MaquinaIndisponivelException.class, () -> vazia.registrarDemanda(CompactoSete.TIPO, 1),
                    "Pedido sem linha");
            esperar(MaquinaIndisponivelException.class, vazia::executarProximaProducao, "Producao sem linha");
        } finally {
            System.setOut(console);
        }
        verificarLinhaQuebrada();
    }

    /** Uma maquina quebrada impede producao sem consumir recursos. */
    private static void verificarLinhaQuebrada() throws Exception {
        PrintStream console = System.out;
        try {
            System.setOut(new PrintStream(new ByteArrayOutputStream(), true, "UTF-8"));
            Maquina.definirSemente(1);
            Maquina quebrada = new PrensaHidraulica("Prensa gasta", 50, 0.2, 4);
            quebrada.configurarCenario(Cenario.APOCALIPTICO);
            quebrada.ligar();
            Produto cobaia = new CompactoSete();
            for (int i = 0; i < 100 && quebrada.estaLigada(); i++) { quebrada.processar(cobaia); }
            final GerenciadorProducao g = new GerenciadorProducao(
                    new MateriaPrima(1, "PVC", 20, "kg", 3), 100);
            g.adicionarMaquina(new MaquinaTeste(true));
            g.registrarDemanda(CompactoSete.TIPO, 1);
            g.adicionarMaquina(quebrada);
            String antes = instantaneo(g);
            MaquinaIndisponivelException e = esperar(MaquinaIndisponivelException.class,
                    () -> g.fabricarDemanda(CompactoSete.TIPO), "Linha com maquina quebrada");
            conferir(e.getMessage().contains("Prensa gasta"), "Mensagem cita a maquina quebrada");
            conferir(antes.equals(instantaneo(g)), "Linha parada nao altera o estado");
        } finally {
            System.setOut(console);
        }
    }

    // ---- Tarefa 4: entrada do usuario -------------------------------------

    private static void testarLeitorConsole() throws Exception {
        PrintStream console = System.out;
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(saida, true, "UTF-8"));
            LeitorConsole opcoes = new LeitorConsole(new Scanner("abc\n9\n\n-1\n99999999999\n3\n"));
            conferir(opcoes.lerOpcao(0, 6) == 3, "Repete ate receber opcao valida");
            String texto = saida.toString("UTF-8");
            conferir(texto.contains("'abc' nao e um numero inteiro"), "Mensagem para texto");
            conferir(texto.contains("Opcao inexistente: 9"), "Mensagem para opcao inexistente");
            conferir(texto.contains("Entrada vazia"), "Mensagem para linha vazia");
            conferir(texto.contains("Opcao inexistente: -1"), "Mensagem para opcao negativa");
            conferir(texto.contains("'99999999999' nao e um numero inteiro"), "Mensagem para numero enorme");

            saida.reset();
            LeitorConsole quantidades = new LeitorConsole(new Scanner("x\n-1\nNaN\nInfinity\n\n1,5\n"));
            conferir(quantidades.lerQuantidade() == 1.5, "Aceita virgula e rejeita invalidas");
            texto = saida.toString("UTF-8");
            conferir(texto.contains("'x' nao e um numero") && texto.contains("finita"),
                    "Mensagens da quantidade");

            LeitorConsole ponto = new LeitorConsole(new Scanner("2.25\n"));
            conferir(ponto.lerQuantidade() == 2.25, "Aceita ponto");

            LeitorConsole acabou = new LeitorConsole(new Scanner(""));
            conferir(acabou.lerOpcao(0, 1) == LeitorConsole.FIM_DA_ENTRADA, "Fim da entrada (opcao)");
            conferir(acabou.lerQuantidade() == LeitorConsole.FIM_DA_ENTRADA, "Fim da entrada (quantidade)");
            acabou.aguardarEnter();
            conferir(true, "ENTER sem entrada nao trava");
        } finally {
            System.setOut(console);
        }
    }

    /** Simula uma sessao completa: um erro no meio nao encerra o programa. */
    private static void testarMenuContinuaAposErro() throws Exception {
        PrintStream console = System.out;
        InputStream entrada = System.in;
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(saida, true, "UTF-8"));
            // cenario ideal; menu 4 (comprar PVC) com valor que estoura o budget;
            // ENTER; opcao inexistente 9; menu 2 > 2 > formato 1 (producao valida); sair.
            String sessao = "1\n4\n99999\n\n9\n2\n2\n1\n0\n0\n";
            System.setIn(new ByteArrayInputStream(sessao.getBytes("UTF-8")));
            Main.main(new String[] {"322"});
            String texto = saida.toString("UTF-8");
            conferir(texto.contains("ERRO"), "Caixa de erro exibida");
            conferir(texto.contains("budget disponivel e R$10000.00"), "Erro de budget explicado");
            conferir(texto.contains("Nenhuma alteracao foi realizada"), "Informa que nada mudou");
            conferir(texto.contains("Pressione ENTER para continuar"), "Pausa para leitura");
            conferir(texto.contains("Opcao inexistente: 9"), "Opcao inexistente tratada no menu");
            conferir(texto.contains(">>> Lote 001"), "Producao funciona depois do erro");
            conferir(texto.contains("Agulha levantada"), "Programa encerra normalmente");
            conferir(texto.indexOf("ERRO") < texto.indexOf(">>> Lote 001"), "Erro veio antes da producao");
        } finally {
            System.setOut(console);
            System.setIn(entrada);
        }
    }
}
