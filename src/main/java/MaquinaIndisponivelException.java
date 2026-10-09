/**
 * A linha de producao nao pode operar: nao ha maquinas instaladas ou alguma
 * maquina esta {@link StatusMaquina#QUEBRADA}. Neste projeto o reparo nao e
 * implementado, entao uma maquina quebrada permanece parada.
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 * @see Maquina
 */
public class MaquinaIndisponivelException extends PlantaException {
    private static final long serialVersionUID = 1L;

    /**
     * Cria a excecao com uma mensagem destinada ao usuario final.
     *
     * @param mensagem explicacao de qual maquina esta indisponivel e por que
     */
    public MaquinaIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
