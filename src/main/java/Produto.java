/**
 * Classe abstrata que representa um disco de vinil generico produzido pela
 * GroovePress Vinyl Works. Cada formato de disco (compacto, LP standard,
 * LP deluxe) e uma subclasse com qualidade e consumo de PVC proprios.
 *
 * <p>Regra de negocio central: a <em>qualidade</em> e diretamente
 * proporcional ao rigor da inspecao. Discos premium sao reprovados com mais
 * frequencia. Ao longo da linha, as maquinas podem acumular
 * {@linkplain #getProbabilidadeFalhaAcumulada() risco de falha} no disco, que
 * a inspecao soma a chance de rejeicao.</p>
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 * @see Maquina
 */
public abstract class Produto implements Auditavel {
    private static int totalProdutosFabricados = 0;

    private int id;
    private String nome;
    private StatusProduto status;
    private int lote;
    private double quantidadeMateriaPrimaPorUnidade;
    private double qualidade;
    private double probabilidadeFalhaAcumulada;

    /**
     * Cria um disco e incrementa o contador estatico, que tambem gera o id.
     *
     * @param nome                             nome do formato
     * @param quantidadeMateriaPrimaPorUnidade kg de PVC consumidos por disco
     * @param qualidade                        qualidade entre 0.0 e 1.0
     */
    protected Produto(String nome, double quantidadeMateriaPrimaPorUnidade, double qualidade) {
        totalProdutosFabricados++;
        this.id = totalProdutosFabricados;
        this.nome = nome;
        this.status = StatusProduto.AGUARDANDO;
        this.quantidadeMateriaPrimaPorUnidade = quantidadeMateriaPrimaPorUnidade;
        this.qualidade = qualidade;
        this.probabilidadeFalhaAcumulada = 0.0;
    }

    // ---- Metodos abstratos -------------------------------------------------

    /** Define o processamento especifico de cada formato de disco. */
    public abstract void processar();

    /**
     * Informa o tempo estimado de producao de uma unidade.
     *
     * @return tempo em minutos
     */
    public abstract double calcularTempoProducao();

    /**
     * Informa o tipo do disco, usado nos pedidos e no armazem.
     *
     * @return nome do formato, igual a constante {@code TIPO} da subclasse
     */
    public abstract String getTipo();

    // ---- Metodos concretos -------------------------------------------------

    /**
     * Soma risco de falha ao disco. O total e limitado a 1.0 e incrementos
     * nao positivos sao ignorados, pois o risco nunca diminui.
     *
     * @param incremento risco a acrescentar, entre 0 e 1
     */
    public void aumentarProbabilidadeFalha(double incremento) {
        if (incremento > 0) {
            probabilidadeFalhaAcumulada = Math.min(1.0, probabilidadeFalhaAcumulada + incremento);
        }
    }

    /**
     * Conta todos os discos ja criados, aprovados ou nao.
     *
     * @return total de instancias de {@code Produto} criadas na execucao
     */
    public static int getTotalProdutosFabricados() {
        return totalProdutosFabricados;
    }

    /**
     * Retorna o identificador unico do disco.
     *
     * @return id sequencial, a partir de 1
     */
    public int getId() {
        return id;
    }

    /**
     * Retorna o nome do formato.
     *
     * @return nome do formato do disco
     */
    public String getNome() {
        return nome;
    }

    /**
     * Retorna a etapa atual do disco na linha.
     *
     * @return status atual
     */
    public StatusProduto getStatus() {
        return status;
    }

    /**
     * Atualiza a etapa do disco.
     *
     * @param status novo status
     * @throws IllegalArgumentException se {@code status} for {@code null}
     */
    public void setStatus(StatusProduto status) {
        if (status == null) {
            throw new IllegalArgumentException("Status obrigatorio.");
        }
        this.status = status;
    }

    /**
     * Retorna o numero do lote em que o disco foi fabricado.
     *
     * @return numero do lote, ou 0 se ainda nao foi atribuido
     */
    public int getLote() { return lote; }

    /**
     * Atribui o lote do disco. So pode ser feito uma vez.
     *
     * @param lote numero positivo do lote
     * @throws IllegalArgumentException se o lote nao for positivo ou ja tiver
     *                                  sido atribuido
     */
    public void setLote(int lote) {
        if (lote <= 0 || this.lote != 0) {
            throw new IllegalArgumentException("Lote deve ser positivo e atribuido uma unica vez.");
        }
        this.lote = lote;
    }

    /**
     * Para discos, intervencao significa revisar a qualidade antes da venda:
     * qualidade abaixo de 0.5, risco acumulado de pelo menos 0.20 ou rejeicao.
     *
     * @return {@code true} se o disco merece revisao
     */
    @Override
    public boolean precisaManutencao() {
        return qualidade < 0.5 || probabilidadeFalhaAcumulada >= 0.20
                || status == StatusProduto.REJEITADO;
    }

    /**
     * Monta a linha de diagnostico do disco, com risco acumulado e alerta.
     *
     * @return texto de diagnostico
     */
    @Override
    public String gerarRelatorioDiagnostico() {
        return String.format("%s | risco acumulado %.0f%% | %s", toString(),
                probabilidadeFalhaAcumulada * 100,
                precisaManutencao() ? "REVISAR DISCO" : "sem alerta");
    }

    /**
     * Retorna o consumo de PVC por disco.
     *
     * @return quilogramas de PVC por unidade
     */
    public double getQuantidadeMateriaPrimaPorUnidade() {
        return quantidadeMateriaPrimaPorUnidade;
    }

    /**
     * Retorna a qualidade do formato. Quanto maior, mais rigorosa a inspecao.
     *
     * @return qualidade entre 0.0 e 1.0
     */
    public double getQualidade() {
        return qualidade;
    }

    /**
     * Retorna o risco de falha acumulado nas maquinas anteriores.
     *
     * @return risco entre 0.0 e 1.0
     */
    public double getProbabilidadeFalhaAcumulada() {
        return probabilidadeFalhaAcumulada;
    }

    /**
     * Resume o disco em uma linha: id, nome, lote, qualidade e status.
     *
     * @return descricao do disco
     */
    @Override
    public String toString() {
        return String.format("#%03d %-28s | lote %03d | qualidade %.1f | status: %s",
                id, nome, lote, qualidade, status);
    }
}
