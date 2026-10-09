/**
 * Insumo da fabrica. Na GroovePress, o PVC reciclado, medido em kg.
 *
 * <p>Garante que a quantidade em estoque nunca fique negativa nem deixe de
 * ser um numero finito.</p>
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 */
public class MateriaPrima {
    private int id;
    private String nome;
    private double quantidade;
    private String unidade;
    private double custoPorUnidade;

    /**
     * Cria um estoque de materia-prima.
     *
     * @param id              identificador do insumo
     * @param nome            nome do insumo
     * @param quantidade      estoque inicial, finito e nao negativo
     * @param unidade         unidade de medida, por exemplo "kg"
     * @param custoPorUnidade preco por unidade, finito e positivo
     * @throws IllegalArgumentException se a quantidade ou o custo forem invalidos
     */
    public MateriaPrima(int id, String nome, double quantidade, String unidade, double custoPorUnidade) {
        if (!Double.isFinite(quantidade) || quantidade < 0
                || !Double.isFinite(custoPorUnidade) || custoPorUnidade <= 0) {
            throw new IllegalArgumentException("Estoque ou custo invalido.");
        }
        this.id = id;
        this.nome = nome;
        this.quantidade = quantidade;
        this.unidade = unidade;
        this.custoPorUnidade = custoPorUnidade;
    }

    /**
     * Retira do estoque.
     *
     * @param consumo quantidade a retirar
     * @return {@code true} se retirou; {@code false} se o valor era invalido ou
     *         nao havia estoque suficiente (nada e alterado nesse caso)
     */
    public boolean consumir(double consumo) {
        if (!verificarDisponibilidade(consumo)) {
            return false;
        }
        quantidade = Math.max(0.0, quantidade - consumo);
        return true;
    }

    /**
     * Acrescenta ao estoque. Valores nao positivos, NaN e infinitos sao
     * ignorados.
     *
     * @param quantidadeRecebida quantidade a somar
     */
    public void adicionarEstoque(double quantidadeRecebida) {
        if (Double.isFinite(quantidadeRecebida) && quantidadeRecebida > 0
                && Double.isFinite(quantidade + quantidadeRecebida)) {
            quantidade += quantidadeRecebida;
        }
    }

    /**
     * Verifica se ha estoque para uma demanda, com tolerancia de 1e-9 para
     * erros de arredondamento de ponto flutuante.
     *
     * @param demanda quantidade desejada
     * @return {@code true} se o valor e positivo, finito e cabe no estoque
     */
    public boolean verificarDisponibilidade(double demanda) {
        return Double.isFinite(demanda) && demanda > 0 && quantidade + 1e-9 >= demanda;
    }

    /**
     * Retorna o identificador do insumo.
     *
     * @return id do insumo
     */
    public int getId() {
        return id;
    }

    /**
     * Retorna o nome do insumo.
     *
     * @return nome do insumo
     */
    public String getNome() {
        return nome;
    }

    /**
     * Retorna o estoque atual.
     *
     * @return quantidade em estoque, na unidade do insumo
     */
    public double getQuantidade() {
        return quantidade;
    }

    /**
     * Retorna a unidade de medida.
     *
     * @return unidade, por exemplo "kg"
     */
    public String getUnidade() {
        return unidade;
    }

    /**
     * Retorna o preco de compra.
     *
     * @return reais por unidade
     */
    public double getCustoPorUnidade() {
        return custoPorUnidade;
    }

    /**
     * Resume o estoque em uma linha.
     *
     * @return nome, quantidade e preco
     */
    @Override
    public String toString() {
        return String.format("%s: %.2f %s (R$%.2f/%s)", nome, quantidade, unidade, custoPorUnidade, unidade);
    }
}
