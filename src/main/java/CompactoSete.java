/**
 * Compacto de 7 polegadas (single). Qualidade basica (0.5), menor consumo de PVC e inspecao mais flexivel.
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 */
public class CompactoSete extends Produto {
    /** Nome do formato, usado nos pedidos e no menu. */
    public static final String TIPO = "Compacto 7 Polegadas";
    /** Quilogramas de PVC consumidos por disco. */
    public static final double PVC_POR_UNIDADE = 0.10;
    /** Qualidade do formato; quanto maior, mais rigorosa a inspecao. */
    public static final double QUALIDADE = 0.5;

    /** Cria um disco deste formato, com os valores das constantes da classe. */
    public CompactoSete() {
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
     * @return 1.5 minutos
     */
    @Override
    public double calcularTempoProducao() {
        return 1.5;
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
