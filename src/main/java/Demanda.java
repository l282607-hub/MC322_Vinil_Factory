/**
 * Pedido de uma gravadora. A quantidade representa os discos ainda devidos.
 *
 * <p>O pedido segue o ciclo de {@link StatusDemanda}. Operacoes incompativeis
 * com o estado atual (por exemplo, produzir um pedido ja concluido) lancam
 * {@link DemandaInvalidaException} sem alterar o pedido.</p>
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 */
public class Demanda {
    private String tipoProduto;
    private int quantidadeProdutos;
    private StatusDemanda status;
    private double consumoPorUnidade;
    private double custoOperacaoPorUnidade;

    /**
     * Cria um pedido pendente.
     *
     * @param tipoProduto        tipo de disco pedido
     * @param quantidadeProdutos quantidade de discos, positiva
     * @throws DemandaInvalidaException se o tipo for nulo ou a quantidade nao
     *                                  for positiva
     */
    public Demanda(String tipoProduto, int quantidadeProdutos) {
        if (tipoProduto == null) {
            throw new DemandaInvalidaException("Pedido sem tipo de disco.");
        }
        if (quantidadeProdutos <= 0) {
            throw new DemandaInvalidaException("A quantidade de discos do pedido deve ser positiva, mas foi "
                    + quantidadeProdutos + ".");
        }
        this.tipoProduto = tipoProduto;
        this.quantidadeProdutos = quantidadeProdutos;
        this.status = StatusDemanda.PENDENTE;
    }

    /**
     * Define as estimativas por unidade usadas nos calculos de viabilidade.
     *
     * @param consumo kg de PVC por disco, finito e positivo
     * @param custo   reais de operacao por disco, finito e positivo
     * @throws IllegalArgumentException se algum valor for invalido (erro de
     *                                  programacao, nao de usuario)
     */
    public void configurarEstimativas(double consumo, double custo) {
        if (!Double.isFinite(consumo) || !Double.isFinite(custo) || consumo <= 0 || custo <= 0) {
            throw new IllegalArgumentException("Consumo e custo devem ser positivos.");
        }
        consumoPorUnidade = consumo;
        custoOperacaoPorUnidade = custo;
    }

    /**
     * Troca a quantidade pendente de um pedido ainda pendente.
     *
     * @param quantidade nova quantidade, positiva
     * @throws DemandaInvalidaException se o pedido nao esta pendente ou a
     *                                  quantidade nao e positiva
     */
    public void atualizarQuantidade(int quantidade) {
        exigirPendente("alterar a quantidade");
        if (quantidade <= 0) {
            throw new DemandaInvalidaException("A quantidade do pedido de " + tipoProduto
                    + " deve ser positiva, mas foi " + quantidade + ".");
        }
        quantidadeProdutos = quantidade;
    }

    /**
     * Marca o pedido como em producao.
     *
     * @throws DemandaInvalidaException se o pedido nao esta pendente
     */
    public void iniciarProducao() {
        exigirPendente("iniciar a producao");
        status = StatusDemanda.EM_PRODUCAO;
    }

    /**
     * Encerra um lote: um lote parcial volta para a fila; apenas aprovados
     * abatem o pedido.
     *
     * @param quantidadeAprovada discos aprovados no lote, entre 0 e a
     *                           quantidade pendente
     * @throws DemandaInvalidaException se o pedido nao esta em producao ou a
     *                                  quantidade aprovada e invalida
     */
    public void atender(int quantidadeAprovada) {
        if (status != StatusDemanda.EM_PRODUCAO) {
            throw new DemandaInvalidaException("Nao e possivel concluir um lote do pedido de "
                    + tipoProduto + ": ele esta " + status.getDescricao() + ".");
        }
        if (quantidadeAprovada < 0 || quantidadeAprovada > quantidadeProdutos) {
            throw new DemandaInvalidaException("Lote do pedido de " + tipoProduto + " informa "
                    + quantidadeAprovada + " disco(s) aprovado(s), mas faltam " + quantidadeProdutos + ".");
        }
        quantidadeProdutos -= quantidadeAprovada;
        status = quantidadeProdutos == 0 ? StatusDemanda.CONCLUIDA : StatusDemanda.PENDENTE;
    }

    /**
     * Cancela o pedido. Estado final: nao pode ser reativado.
     *
     * @throws DemandaInvalidaException se o pedido nao esta pendente
     */
    public void cancelar() {
        exigirPendente("cancelar");
        status = StatusDemanda.CANCELADA;
    }

    /** Garante que o pedido esta pendente antes de uma operacao. */
    private void exigirPendente(String operacao) {
        if (!estaPendente()) {
            throw new DemandaInvalidaException("Nao e possivel " + operacao + " o pedido de "
                    + tipoProduto + ": ele esta " + status.getDescricao() + ".");
        }
    }

    /**
     * Calcula o PVC necessario para a quantidade pendente.
     *
     * @param quantidadePorUnidade kg de PVC por disco
     * @return kg de PVC para todos os discos pendentes
     */
    public double calcularMateriaPrimaNecessaria(double quantidadePorUnidade) {
        return quantidadeProdutos * quantidadePorUnidade;
    }

    /**
     * Calcula o PVC necessario usando a estimativa configurada.
     *
     * @return kg de PVC para todos os discos pendentes
     */
    public double calcularConsumoEstimado() {
        return calcularMateriaPrimaNecessaria(consumoPorUnidade);
    }

    /**
     * Calcula o custo de operacao da quantidade pendente.
     *
     * @return reais de operacao para todos os discos pendentes
     */
    public double calcularCustoEstimado() {
        return quantidadeProdutos * custoOperacaoPorUnidade;
    }

    /**
     * Calcula quantos discos do pedido o orcamento financia, sem arredondar
     * para cima.
     *
     * @param orcamento budget disponivel, em reais
     * @return unidades financiaveis, no maximo a quantidade pendente; 0 se o
     *         orcamento for invalido
     */
    public int calcularQuantidadeViavel(double orcamento) {
        if (!Double.isFinite(orcamento) || orcamento < 0 || custoOperacaoPorUnidade <= 0) {
            return 0;
        }
        return (int) Math.min(quantidadeProdutos, Math.floor((orcamento + 1e-9) / custoOperacaoPorUnidade));
    }

    /**
     * Verifica se o orcamento cobre o pedido inteiro.
     *
     * @param orcamento budget disponivel, em reais
     * @return {@code true} se todas as unidades pendentes sao financiaveis
     */
    public boolean ehFinanceiramenteViavel(double orcamento) {
        return calcularQuantidadeViavel(orcamento) == quantidadeProdutos;
    }

    /**
     * Informa se o pedido aguarda producao.
     *
     * @return {@code true} se o status e {@link StatusDemanda#PENDENTE}
     */
    public boolean estaPendente() { return status == StatusDemanda.PENDENTE; }

    /**
     * Informa se o pedido foi totalmente atendido.
     *
     * @return {@code true} se o status e {@link StatusDemanda#CONCLUIDA}
     */
    public boolean isAtendida() { return status == StatusDemanda.CONCLUIDA; }

    /**
     * Retorna o status do pedido.
     *
     * @return status atual
     */
    public StatusDemanda getStatus() { return status; }

    /**
     * Retorna o tipo de disco pedido.
     *
     * @return nome do formato
     */
    public String getTipoProduto() { return tipoProduto; }

    /**
     * Retorna quantos discos ainda faltam.
     *
     * @return quantidade pendente
     */
    public int getQuantidadeProdutos() { return quantidadeProdutos; }

    /**
     * Retorna o consumo de PVC por disco usado nas estimativas.
     *
     * @return kg de PVC por unidade
     */
    public double getConsumoPorUnidade() { return consumoPorUnidade; }

    /**
     * Resume o pedido em uma linha, com status e estimativas.
     *
     * @return descricao do pedido
     */
    @Override
    public String toString() {
        return String.format("%-28s | faltam: %3d | %-12s | PVC %.2f kg | operacao R$%.2f",
                tipoProduto, quantidadeProdutos, status.getDescricao(),
                calcularConsumoEstimado(), calcularCustoEstimado());
    }
}
