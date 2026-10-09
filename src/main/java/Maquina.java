import java.util.Random;

/**
 * Classe abstrata para os equipamentos da linha de producao. Cada maquina
 * processa um {@link Produto} por vez, cobra um custo de operacao e pode
 * (dependendo do tipo) afetar a probabilidade de falha do disco ou falhar
 * diretamente.
 *
 * <p>Regras de negocio:</p>
 * <ul>
 *   <li>Toda maquina comeca com saude 100 e perde um desgaste sorteado a cada
 *       uso; com saude zero fica {@link StatusMaquina#QUEBRADA} e nao opera
 *       mais (o reparo nao esta implementado).</li>
 *   <li>A chance efetiva de falha e
 *       {@code min(1, chanceBase * fatorCenario * 100 / saude)}: quanto mais
 *       desgastada, mais falha.</li>
 *   <li>Uma maquina desligada ou quebrada nao processa discos.</li>
 * </ul>
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 * @see Cenario
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

    /**
     * Cria uma maquina desligada, com saude 100.
     *
     * @param nome               nome da maquina
     * @param capacidadeMaxima   maximo de discos por lote, positivo
     * @param probabilidadeFalha chance base de falha, entre 0 e 1
     * @param custoOperacao      custo em reais por disco processado, positivo
     * @throws IllegalArgumentException se algum parametro numerico for invalido
     */
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
     *
     * @param produto disco a processar
     * @return {@code true} se o produto segue na linha; {@code false} se foi
     *         rejeitado ou se a maquina nao operou (desligada ou quebrada)
     */
    public abstract boolean processar(Produto produto);

    /**
     * Informa o tipo da maquina.
     *
     * @return "Processamento", "Embalagem" ou "Inspecao"
     */
    public abstract String getTipo();

    // ---- Metodos concretos -------------------------------------------------

    /** Liga a maquina. Uma maquina quebrada permanece quebrada. */
    public void ligar() {
        if (status != StatusMaquina.QUEBRADA) {
            status = StatusMaquina.LIGADA;
        }
    }

    /** Desliga a maquina. Uma maquina quebrada permanece quebrada. */
    public void desligar() {
        if (status != StatusMaquina.QUEBRADA) {
            status = StatusMaquina.DESLIGADA;
        }
    }

    /**
     * Informa se a maquina esta em operacao.
     *
     * @return {@code true} se o status e {@link StatusMaquina#LIGADA}
     */
    public boolean estaLigada() {
        return status == StatusMaquina.LIGADA;
    }

    /**
     * Retorna o nome da maquina.
     *
     * @return nome da maquina
     */
    public String getNome() {
        return nome;
    }

    /**
     * Retorna o maximo de discos que a maquina aceita por lote.
     *
     * @return capacidade maxima por lote
     */
    public int getCapacidadeMaxima() {
        return capacidadeMaxima;
    }

    /**
     * Calcula a chance efetiva de falha, considerando cenario e desgaste.
     *
     * @return probabilidade entre 0 e 1 (1 se a saude e zero)
     */
    public double getProbabilidadeFalha() {
        if (saude == 0) {
            return 1.0;
        }
        return Math.min(1.0, probabilidadeFalha * cenario.getFatorFalha() * 100.0 / saude);
    }

    /**
     * Retorna o custo de operacao por disco.
     *
     * @return reais debitados do budget a cada disco processado
     */
    public double getCustoOperacao() {
        return custoOperacao;
    }

    /**
     * Sorteia, com a probabilidade propria da maquina, se ela falhou nesta
     * operacao. Cada falha sorteada entra no historico.
     *
     * @return {@code true} se houve falha
     */
    protected boolean verificarFalha() {
        boolean falhou = sortear() < getProbabilidadeFalha();
        if (falhou) {
            historicoFalhas++;
        }
        return falhou;
    }

    /**
     * Gera um numero aleatorio compartilhado pelas subclasses.
     *
     * @return valor em [0, 1)
     */
    protected double sortear() {
        return SORTEIO.nextDouble();
    }

    /**
     * Fixa a semente do gerador aleatorio para repetir uma simulacao.
     *
     * @param semente semente do sorteio
     */
    public static void definirSemente(long semente) {
        SORTEIO.setSeed(semente);
    }

    /**
     * Define o cenario que modifica falhas e desgaste.
     *
     * @param cenario cenario da simulacao
     * @throws IllegalArgumentException se {@code cenario} for {@code null}
     */
    public void configurarCenario(Cenario cenario) {
        if (cenario == null) {
            throw new IllegalArgumentException("Cenario obrigatorio.");
        }
        this.cenario = cenario;
    }

    /**
     * Retorna o fator de falha do cenario atual, para uso das subclasses.
     *
     * @return multiplicador de falha do cenario
     */
    protected double getFatorFalhaCenario() {
        return cenario.getFatorFalha();
    }

    /**
     * Aplica o desgaste ao final de um uso. Se a saude chega a zero, a maquina
     * quebra e o proximo ciclo fica bloqueado.
     */
    protected void registrarUso() {
        saude = Math.max(0.0, saude - sortear() * cenario.getDesgasteMaximo());
        if (saude == 0.0) {
            status = StatusMaquina.QUEBRADA;
        }
    }

    /**
     * Retorna a saude atual.
     *
     * @return saude entre 0 e 100
     */
    public double getSaude() { return saude; }

    /**
     * Retorna o estado operacional.
     *
     * @return status atual
     */
    public StatusMaquina getStatus() { return status; }

    /**
     * Configura a saude abaixo da qual a maquina precisa de manutencao.
     *
     * @param limiar valor maior que 0 e no maximo 100
     * @throws IllegalArgumentException se o limiar estiver fora do intervalo
     */
    public void setLimiarManutencao(double limiar) {
        if (!Double.isFinite(limiar) || limiar <= 0 || limiar > 100) {
            throw new IllegalArgumentException("Limiar deve estar entre 0 (exclusivo) e 100.");
        }
        limiarManutencao = limiar;
    }

    /**
     * Indica se a saude esta abaixo do limiar de manutencao.
     *
     * @return {@code true} se a manutencao e necessaria
     */
    @Override
    public boolean precisaManutencao() {
        return saude < limiarManutencao;
    }

    /**
     * Monta o diagnostico da maquina: saude, status, chance de falha e erros.
     *
     * @return texto de diagnostico
     */
    @Override
    public String gerarRelatorioDiagnostico() {
        return String.format("%s | saude %.1f/100 | %s | falha %.1f%% | erros %d | %s",
                nome, saude, status, getProbabilidadeFalha() * 100, historicoFalhas,
                precisaManutencao() ? "MANUTENCAO NECESSARIA" : "operacional");
    }

    /**
     * Resume a maquina com diagnostico, tipo, custo e capacidade.
     *
     * @return descricao da maquina
     */
    @Override
    public String toString() {
        return String.format("%s (%s) | R$%.2f/op | limite %d discos/lote",
                gerarRelatorioDiagnostico(), getTipo(), custoOperacao, capacidadeMaxima);
    }
}
