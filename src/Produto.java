/**
 * Classe abstrata que representa um disco de vinil generico produzido pela
 * GroovePress Vinyl Works. Cada formato de disco (compacto, LP standard,
 * LP deluxe) e uma subclasse com qualidade e consumo de PVC proprios.
 */
public abstract class Produto {
    private static int totalProdutosFabricados = 0;

    private int id;
    private String nome;
    private String status;
    private double quantidadeMateriaPrimaPorUnidade;
    private double qualidade;
    private double probabilidadeFalhaAcumulada;

    protected Produto(String nome, double quantidadeMateriaPrimaPorUnidade, double qualidade) {
        totalProdutosFabricados++;
        this.id = totalProdutosFabricados;
        this.nome = nome;
        this.status = "aguardando";
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
        return String.format("#%03d %-28s | qualidade %.1f | status: %s",
                id, nome, qualidade, status);
    }
}
