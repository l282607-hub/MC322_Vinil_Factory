import java.util.ArrayList;

/**
 * Coordena a planta da GroovePress: demandas, linha de maquinas, estoque de
 * PVC, armazem de discos prontos e o budget alocado pela diretoria.
 */
public class GerenciadorProducao {
    /** Fracao do PVC de um disco rejeitado que volta ao estoque apos trituracao. */
    private static final double TAXA_RECICLAGEM = 0.5;

    private ArrayList<Demanda> demandas;
    private ArrayList<Produto> produtosFabricados;
    private ArrayList<Maquina> maquinas;
    private MateriaPrima materiaPrima;
    private double budget;
    private Cenario cenario = Cenario.IDEAL;
    private EstrategiaProducao estrategiaAtual;
    private int ultimoLote;

    public GerenciadorProducao(MateriaPrima materiaPrima, double budget) {
        if (materiaPrima == null || !Double.isFinite(budget) || budget < 0) {
            throw new IllegalArgumentException("Estoque obrigatorio e budget nao negativo.");
        }
        this.demandas = new ArrayList<>();
        this.produtosFabricados = new ArrayList<>();
        this.maquinas = new ArrayList<>();
        this.materiaPrima = materiaPrima;
        this.budget = budget;
    }

    public GerenciadorProducao(MateriaPrima materiaPrima, Cenario cenario,
            EstrategiaProducao estrategia) {
        this(materiaPrima, cenario.getBudgetInicial());
        this.cenario = cenario;
        setEstrategia(estrategia);
    }

    public final void setEstrategia(EstrategiaProducao novaEstrategia) {
        if (novaEstrategia == null) {
            throw new IllegalArgumentException("Estrategia obrigatoria.");
        }
        estrategiaAtual = novaEstrategia;
    }

    public String getNomeEstrategia() {
        return estrategiaAtual == null ? "Nao definida" : estrategiaAtual.getNomeEstrategia();
    }

    public Cenario getCenario() { return cenario; }

    public void executarProximaProducao() {
        if (estrategiaAtual == null || maquinas.isEmpty()) {
            System.out.println("[ERRO] Configure a estrategia e a linha primeiro.");
            return;
        }
        Demanda proxima = estrategiaAtual.selecionarDemanda(demandas, budget);
        if (proxima == null) {
            System.out.println("Nenhum pedido elegivel para a estrategia e o budget atuais.");
            cancelarDemandasSemRecursos();
            return;
        }
        System.out.println("Pedido selecionado: " + proxima.getTipoProduto());
        fabricarDemanda(proxima);
    }

    // ---- Configuracao da planta -------------------------------------------

    public void adicionarMaquina(Maquina maquina) {
        if (maquina == null) {
            throw new IllegalArgumentException("Maquina obrigatoria.");
        }
        maquina.configurarCenario(cenario);
        maquinas.add(maquina);
        for (Demanda demanda : demandas) {
            atualizarEstimativas(demanda);
        }
    }

    // ---- Demandas ----------------------------------------------------------

    public void registrarDemanda(String tipoProduto, int quantidade) {
        if (quantidade <= 0 || pvcPorUnidade(tipoProduto) == 0 || maquinas.isEmpty()) {
            System.out.println("[ERRO] Escolha um tipo valido, quantidade positiva e configure a linha.");
            return;
        }
        Demanda existente = buscarDemanda(tipoProduto);
        if (existente == null) {
            Demanda nova = new Demanda(tipoProduto, quantidade);
            atualizarEstimativas(nova);
            demandas.add(nova);
        } else {
            existente.atualizarQuantidade(quantidade);
        }
        System.out.printf("[OK] Pedido de %s: %d disco(s) pendente(s).%n", tipoProduto, quantidade);
    }

    public void atualizarDemanda(String tipoProduto, int novaQuantidade) {
        registrarDemanda(tipoProduto, novaQuantidade);
    }

    public void exibirDemandas() {
        System.out.println("\n--- DEMANDAS DAS GRAVADORAS ---");
        if (demandas.isEmpty()) {
            System.out.println("Nenhuma demanda registrada.");
            return;
        }
        for (Demanda d : demandas) {
            System.out.println("  " + d);
        }
    }

    // ---- Fabricacao --------------------------------------------------------

    public void fabricarDemanda(String tipoProduto) {
        Demanda demanda = buscarDemanda(tipoProduto);
        if (demanda == null) {
            System.out.println("[ERRO] Nao ha demanda em aberto para " + tipoProduto
                    + ". Atualize a demanda antes de fabricar.");
            return;
        }
        fabricarDemanda(demanda);
    }

    /** Producao manual e automatica compartilham o mesmo fluxo. */
    private void fabricarDemanda(Demanda demanda) {
        String tipoProduto = demanda.getTipoProduto();
        if (maquinas.isEmpty()) {
            System.out.println("[ERRO] Nenhuma maquina instalada na planta.");
            return;
        }
        if (!linhaDisponivel()) {
            System.out.println("[ERRO] Linha parada: ha maquina quebrada. Consulte a auditoria.");
            return;
        }

        int quantidade = calcularLoteViavel(demanda);
        if (quantidade == 0) {
            demanda.cancelar();
            System.out.println("Pedido cancelado por falta de budget ou PVC.");
            return;
        }

        demanda.iniciarProducao();
        int lote = ++ultimoLote;
        System.out.printf("%n>>> Lote %03d: %d x %s%n", lote, quantidade, tipoProduto);
        ligarMaquinas();

        int aprovados = 0;
        int rejeitados = 0;
        double pvcReciclado = 0.0;
        double tempoTotal = 0.0;
        double budgetInicial = budget;

        for (int i = 0; i < quantidade; i++) {
            if (!linhaDisponivel()) {
                System.out.println("[AVISO] Lote interrompido: uma maquina quebrou no ciclo anterior.");
                break;
            }
            Produto disco = criarProduto(tipoProduto);
            if (disco == null) {
                break;
            }
            if (!materiaPrima.consumir(disco.getQuantidadeMateriaPrimaPorUnidade())) {
                System.out.println("[ERRO] PVC acabou no meio do lote.");
                break;
            }
            disco.setLote(lote);

            System.out.printf("  Disco #%03d entra na linha%n", disco.getId());
            boolean aprovado = passarPelaLinha(disco);
            if (!aprovado && disco.getStatus() != StatusProduto.REJEITADO) {
                // Uma operacao interrompida tambem devolve parte do PVC.
                System.out.println("[ERRO] Operacao interrompida. Disco inacabado enviado para reciclagem.");
                disco.setStatus(StatusProduto.REJEITADO);
                rejeitados++;
                pvcReciclado += reciclar(disco);
                break;
            }

            tempoTotal += disco.calcularTempoProducao();
            if (aprovado) {
                produtosFabricados.add(disco);
                aprovados++;
                System.out.printf("  Disco #%03d aprovado -> armazem%n", disco.getId());
            } else {
                rejeitados++;
                pvcReciclado += reciclar(disco);
                System.out.printf("  Disco #%03d rejeitado -> triturado e reciclado%n", disco.getId());
            }
        }

        desligarMaquinas();
        demanda.atender(aprovados);
        cancelarDemandasSemRecursos();

        System.out.println("\n--- RESUMO DO LOTE ---");
        System.out.printf("  Aprovados: %d | Rejeitados: %d%n", aprovados, rejeitados);
        System.out.printf("  PVC reciclado: %.2f kg%n", pvcReciclado);
        System.out.printf("  Tempo de producao: %.1f min%n", tempoTotal);
        System.out.printf("  Custo de operacao: R$%.2f%n", budgetInicial - budget);
        System.out.printf("  Demanda restante de %s: %d%n", tipoProduto, demanda.getQuantidadeProdutos());
        exibirBudget();
    }

    // ---- Compras -----------------------------------------------------------

    public void comprarMateriaPrima(double quantidade) {
        if (!Double.isFinite(quantidade) || quantidade <= 0) {
            System.out.println("[ERRO] Quantidade invalida.");
            return;
        }
        double custo = quantidade * materiaPrima.getCustoPorUnidade();
        if (!Double.isFinite(custo) || custo > budget) {
            System.out.printf("[ERRO] Compra de %.2f %s custa R$%.2f; budget disponivel: R$%.2f.%n",
                    quantidade, materiaPrima.getUnidade(), custo, budget);
            return;
        }
        budget -= custo;
        materiaPrima.adicionarEstoque(quantidade);
        System.out.printf("[OK] Comprados %.2f %s de %s por R$%.2f.%n",
                quantidade, materiaPrima.getUnidade(), materiaPrima.getNome(), custo);
        exibirEstoque();
        exibirBudget();
    }

    // ---- Consultas ---------------------------------------------------------

    public void exibirBudget() {
        System.out.printf("BUDGET ATUAL: R$%.2f%n", budget);
    }

    public void exibirEstoque() {
        System.out.println("Estoque de " + materiaPrima);
    }

    public void exibirArmazem() {
        System.out.println("\n--- ARMAZEM DE DISCOS PRONTOS ---");
        if (produtosFabricados.isEmpty()) {
            System.out.println("Armazem vazio.");
        } else {
            for (Produto p : produtosFabricados) {
                System.out.println("  " + p.gerarRelatorioDiagnostico());
            }
            System.out.println("  Por tipo:");
            System.out.printf("    %-28s %d%n", LpAudiofiloDeluxe.TIPO, contarNoArmazem(LpAudiofiloDeluxe.TIPO));
            System.out.printf("    %-28s %d%n", LpStandard.TIPO, contarNoArmazem(LpStandard.TIPO));
            System.out.printf("    %-28s %d%n", CompactoSete.TIPO, contarNoArmazem(CompactoSete.TIPO));
        }
        System.out.printf("Total no armazem: %d | Total de discos ja produzidos (inclui rejeitados): %d%n",
                produtosFabricados.size(), Produto.getTotalProdutosFabricados());
    }

    public void exibirMaquinas() {
        System.out.println("\n--- LINHA DE PRODUCAO ---");
        for (Maquina m : maquinas) {
            System.out.println("  " + m);
        }
    }

    public double getBudget() {
        return budget;
    }

    /** A mesma colecao aceita objetos de duas hierarquias diferentes. */
    public void gerarAuditoriaGeral() {
        ArrayList<Auditavel> componentes = new ArrayList<>();
        componentes.addAll(maquinas);
        componentes.addAll(produtosFabricados);
        int alertas = 0;
        System.out.println("\n--- AUDITORIA: AGULHA FINA ---");
        for (Auditavel componente : componentes) {
            System.out.println(componente.gerarRelatorioDiagnostico());
            if (componente.precisaManutencao()) {
                alertas++;
            }
        }
        System.out.printf("Componentes: %d | Alertas: %d%n", componentes.size(), alertas);
    }

    private void atualizarEstimativas(Demanda demanda) {
        demanda.configurarEstimativas(pvcPorUnidade(demanda.getTipoProduto()), calcularCustoProducao(1));
    }

    private boolean linhaDisponivel() {
        for (Maquina maquina : maquinas) {
            if (maquina.getStatus() == StatusMaquina.QUEBRADA) {
                return false;
            }
        }
        return true;
    }

    private void cancelarDemandasSemRecursos() {
        for (Demanda demanda : demandas) {
            if (demanda.estaPendente() && (demanda.calcularQuantidadeViavel(budget) == 0
                    || !materiaPrima.verificarDisponibilidade(demanda.getConsumoPorUnidade()))) {
                demanda.cancelar();
                System.out.println("Pedido cancelado por falta de recursos: " + demanda.getTipoProduto());
            }
        }
    }

    // ---- Logica interna (privada) -----------------------------------------

    /** Custo de operacao de todas as maquinas para uma quantidade de discos. */
    private double calcularCustoProducao(int quantidade) {
        double custoPorDisco = 0.0;
        for (Maquina m : maquinas) {
            custoPorDisco += m.getCustoOperacao();
        }
        return custoPorDisco * quantidade;
    }

    /**
     * Quantos discos da demanda cabem no estoque e no budget atuais.
     * Avisa o usuario quando o lote precisa ser reduzido.
     */
    private int calcularLoteViavel(Demanda demanda) {
        int pedido = demanda.getQuantidadeProdutos();
        double pvcPorUnidade = pvcPorUnidade(demanda.getTipoProduto());
        double custoPorDisco = calcularCustoProducao(1);

        int porEstoque = (int) Math.floor((materiaPrima.getQuantidade() + 1e-9) / pvcPorUnidade);
        int porBudget = demanda.calcularQuantidadeViavel(budget);
        int viavel = Math.min(pedido, Math.min(porEstoque, porBudget));
        for (Maquina maquina : maquinas) {
            viavel = Math.min(viavel, maquina.getCapacidadeMaxima());
        }

        if (viavel <= 0) {
            System.out.printf("[ERRO] Impossivel fabricar %s: precisa de %.2f kg de PVC (ha %.2f) e R$%.2f por disco (ha R$%.2f).%n",
                    demanda.getTipoProduto(), pvcPorUnidade, materiaPrima.getQuantidade(), custoPorDisco, budget);
            return 0;
        }
        if (viavel < pedido) {
            System.out.printf("[AVISO] Pedido de %d, mas recursos/capacidade permitem %d neste lote.%n",
                    pedido, viavel);
        }
        return viavel;
    }

    /** Leva o disco por todas as maquinas, debitando o custo de cada operacao. */
    private boolean passarPelaLinha(Produto disco) {
        for (Maquina maquina : maquinas) {
            if (!maquina.estaLigada() || budget + 1e-9 < maquina.getCustoOperacao()) {
                return false;
            }
            budget = Math.max(0.0, budget - maquina.getCustoOperacao());
            if (!maquina.processar(disco)) {
                return false;
            }
        }
        return true;
    }

    /** Tritura o disco rejeitado e devolve parte do PVC ao estoque. */
    private double reciclar(Produto disco) {
        double recuperado = disco.getQuantidadeMateriaPrimaPorUnidade() * TAXA_RECICLAGEM;
        materiaPrima.adicionarEstoque(recuperado);
        return recuperado;
    }

    private Produto criarProduto(String tipoProduto) {
        if (LpAudiofiloDeluxe.TIPO.equals(tipoProduto)) {
            return new LpAudiofiloDeluxe();
        } else if (LpStandard.TIPO.equals(tipoProduto)) {
            return new LpStandard();
        } else if (CompactoSete.TIPO.equals(tipoProduto)) {
            return new CompactoSete();
        }
        System.out.println("[ERRO] Tipo de produto desconhecido: " + tipoProduto);
        return null;
    }

    private double pvcPorUnidade(String tipoProduto) {
        if (LpAudiofiloDeluxe.TIPO.equals(tipoProduto)) {
            return LpAudiofiloDeluxe.PVC_POR_UNIDADE;
        } else if (LpStandard.TIPO.equals(tipoProduto)) {
            return LpStandard.PVC_POR_UNIDADE;
        } else if (CompactoSete.TIPO.equals(tipoProduto)) {
            return CompactoSete.PVC_POR_UNIDADE;
        }
        return 0;
    }

    private Demanda buscarDemanda(String tipoProduto) {
        for (Demanda d : demandas) {
            if (d.getTipoProduto().equals(tipoProduto) && d.estaPendente()) {
                return d;
            }
        }
        return null;
    }

    private int contarNoArmazem(String tipoProduto) {
        int total = 0;
        for (Produto p : produtosFabricados) {
            if (p.getTipo().equals(tipoProduto)) {
                total++;
            }
        }
        return total;
    }

    private void ligarMaquinas() {
        for (Maquina m : maquinas) {
            m.ligar();
        }
    }

    private void desligarMaquinas() {
        for (Maquina m : maquinas) {
            m.desligar();
        }
    }
}
