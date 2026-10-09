/**
 * Raiz das excecoes de dominio da GroovePress Vinyl Works.
 *
 * <p>Representa uma regra da planta que impediu uma operacao: pedido
 * inexistente, falta de PVC ou de budget, linha parada, valor invalido.
 * Por convencao do projeto, o {@link GerenciadorProducao} so lanca uma
 * {@code PlantaException} <strong>antes</strong> de alterar estoque, budget,
 * pedidos ou maquinas. Quem captura pode, portanto, informar o usuario e
 * oferecer outra operacao sem se preocupar com estado parcial.</p>
 *
 * <p>E uma excecao nao verificada (herda de {@link RuntimeException}) para que
 * as regras de negocio nao poluam as assinaturas com {@code throws}.</p>
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 * @see DemandaInvalidaException
 * @see RecursoInsuficienteException
 * @see MaquinaIndisponivelException
 * @see EntradaInvalidaException
 */
public class PlantaException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Cria a excecao com uma mensagem destinada ao usuario final.
     *
     * @param mensagem explicacao do que aconteceu e, se possivel, como corrigir
     */
    public PlantaException(String mensagem) {
        super(mensagem);
    }
}
