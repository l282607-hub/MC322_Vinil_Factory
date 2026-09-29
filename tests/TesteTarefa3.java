import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Testes sem bibliotecas externas. Execute depois de compilar src. */
public class TesteTarefa3 {
    private static int verificacoes;

    public static void main(String[] args) throws Exception {
        testarDemandas();
        testarEstrategias();
        testarMaquinas();
        testarEstoque();
        testarGerenciador();
        testarSimulacoes();
        System.out.println("OK: " + verificacoes + " verificacoes.");
    }

    private static void conferir(boolean condicao, String mensagem) {
        verificacoes++;
        if (!condicao) { throw new AssertionError(mensagem); }
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
        boolean bloqueou = false;
        try { d.iniciarProducao(); } catch (IllegalStateException e) { bloqueou = true; }
        conferir(bloqueou, "Nao reiniciar concluida");
        d = pedido("Compacto", 2, 0.1, 8);
        d.cancelar();
        bloqueou = false;
        try { d.atender(2); } catch (IllegalStateException e) { bloqueou = true; }
        conferir(bloqueou && d.getStatus() == StatusDemanda.CANCELADA, "Nao concluir cancelada");
        conferir(d.calcularQuantidadeViavel(7.99) == 0, "Nao arredondar budget para cima");
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
                    for (int lote = 0; lote < 10; lote++) { g.executarProximaProducao(); }
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
            GerenciadorProducao g = new GerenciadorProducao(pvc, 24);
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
            g.comprarMateriaPrima(Double.NaN);
            g.comprarMateriaPrima(1);
            conferir(g.getBudget() == 0, "Compra invalida ou sem budget nao debita");
            g.registrarDemanda(CompactoSete.TIPO, 1);
            g.executarProximaProducao();
            g.exibirDemandas();
            conferir(saida.toString("UTF-8").contains("Cancelada"), "Cancelamento sem budget");

            pvc = new MateriaPrima(1, "PVC", 1, "kg", 3);
            g = new GerenciadorProducao(pvc, 16);
            g.adicionarMaquina(new MaquinaTeste(false));
            g.registrarDemanda(CompactoSete.TIPO, 2);
            g.fabricarDemanda(CompactoSete.TIPO);
            conferir(Math.abs(pvc.getQuantidade() - 0.9) < 1e-9, "Reciclagem de 50%");
            conferir(g.getBudget() == 0, "Rejeitados tambem custam");
            saida.reset();
            g.exibirArmazem();
            conferir(saida.toString("UTF-8").contains("Total no armazem: 0"), "Rejeitados fora");

            pvc = new MateriaPrima(1, "PVC", 0, "kg", 3);
            g = new GerenciadorProducao(pvc, 100);
            g.adicionarMaquina(new MaquinaTeste(true));
            g.registrarDemanda(CompactoSete.TIPO, 1);
            g.fabricarDemanda(CompactoSete.TIPO);
            conferir(g.getBudget() == 100, "Sem PVC nao gasta");
            g.comprarMateriaPrima(1);
            conferir(g.getBudget() == 97 && pvc.getQuantidade() == 1, "Compra repoe PVC");
            g.registrarDemanda(CompactoSete.TIPO, 1);
            g.fabricarDemanda(CompactoSete.TIPO);
            conferir(g.getBudget() == 89, "Novo pedido apos cancelamento");
        } finally {
            System.setOut(console);
        }
    }
}
