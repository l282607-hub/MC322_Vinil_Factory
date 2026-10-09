import java.util.ArrayList;
import java.util.List;

/**
 * Coordena a planta da GroovePress: pedidos das gravadoras, linha de maquinas,
 * estoque de PVC, armazem de discos prontos e o budget alocado pela diretoria.
 *
 * <h2>Tratamento de erros</h2>
 * <p>Toda operacao que o usuario pode pedir valida <strong>primeiro</strong>
 * e so depois altera o estado. Se a regra de negocio impede a operacao, o
 * metodo lanca uma {@link PlantaException} e nada foi modificado: nem estoque,
 * nem budget, nem pedidos, nem maquinas. Assim a camada de interface pode
 * informar o usuario e seguir com outra operacao.</p>
 *
 * <p>Durante um lote, as maquinas sao sempre desligadas e o pedido sempre
 * volta a um estado valido (bloco {@code finally}), mesmo que ocorra uma falha
 * inesperada.</p>
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 * @see PlantaException
 * @see EstrategiaProducao
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
    private EstrategiaProducao estrategiaAtual = new EstrategiaFilaGravadoras();
    private int ultimoLote;

    /**
     * Cria a planta com a estrategia padrao (fila das gravadoras) e o cenario
     * ideal.
     *
     * @param materiaPrima estoque de PVC, obrigatorio
     * @param budget       orcamento inicial em reais, finito e nao negativo
     * @throws IllegalArgumentException se o estoque for nulo ou o budget for
     *                                  invalido
     */
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

    /**
     * Cria a planta a partir de um cenario; o budget inicial vem do cenario.
     *
     * @param materiaPrima estoque de PVC, obrigatorio
     * @param cenario      cenario da simulacao, obrigatorio
     * @param estrategia   estrategia de selecao de pedidos, obrigatoria
     * @throws IllegalArgumentException se algum argumento for nulo
     */
    public GerenciadorProducao(MateriaPrima materiaPrima, Cenario cenario,
            EstrategiaProducao estrategia) {
        this(materiaPrima, cenario.getBudgetInicial());
        this.cenario = cenario;
        setEstrategia(estrategia);
    }

    /**
     * Troca a estrategia que escolhe o proximo pedido.
     *
     * @param novaEstrategia estrategia a usar, obrigatoria
     * @throws IllegalArgumentException se {@code novaEstrategia} for nula
     */
    public final void setEstrategia(EstrategiaProducao novaEstrategia) {
        if (novaEstrategia == null) {
            throw new IllegalArgumentException("Estrategia obrigatoria.");
        }
        estrategiaAtual = novaEstrategia;
    }

    /**
     * Retorna o nome da estrategia ativa.
     *
     * @return nome legivel da estrategia
     */
    public String getNomeEstrategia() {
        return estrategiaAtual.getNomeEstrategia();
    }

    /**
     * Retorna o cenario da simulacao.
     *
     * @return cenario atual
     */
    public Cenario getCenario() { return cenario; }

    /**
     * Produz o proximo pedido escolhido pela estrategia ativa.
     *
     * <p>Se existem pedidos pendentes, mas a estrategia nao encontra nenhum
     * elegivel com o budget atual, informa o fato e cancela os pedidos que
     * ficaram sem recursos (regra de cancelamento da Tarefa 3).</p>
     *
     * @throws MaquinaIndisponivelException se nao ha maquinas ou alguma esta
     *                                      quebrada
     * @throws DemandaInvalidaException     se nao ha nenhum pedido pendente
     * @throws RecursoInsuficienteException se o pedido escolhido nao pode ser
     *                                      fabricado por falta de PVC ou budget
     */
    public void executarProximaProducao() {
        validarLinhaOperante();
        if (!existePedidoPendente()) {
            throw new DemandaInvalidaException(
                    "Nao ha pedidos pendentes para produzir. Registre um pedido no menu de pedidos.");
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

    /**
     * Instala uma maquina no fim da linha. A ordem de instalacao e a ordem em
     * que os discos passam pelas maquinas.
     *
     * @param maquina maquina a instalar, obrigatoria
     * @throws IllegalArgumentException se {@code maquina} for nula
     */
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

    /**
     * Registra um pedido ou atualiza a quantidade do pedido pendente do mesmo
     * tipo. Pedidos concluidos ou cancelados ficam no historico e um novo
     * pedido e criado no fim da fila.
     *
     * @param tipoProduto tipo de disco, igual a constante {@code TIPO} de uma
     *                    das subclasses de {@link Produto}
     * @param quantidade  discos pendentes, positiva
     * @throws DemandaInvalidaException     se o tipo for desconhecido ou a
     *                                      quantidade nao for positiva
     * @throws MaquinaIndisponivelException se a linha ainda nao tem maquinas
     */
    public void registrarDemanda(String tipoProduto, int quantidade) {
        if (pvcPorUnidade(tipoProduto) == 0) {
            throw new DemandaInvalidaException("Tipo de disco desconhecido: " + tipoProduto + ".");
        }
        if (quantidade <= 0) {
            throw new DemandaInvalidaException("A quantidade do pedido de " + tipoProduto
                    + " deve ser positiva, mas foi " + quantidade + ".");
        }
        if (maquinas.isEmpty()) {
            throw new MaquinaIndisponivelException(
                    "Nenhuma maquina instalada na planta. Configure a linha antes de registrar pedidos.");
        }
        Demanda existente = buscarDemandaPendente(tipoProduto);
        if (existente == null) {
            Demanda nova = new Demanda(tipoProduto, quantidade);
            atualizarEstimativas(nova);
            demandas.add(nova);
        } else {
            existente.atualizarQuantidade(quantidade);
        }
        System.out.printf("[OK] Pedido de %s: %d disco(s) pendente(s).%n", tipoProduto, quantidade);
    }

    /**
     * Atualiza a quantidade pendente de um tipo. Equivale a
     * {@link #registrarDemanda(String, int)}.
     *
     * @param tipoProduto  tipo de disco
     * @param novaQuantidade nova quantidade pendente, positiva
     * @throws DemandaInvalidaException     se o tipo for desconhecido ou a
     *                                      quantidade nao for positiva
     * @throws MaquinaIndisponivelException se a linha ainda nao tem maquinas
     */
    public void atualizarDemanda(String tipoProduto, int novaQuantidade) {
        registrarDemanda(tipoProduto, novaQuantidade);
    }

    /** Lista todos os pedidos, inclusive concluidos e cancelados. */
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

    /**
     * Fabrica o pedido pendente de um tipo especifico.
     *
     * @param tipoProduto tipo de disco a fabricar
     * @throws DemandaInvalidaException     se nao ha pedido pendente desse tipo
     *                                      (inexistente, concluido ou cancelado)
     * @throws MaquinaIndisponivelException se nao ha maquinas ou alguma esta
     *                                      quebrada
     * @throws RecursoInsuficienteException se nao ha PVC ou budget para
     *                                      fabricar nem um disco
     */
    public void fabricarDemanda(String tipoProduto) {
        Demanda demanda = buscarDemandaPendente(tipoProduto);
        if (demanda == null) {
            throw new DemandaInvalidaException(descreverAusenciaDePedido(tipoProduto));
        }
        fabricarDemanda(demanda);
    }

    /**
     * Producao manual e automatica compartilham o mesmo fluxo: primeiro todas
     * as validacoes (que podem lancar excecao sem alterar nada), depois o
     * lote propriamente dito.
     */
    private void fabricarDemanda(Demanda demanda) {
        String tipoProduto = demanda.getTipoProduto();
        validarLinhaOperante();
        int quantidade = calcularLoteViavel(demanda);

        // ---- A partir daqui o estado muda: nenhuma excecao de dominio ------
        demanda.iniciarProducao();
        int lote = ++ultimoLote;
        System.out.printf("%n>>> Lote %03d: %d x %s%n", lote, quantidade, tipoProduto);
        ligarMaquinas();

        int aprovados = 0;
        int rejeitados = 0;
        double pvcReciclado = 0.0;
        double tempoTotal = 0.0;
        double budgetInicial = budget;

        try {
            for (int i = 0; i < quantidade; i++) {
                if (!linhaDisponivel()) {
                    System.out.println("[AVISO] Lote interrompido: uma maquina quebrou no ciclo anterior.");
                    break;
                }
                Produto disco = criarProduto(tipoProduto);
                if (!materiaPrima.consumir(disco.getQuantidadeMateriaPrimaPorUnidade())) {
                    System.out.println("[AVISO] PVC acabou no meio do lote.");
                    break;
                }
                disco.setLote(lote);

                System.out.printf("  Disco #%03d entra na linha%n", disco.getId());
                boolean aprovado = passarPelaLinha(disco);
                if (!aprovado && disco.getStatus() != StatusProduto.REJEITADO) {
                    // Uma operacao interrompida tambem devolve parte do PVC.
                    System.out.println("[AVISO] Operacao interrompida. Disco inacabado enviado para reciclagem.");
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
        } finally {
            // Garante estado consistente mesmo diante de uma falha inesperada.
            desligarMaquinas();
            demanda.atender(aprovados);
        }
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

    /**
     * Compra PVC do fornecedor, debitando o custo do budget.
     *
     * @param quantidade quilogramas a comprar, finito e positivo
     * @throws EntradaInvalidaException     se a quantidade nao for um numero
     *                                      positivo e finito
     * @throws RecursoInsuficienteException se o custo da compra excede o budget
     */
    public void comprarMateriaPrima(double quantidade) {
        if (!Double.isFinite(quantidade) || quantidade <= 0) {
            throw new EntradaInvalidaException(
                    "A quantidade de PVC deve ser um numero positivo, mas foi " + quantidade + ".");
        }
        double custo = quantidade * materiaPrima.getCustoPorUnidade();
        if (!Double.isFinite(custo)) {
            throw new EntradaInvalidaException("A quantidade de PVC informada e grande demais.");
        }
        if (custo > budget) {
            throw new RecursoInsuficienteException(String.format(
                    "Nao foi possivel comprar %.2f %s de %s: custa R$%.2f e o budget disponivel e R$%.2f.",
                    quantidade, materiaPrima.getUnidade(), materiaPrima.getNome(), custo, budget),
                    "budget", custo, budget);
        }
        budget -= custo;
        materiaPrima.adicionarEstoque(quantidade);
        System.out.printf("[OK] Comprados %.2f %s de %s por R$%.2f.%n",
                quantidade, materiaPrima.getUnidade(), materiaPrima.getNome(), custo);
        exibirEstoque();
        exibirBudget();
    }

    // ---- Consultas ---------------------------------------------------------

    /** Mostra o budget atual. */
    public void exibirBudget() {
        System.out.printf("BUDGET ATUAL: R$%.2f%n", budget);
    }

    /** Mostra o estoque atual de PVC. */
    public void exibirEstoque() {
        System.out.println("Estoque de " + materiaPrima);
    }

    /** Lista os discos aprovados no armazem, com totais por tipo. */
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

    /** Lista as maquinas da linha com seu diagnostico. */
    public void exibirMaquinas() {
        System.out.println("\n--- LINHA DE PRODUCAO ---");
        for (Maquina m : maquinas) {
            System.out.println("  " + m);
        }
    }

    /**
     * Retorna o budget atual.
     *
     * @return reais disponiveis
     */
    public double getBudget() {
        return budget;
    }

    /**
     * Gera o relatorio de auditoria. A mesma colecao aceita objetos de duas
     * hierarquias diferentes ({@link Maquina} e {@link Produto}) por meio da
     * interface {@link Auditavel}.
     */
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

    // ---- Validacoes (lancam excecao sem alterar estado) --------------------

    /**
     * Confirma que a linha pode operar.
     *
     * @throws MaquinaIndisponivelException se nao ha maquinas ou alguma esta
     *                                      quebrada
     */
    private void validarLinhaOperante() {
        if (maquinas.isEmpty()) {
            throw new MaquinaIndisponivelException(
                    "Nenhuma maquina instalada na planta. Configure a linha antes de produzir.");
        }
        List<String> quebradas = new ArrayList<>();
        for (Maquina maquina : maquinas) {
            if (maquina.getStatus() == StatusMaquina.QUEBRADA) {
                quebradas.add(maquina.getNome());
            }
        }
        if (!quebradas.isEmpty()) {
            throw new MaquinaIndisponivelException("Linha parada: " + String.join(", ", quebradas)
                    + " quebrou. O reparo nao esta disponivel nesta simulacao; veja a auditoria (menu 6).");
        }
    }

    /**
     * Quantos discos da demanda cabem no estoque, no budget e na capacidade
     * das maquinas. Avisa o usuario quando o lote precisa ser reduzido.
     *
     * @throws RecursoInsuficienteException se nao cabe nem um disco
     */
    private int calcularLoteViavel(Demanda demanda) {
        int pedido = demanda.getQuantidadeProdutos();
        String tipo = demanda.getTipoProduto();
        double pvcPorUnidade = pvcPorUnidade(tipo);
        double custoPorDisco = calcularCustoProducao(1);

        int porEstoque = (int) Math.floor((materiaPrima.getQuantidade() + 1e-9) / pvcPorUnidade);
        int porBudget = demanda.calcularQuantidadeViavel(budget);
        if (porEstoque <= 0) {
            throw new RecursoInsuficienteException(String.format(
                    "Nao foi possivel iniciar a producao de %s: PVC insuficiente. "
                    + "Cada disco usa %.2f %s e o estoque tem %.2f %s. Compre PVC pelo menu e tente de novo.",
                    tipo, pvcPorUnidade, materiaPrima.getUnidade(),
                    materiaPrima.getQuantidade(), materiaPrima.getUnidade()),
                    "PVC", pvcPorUnidade, materiaPrima.getQuantidade());
        }
        if (porBudget <= 0) {
            throw new RecursoInsuficienteException(String.format(
                    "Nao foi possivel iniciar a producao de %s: budget insuficiente. "
                    + "Cada disco custa R$%.2f em operacao e o budget e R$%.2f.",
                    tipo, custoPorDisco, budget),
                    "budget", custoPorDisco, budget);
        }
        int viavel = Math.min(pedido, Math.min(porEstoque, porBudget));
        for (Maquina maquina : maquinas) {
            viavel = Math.min(viavel, maquina.getCapacidadeMaxima());
        }
        if (viavel < pedido) {
            System.out.printf("[AVISO] Pedido de %d, mas recursos/capacidade permitem %d neste lote.%n",
                    pedido, viavel);
        }
        return viavel;
    }

    // ---- Logica interna (privada) -----------------------------------------

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

    /** Cancela pedidos pendentes que nao cabem mais no budget nem no estoque. */
    private void cancelarDemandasSemRecursos() {
        for (Demanda demanda : demandas) {
            if (demanda.estaPendente() && (demanda.calcularQuantidadeViavel(budget) == 0
                    || !materiaPrima.verificarDisponibilidade(demanda.getConsumoPorUnidade()))) {
                demanda.cancelar();
                System.out.println("Pedido cancelado por falta de recursos: " + demanda.getTipoProduto());
            }
        }
    }

    /** Custo de operacao de todas as maquinas para uma quantidade de discos. */
    private double calcularCustoProducao(int quantidade) {
        double custoPorDisco = 0.0;
        for (Maquina m : maquinas) {
            custoPorDisco += m.getCustoOperacao();
        }
        return custoPorDisco * quantidade;
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
        throw new DemandaInvalidaException("Tipo de disco desconhecido: " + tipoProduto + ".");
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

    private Demanda buscarDemandaPendente(String tipoProduto) {
        for (Demanda d : demandas) {
            if (d.getTipoProduto().equals(tipoProduto) && d.estaPendente()) {
                return d;
            }
        }
        return null;
    }

    private boolean existePedidoPendente() {
        for (Demanda d : demandas) {
            if (d.estaPendente()) {
                return true;
            }
        }
        return false;
    }

    /** Explica por que nao ha pedido pendente: nunca existiu ou ja foi encerrado. */
    private String descreverAusenciaDePedido(String tipoProduto) {
        if (pvcPorUnidade(tipoProduto) == 0) {
            return "Tipo de disco desconhecido: " + tipoProduto + ".";
        }
        Demanda maisRecente = null;
        for (Demanda d : demandas) {
            if (d.getTipoProduto().equals(tipoProduto)) {
                maisRecente = d;
            }
        }
        if (maisRecente == null) {
            return "Nao ha pedido registrado para " + tipoProduto
                    + ". Registre um pedido no menu de pedidos.";
        }
        return "O pedido de " + tipoProduto + " esta " + maisRecente.getStatus().getDescricao()
                + " e nao tem discos pendentes. Registre um novo pedido no menu de pedidos.";
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
