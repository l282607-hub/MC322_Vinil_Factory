/**
 * Problema com um pedido de gravadora: o tipo de disco nao existe, a
 * quantidade e invalida, nao ha pedido pendente ou a operacao pedida e
 * incompativel com o estado atual do pedido (por exemplo, produzir um pedido
 * ja concluido ou cancelado).
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 * @see Demanda
 * @see StatusDemanda
 */
public class DemandaInvalidaException extends PlantaException {
    private static final long serialVersionUID = 1L;

    /**
     * Cria a excecao com uma mensagem destinada ao usuario final.
     *
     * @param mensagem explicacao do problema com o pedido
     */
    public DemandaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
