import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Agrupa maquinas na ordem percorrida pelo disco. A relacao e agregacao:
 * maquinas sao recebidas prontas e podem ser retiradas e reutilizadas.
 * Cada maquina pode estar instalada em no maximo uma linha por vez.
 * Uma linha vazia representa a montagem e nao pode produzir.
 */
public class LinhaDeProducao implements Auditavel {
    private final String nome;
    private final List<Maquina> maquinas = new ArrayList<>();
    private double custoUltimoProcessamento;

    /**
     * Inicia a montagem de uma linha.
     * @param nome identificacao nao vazia
     * @throws IllegalArgumentException se o nome for vazio ou nulo
     */
    public LinhaDeProducao(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome da linha obrigatorio.");
        }
        this.nome = nome;
    }

    /**
     * Monta uma linha copiando a estrutura da lista recebida, sem clonar maquinas.
     * @param nome identificacao
     * @param maquinas equipamentos na ordem de processamento
     * @throws IllegalArgumentException se houver nulo, repetido, ligado ou ja instalado
     */
    public LinhaDeProducao(String nome, List<Maquina> maquinas) {
        this(nome);
        if (maquinas == null) { throw new IllegalArgumentException("Lista obrigatoria."); }
        List<Maquina> copia = new ArrayList<>(maquinas);
        // Valida tudo antes de vincular: um construtor rejeitado nao prende maquinas.
        for (int i = 0; i < copia.size(); i++) {
            Maquina maquina = copia.get(i);
            if (maquina == null || copia.indexOf(maquina) != i
                    || maquina.estaInstalada() || maquina.estaLigada()) {
                throw new IllegalArgumentException("Lista contem maquina invalida, repetida ou indisponivel.");
            }
        }
        for (Maquina maquina : copia) { adicionarMaquina(maquina); }
    }

    /** @return identificacao da linha */
    public String getNome() { return nome; }

    /** @return copia imutavel da estrutura, sem expor a lista interna */
    public List<Maquina> getMaquinas() {
        return Collections.unmodifiableList(new ArrayList<>(maquinas));
    }

    /**
     * Instala um equipamento no fim da linha.
     * @param maquina equipamento desligado e ainda nao instalado em nenhuma linha
     * @throws IllegalArgumentException se nulo, duplicado, ligado ou ja instalado
     */
    public void adicionarMaquina(Maquina maquina) {
        if (maquina == null || maquinas.contains(maquina)) {
            throw new IllegalArgumentException("Maquina obrigatoria e nao pode estar repetida na linha.");
        }
        maquina.vincularLinha(this);
        maquinas.add(maquina);
    }

    /**
     * Retira a maquina sem destruir seus componentes, permitindo reinstalacao.
     * @param maquina equipamento a retirar
     * @throws MaquinaIndisponivelException se a maquina estiver ligada ou ausente
     */
    public void removerMaquina(Maquina maquina) {
        if (!maquinas.contains(maquina) || maquina.estaLigada()) {
            throw new MaquinaIndisponivelException("So e possivel retirar uma maquina instalada e desligada.");
        }
        maquina.desvincularLinha(this);
        maquinas.remove(maquina);
    }

    /** @return quantidade instalada */
    public int getQuantidadeMaquinas() { return maquinas.size(); }

    /** @return custo de um disco que percorre todos os equipamentos */
    public double calcularCustoPorDisco() {
        double custo = 0;
        for (Maquina maquina : maquinas) { custo += maquina.getCustoOperacao(); }
        return custo;
    }

    /** @return menor capacidade dos equipamentos ou zero numa linha vazia */
    public int getCapacidadeMaxima() {
        int capacidade = maquinas.isEmpty() ? 0 : Integer.MAX_VALUE;
        for (Maquina maquina : maquinas) { capacidade = Math.min(capacidade, maquina.getCapacidadeMaxima()); }
        return capacidade;
    }

    /**
     * Configura o cenario de todas as maquinas.
     * @param cenario cenario nao nulo
     * @throws IllegalArgumentException se nulo
     */
    public void configurarCenario(Cenario cenario) {
        if (cenario == null) { throw new IllegalArgumentException("Cenario obrigatorio."); }
        for (Maquina maquina : maquinas) { maquina.configurarCenario(cenario); }
    }

    /** @return se existe ao menos uma maquina e nenhuma esta quebrada */
    public boolean estaDisponivel() {
        if (maquinas.isEmpty()) { return false; }
        for (Maquina maquina : maquinas) {
            if (maquina.getStatus() == StatusMaquina.QUEBRADA) { return false; }
        }
        return true;
    }

    /**
     * Valida antes de iniciar o lote, sem alterar o estado.
     * @throws MaquinaIndisponivelException se vazia ou com maquina quebrada
     */
    public void validarDisponibilidade() {
        if (maquinas.isEmpty()) {
            throw new MaquinaIndisponivelException("Nenhuma maquina instalada na linha " + nome + ".");
        }
        for (Maquina maquina : maquinas) {
            if (maquina.getStatus() == StatusMaquina.QUEBRADA) {
                throw new MaquinaIndisponivelException("Linha " + nome + " parada: " + maquina.getNome()
                        + " quebrou. Consulte a auditoria estrutural.");
            }
        }
    }

    /**
     * Liga os equipamentos depois de validar a linha inteira.
     * @throws MaquinaIndisponivelException se a linha nao puder operar
     */
    public void ligar() {
        validarDisponibilidade();
        for (Maquina maquina : maquinas) { maquina.ligar(); }
    }

    /** Desliga todos os equipamentos, inclusive apos interrupcao do lote. */
    public void desligar() {
        for (Maquina maquina : maquinas) { maquina.desligar(); }
    }

    /**
     * Conduz um disco pelas maquinas ligadas; para na primeira rejeicao.
     * O gerenciador reserva recursos para o percurso completo antes da chamada.
     * @param produto disco nao nulo
     * @return custo somente das operacoes efetivamente iniciadas
     * @throws IllegalArgumentException se o produto for nulo
     */
    public double processar(Produto produto) {
        if (produto == null) { throw new IllegalArgumentException("Produto obrigatorio."); }
        custoUltimoProcessamento = 0;
        for (Maquina maquina : maquinas) {
            if (!maquina.estaLigada()) { break; }
            custoUltimoProcessamento += maquina.getCustoOperacao();
            if (!maquina.processar(produto)) { break; }
        }
        return custoUltimoProcessamento;
    }

    /** @return custo ja iniciado no ultimo disco, mesmo se houve falha inesperada */
    public double getCustoUltimoProcessamento() { return custoUltimoProcessamento; }

    /** @return se alguma maquina ou parte precisa de revisao */
    @Override
    public boolean precisaManutencao() {
        for (Maquina maquina : maquinas) {
            if (maquina.precisaManutencao()) { return true; }
        }
        return false;
    }

    /** @return diagnostico indentado da linha, maquinas e todas as partes */
    @Override
    public String gerarRelatorioDiagnostico() {
        StringBuilder texto = new StringBuilder("Linha: " + nome + " | " + maquinas.size() + " maquina(s)");
        if (maquinas.isEmpty()) { texto.append(" | em montagem"); }
        for (Maquina maquina : maquinas) {
            texto.append("\n  ").append(maquina.gerarRelatorioDiagnostico().replace("\n", "\n  "));
        }
        return texto.toString();
    }
}
