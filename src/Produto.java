/**
 * Classe abstrata que representa um disco de vinil generico produzido pela
 * GroovePress Vinyl Works. Cada formato de disco (compacto, LP standard,
 * LP deluxe) e uma subclasse com qualidade e consumo de PVC proprios.
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

    /** Tempo estimado de producao de uma unidade, em minutos. */
    public abstract double calcularTempoProducao();

    /** Nome do tipo (usado nas demandas e no armazem). */
    public abstract String getTipo();

    // ---- Metodos concretos -------------------------------------------------

    public void aumentarProbabilidadeFalha(double incremento) {
        if (incremento > 0) {
            probabilidadeFalhaAcumulada = Math.min(1.0, probabilidadeFalhaAcumulada + incremento);
        }
    }

    public static int getTotalProdutosFabricados() {
        return totalProdutosFabricados;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public StatusProduto getStatus() {
        return status;
    }

    public void setStatus(StatusProduto status) {
        if (status == null) {
            throw new IllegalArgumentException("Status obrigatorio.");
        }
        this.status = status;
    }

    public int getLote() { return lote; }

    public void setLote(int lote) {
        if (lote <= 0 || this.lote != 0) {
            throw new IllegalArgumentException("Lote deve ser positivo e atribuido uma unica vez.");
        }
        this.lote = lote;
    }

    /** Para discos, intervencao significa revisar a qualidade antes da venda. */
    @Override
    public boolean precisaManutencao() {
        return qualidade < 0.5 || probabilidadeFalhaAcumulada >= 0.20
                || status == StatusProduto.REJEITADO;
    }

    @Override
    public String gerarRelatorioDiagnostico() {
        return String.format("%s | risco acumulado %.0f%% | %s", toString(),
                probabilidadeFalhaAcumulada * 100,
                precisaManutencao() ? "REVISAR DISCO" : "sem alerta");
    }

    public double getQuantidadeMateriaPrimaPorUnidade() {
        return quantidadeMateriaPrimaPorUnidade;
    }

    public double getQualidade() {
        return qualidade;
    }

    public double getProbabilidadeFalhaAcumulada() {
        return probabilidadeFalhaAcumulada;
    }

    @Override
    public String toString() {
        return String.format("#%03d %-28s | lote %03d | qualidade %.1f | status: %s",
                id, nome, lote, qualidade, status);
    }
}
