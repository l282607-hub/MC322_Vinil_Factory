import java.util.Random;
import java.util.ArrayList;
import java.util.List;

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
 *   <li>A saude estrutural e a menor condicao entre motor, sensor e atuadores.
 *       A chance de falha cresce com desgaste e desalinhamento do sensor.</li>
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
    private LinhaDeProducao linhaInstalada;
    private final MotorAcionamento motor;
    private final SensorAlinhamento sensor;
    private final List<AtuadorTransporte> atuadores;
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
        this(nome, capacidadeMaxima, probabilidadeFalha, custoOperacao, 2);
    }

    /**
     * Constroi as partes exclusivas a partir de configuracoes basicas.
     * @param nome nome da maquina
     * @param capacidadeMaxima discos por lote, positivo
     * @param probabilidadeFalha chance base entre 0 e 1
     * @param custoOperacao custo positivo por disco
     * @param quantidadeAtuadores quantidade positiva de mecanismos de transporte
     * @throws IllegalArgumentException se alguma configuracao for invalida
     */
    protected Maquina(String nome, int capacidadeMaxima, double probabilidadeFalha,
            double custoOperacao, int quantidadeAtuadores) {
        if (nome == null || nome.trim().isEmpty() || capacidadeMaxima <= 0 || !Double.isFinite(probabilidadeFalha)
                || probabilidadeFalha < 0 || probabilidadeFalha > 1
                || !Double.isFinite(custoOperacao) || custoOperacao <= 0 || quantidadeAtuadores <= 0) {
            throw new IllegalArgumentException("Parametros invalidos da maquina.");
        }
        this.status = StatusMaquina.DESLIGADA;
        this.nome = nome;
        this.capacidadeMaxima = capacidadeMaxima;
        this.probabilidadeFalha = probabilidadeFalha;
        this.custoOperacao = custoOperacao;
        this.motor = new MotorAcionamento();
        this.sensor = new SensorAlinhamento();
        this.atuadores = new ArrayList<>();
        // Um mecanismo recebe o disco e o outro o encaminha para a saida.
        for (int numero = 1; numero <= quantidadeAtuadores; numero++) {
            atuadores.add(new AtuadorTransporte(numero));
        }
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
        if (status != StatusMaquina.QUEBRADA && partesOperantes()) {
            motor.ligar();
            status = StatusMaquina.LIGADA;
        }
    }

    /** Desliga a maquina. Uma maquina quebrada permanece quebrada. */
    public void desligar() {
        motor.desligar();
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
        return status == StatusMaquina.LIGADA && motor.estaLigado() && partesOperantes();
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

    // A linha controla a instalacao; essa referencia nao expoe partes da maquina.
    boolean estaInstalada() { return linhaInstalada != null; }

    void vincularLinha(LinhaDeProducao linha) {
        if (linha == null || estaInstalada() || estaLigada()) {
            throw new IllegalArgumentException("A maquina deve estar desligada e fora de outra linha.");
        }
        linhaInstalada = linha;
    }

    void desvincularLinha(LinhaDeProducao linha) {
        if (linhaInstalada != linha || estaLigada()) {
            throw new MaquinaIndisponivelException("A maquina so pode ser retirada de sua linha e desligada.");
        }
        linhaInstalada = null;
    }

    /**
     * Calcula a chance efetiva de falha, considerando cenario e desgaste.
     *
     * @return probabilidade entre 0 e 1, incluindo o risco de desalinhamento
     */
    public double getProbabilidadeFalha() {
        double saude = getSaude();
        if (saude == 0) {
            return 1.0;
        }
        return Math.min(1.0, probabilidadeFalha * cenario.getFatorFalha() * 100.0 / saude
                + sensor.calcularRiscoDesalinhamento());
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
        double desgasteMaximo = cenario.getDesgasteMaximo();
        motor.desgastar(sortear() * desgasteMaximo);
        sensor.desgastar(sortear() * desgasteMaximo);
        for (AtuadorTransporte atuador : atuadores) {
            atuador.desgastar(sortear() * desgasteMaximo);
        }
        if (!partesOperantes()) {
            status = StatusMaquina.QUEBRADA;
            motor.desligar();
        }
    }

    /**
     * Retorna a saude atual.
     *
     * @return menor saude entre as partes, entre 0 e 100; nenhuma referencia interna
     */
    public double getSaude() {
        double saude = Math.min(motor.getSaude(), sensor.getCalibracao());
        for (AtuadorTransporte atuador : atuadores) {
            saude = Math.min(saude, atuador.getIntegridade());
        }
        return saude;
    }

    private boolean partesOperantes() {
        if (motor.getSaude() == 0 || !sensor.estaOperante()) { return false; }
        for (AtuadorTransporte atuador : atuadores) {
            if (!atuador.estaOperante()) { return false; }
        }
        return true;
    }

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
        boolean alerta = getSaude() < limiarManutencao || motor.precisaManutencao()
                || sensor.precisaManutencao();
        for (AtuadorTransporte atuador : atuadores) { alerta |= atuador.precisaManutencao(); }
        return alerta;
    }

    /**
     * Monta o diagnostico da maquina: saude, status, chance de falha e erros.
     *
     * @return texto de diagnostico
     */
    @Override
    public String gerarRelatorioDiagnostico() {
        StringBuilder texto = new StringBuilder(String.format(
                "%s | saude %.1f/100 | %s | falha %.1f%% | erros %d | %s",
                nome, getSaude(), status, getProbabilidadeFalha() * 100, historicoFalhas,
                precisaManutencao() ? "MANUTENCAO NECESSARIA" : "operacional"));
        texto.append("\n  ").append(motor.gerarRelatorioDiagnostico());
        texto.append("\n  ").append(sensor.gerarRelatorioDiagnostico());
        for (AtuadorTransporte atuador : atuadores) {
            texto.append("\n  ").append(atuador.gerarRelatorioDiagnostico());
        }
        return texto.toString();
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
