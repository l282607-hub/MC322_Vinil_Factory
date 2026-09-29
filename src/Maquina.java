import java.util.Random;

/**
 * Classe abstrata para os equipamentos da linha de producao. Cada maquina
 * processa um Produto por vez, cobra um custo de operacao e pode (dependendo
 * do tipo) afetar a probabilidade de falha do disco ou falhar diretamente.
 */
public abstract class Maquina implements Auditavel {
    private static final Random SORTEIO = new Random();

    private String nome;
    private StatusMaquina status;
    private double saude = 100.0;
    private double limiarManutencao = 30.0;
    private int historicoFalhas;
    private Cenario cenario = Cenario.IDEAL;
    private int capacidadeMaxima;
    private double probabilidadeFalha;
    private double custoOperacao;

    protected Maquina(String nome, int capacidadeMaxima, double probabilidadeFalha, double custoOperacao) {
        this.nome = nome;
        if (capacidadeMaxima <= 0 || !Double.isFinite(probabilidadeFalha)
                || probabilidadeFalha < 0 || probabilidadeFalha > 1
                || !Double.isFinite(custoOperacao) || custoOperacao <= 0) {
            throw new IllegalArgumentException("Parametros invalidos da maquina.");
        }
        this.status = StatusMaquina.DESLIGADA;
        this.capacidadeMaxima = capacidadeMaxima;
        this.probabilidadeFalha = probabilidadeFalha;
        this.custoOperacao = custoOperacao;
    }

    // ---- Metodos abstratos -------------------------------------------------

    /**
     * Processa um produto.
     * @return true se o produto segue na linha; false se rejeitado ou sem operacao.
     */
    public abstract boolean processar(Produto produto);

    public abstract String getTipo();

    // ---- Metodos concretos -------------------------------------------------

    public void ligar() {
        if (status != StatusMaquina.QUEBRADA) {
            status = StatusMaquina.LIGADA;
        }
    }

    public void desligar() {
        if (status != StatusMaquina.QUEBRADA) {
            status = StatusMaquina.DESLIGADA;
        }
    }

    public boolean estaLigada() {
        return status == StatusMaquina.LIGADA;
    }

    public String getNome() {
        return nome;
    }

    public int getCapacidadeMaxima() {
        return capacidadeMaxima;
    }

    public double getProbabilidadeFalha() {
        if (saude == 0) {
            return 1.0;
        }
        return Math.min(1.0, probabilidadeFalha * cenario.getFatorFalha() * 100.0 / saude);
    }

    public double getCustoOperacao() {
        return custoOperacao;
    }

    /** Sorteia, com a probabilidade propria da maquina, se ela falhou nesta operacao. */
    protected boolean verificarFalha() {
        boolean falhou = sortear() < getProbabilidadeFalha();
        if (falhou) {
            historicoFalhas++;
        }
        return falhou;
    }

    /** Numero aleatorio em [0, 1) compartilhado pelas subclasses. */
    protected double sortear() {
        return SORTEIO.nextDouble();
    }

    public static void definirSemente(long semente) {
        SORTEIO.setSeed(semente);
    }

    public void configurarCenario(Cenario cenario) {
        if (cenario == null) {
            throw new IllegalArgumentException("Cenario obrigatorio.");
        }
        this.cenario = cenario;
    }

    protected double getFatorFalhaCenario() {
        return cenario.getFatorFalha();
    }

    /** Desgaste ao final do uso: uma quebra impede o proximo ciclo. */
    protected void registrarUso() {
        saude = Math.max(0.0, saude - sortear() * cenario.getDesgasteMaximo());
        if (saude == 0.0) {
            status = StatusMaquina.QUEBRADA;
        }
    }

    public double getSaude() { return saude; }
    public StatusMaquina getStatus() { return status; }

    public void setLimiarManutencao(double limiar) {
        if (!Double.isFinite(limiar) || limiar <= 0 || limiar > 100) {
            throw new IllegalArgumentException("Limiar deve estar entre 0 (exclusivo) e 100.");
        }
        limiarManutencao = limiar;
    }

    @Override
    public boolean precisaManutencao() {
        return saude < limiarManutencao;
    }

    @Override
    public String gerarRelatorioDiagnostico() {
        return String.format("%s | saude %.1f/100 | %s | falha %.1f%% | erros %d | %s",
                nome, saude, status, getProbabilidadeFalha() * 100, historicoFalhas,
                precisaManutencao() ? "MANUTENCAO NECESSARIA" : "operacional");
    }

    @Override
    public String toString() {
        return String.format("%s (%s) | R$%.2f/op | limite %d discos/lote",
                gerarRelatorioDiagnostico(), getTipo(), custoOperacao, capacidadeMaxima);
    }
}
