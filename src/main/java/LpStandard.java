/**
 * LP simples de 12 polegadas (140g). Qualidade media (0.7) e consumo medio de PVC.
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 */
public class LpStandard extends Produto {
    /** Nome do formato, usado nos pedidos e no menu. */
    public static final String TIPO = "LP Standard 12 Polegadas";
    /** Quilogramas de PVC consumidos por disco. */
    public static final double PVC_POR_UNIDADE = 0.20;
    /** Qualidade do formato; quanto maior, mais rigorosa a inspecao. */
    public static final double QUALIDADE = 0.7;

    /** Cria um disco deste formato, com os valores das constantes da classe. */
    public LpStandard() {
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
     * @return 3.0 minutos
     */
    @Override
    public double calcularTempoProducao() {
        return 3.0;
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
