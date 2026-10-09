/**
 * Valor fornecido pelo usuario que nao pode ser aceito: texto onde se esperava
 * numero, opcao fora do menu, quantidade negativa, NaN ou infinito.
 *
 * <p>Distingue erro de <em>entrada</em> (o usuario digitou algo invalido) de
 * erro de <em>regra de negocio</em> (a entrada era valida, mas a planta nao
 * pode executa-la).</p>
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 * @see LeitorConsole
 */
public class EntradaInvalidaException extends PlantaException {
    private static final long serialVersionUID = 1L;

    /**
     * Cria a excecao com uma mensagem destinada ao usuario final.
     *
     * @param mensagem explicacao do valor invalido e do que e esperado
     */
    public EntradaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
