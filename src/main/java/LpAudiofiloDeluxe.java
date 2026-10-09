/**
 * LP duplo de 180g para audiofilos. Maior qualidade (0.9), maior consumo de PVC e inspecao mais rigorosa.
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 */
public class LpAudiofiloDeluxe extends Produto {
    /** Nome do formato, usado nos pedidos e no menu. */
    public static final String TIPO = "LP Audiofilo Deluxe";
    /** Quilogramas de PVC consumidos por disco. */
    public static final double PVC_POR_UNIDADE = 0.40;
    /** Qualidade do formato; quanto maior, mais rigorosa a inspecao. */
    public static final double QUALIDADE = 0.9;

    /** Cria um disco deste formato, com os valores das constantes da classe. */
    public LpAudiofiloDeluxe() {
        super(TIPO, PVC_POR_UNIDADE, QUALIDADE);
    }

    /** Marca o disco como {@link StatusProduto#PRENSADO}. */
    @Override
    public void processar() {
        setStatus(StatusProduto.PRENSADO);
    }

    /**
     * Informa o tempo de producao deste formato.
     *
     * @return 6.0 minutos
     */
    @Override
    public double calcularTempoProducao() {
        return 6.0;
    }

    /**
     * Informa o tipo do disco.
     *
     * @return o valor de {@link #TIPO}
     */
    @Override
    public String getTipo() {
        return TIPO;
    }
}
