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

    public GerenciadorProducao(MateriaPrima materiaPrima, double budget) {
        this.demandas = new ArrayList<>();
        this.produtosFabricados = new ArrayList<>();
        this.maquinas = new ArrayList<>();
        this.materiaPrima = materiaPrima;
        this.budget = budget;
    }

    // ---- Configuracao da planta -------------------------------------------

    public void adicionarMaquina(Maquina maquina) {
        maquinas.add(maquina);
    }

    // ---- Demandas ----------------------------------------------------------

    public void registrarDemanda(String tipoProduto, int quantidade) {
        Demanda existente = buscarDemanda(tipoProduto);
        if (existente == null) {
            demandas.add(new Demanda(tipoProduto, quantidade));
        } else {
            existente.atualizarQuantidade(quantidade);
        }
    }

    public void atualizarDemanda(String tipoProduto, int novaQuantidade) {
        registrarDemanda(tipoProduto, novaQuantidade);
        System.out.printf("[OK] Demanda de %s atualizada para %d unidade(s).%n", tipoProduto, novaQuantidade);
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
        if (demanda == null || demanda.isAtendida()) {
            System.out.println("[ERRO] Nao ha demanda em aberto para " + tipoProduto
                    + ". Atualize a demanda antes de fabricar.");
            return;
        }
        if (maquinas.isEmpty()) {
            System.out.println("[ERRO] Nenhuma maquina instalada na planta.");
            return;
        }

        int quantidade = calcularLoteViavel(demanda);
        if (quantidade == 0) {
            return;
        }

        System.out.printf("%n>>> Iniciando lote de %d x %s%n", quantidade, tipoProduto);
        ligarMaquinas();

        int aprovados = 0;
        int rejeitados = 0;
        double pvcReciclado = 0.0;
        double tempoTotal = 0.0;
        double budgetInicial = budget;

        for (int i = 0; i < quantidade; i++) {
            Produto disco = criarProduto(tipoProduto);
            if (disco == null) {
                break;
            }
            if (!materiaPrima.consumir(disco.getQuantidadeMateriaPrimaPorUnidade())) {
                System.out.println("[ERRO] PVC acabou no meio do lote.");
                break;
            }

            System.out.printf("  Disco #%03d entra na linha%n", disco.getId());
            boolean aprovado = passarPelaLinha(disco);
            if (!aprovado && !disco.getStatus().startsWith("REJEITADO")) {
                // Interrompido por falta de budget: disco inacabado vai para reciclagem.
                System.out.println("[ERRO] Budget insuficiente para concluir o lote.");
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
        if (quantidade <= 0) {
            System.out.println("[ERRO] Quantidade invalida.");
            return;
        }
        double custo = quantidade * materiaPrima.getCustoPorUnidade();
        if (custo > budget) {
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
                System.out.println("  " + p);
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

        int porEstoque = (int) Math.floor(materiaPrima.getQuantidade() / pvcPorUnidade);
        int porBudget = (int) Math.floor(budget / custoPorDisco);
        int viavel = Math.min(pedido, Math.min(porEstoque, porBudget));

        if (viavel <= 0) {
            System.out.printf("[ERRO] Impossivel fabricar %s: precisa de %.2f kg de PVC (ha %.2f) e R$%.2f por disco (ha R$%.2f).%n",
                    demanda.getTipoProduto(), pvcPorUnidade, materiaPrima.getQuantidade(), custoPorDisco, budget);
            return 0;
        }
        if (viavel < pedido) {
            System.out.printf("[AVISO] Demanda de %d, mas estoque/budget permitem apenas %d. Fabricando lote parcial.%n",
                    pedido, viavel);
        }
        return viavel;
    }

    /** Leva o disco por todas as maquinas, debitando o custo de cada operacao. */
    private boolean passarPelaLinha(Produto disco) {
        for (Maquina maquina : maquinas) {
            if (budget < maquina.getCustoOperacao()) {
                return false;
            }
            budget -= maquina.getCustoOperacao();
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
        if (tipoProduto.equals(LpAudiofiloDeluxe.TIPO)) {
            return new LpAudiofiloDeluxe();
        } else if (tipoProduto.equals(LpStandard.TIPO)) {
            return new LpStandard();
        } else if (tipoProduto.equals(CompactoSete.TIPO)) {
            return new CompactoSete();
        }
        System.out.println("[ERRO] Tipo de produto desconhecido: " + tipoProduto);
        return null;
    }

    private double pvcPorUnidade(String tipoProduto) {
        if (tipoProduto.equals(LpAudiofiloDeluxe.TIPO)) {
            return LpAudiofiloDeluxe.PVC_POR_UNIDADE;
        } else if (tipoProduto.equals(LpStandard.TIPO)) {
            return LpStandard.PVC_POR_UNIDADE;
        }
        return CompactoSete.PVC_POR_UNIDADE;
    }

    private Demanda buscarDemanda(String tipoProduto) {
        for (Demanda d : demandas) {
            if (d.getTipoProduto().equals(tipoProduto)) {
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
