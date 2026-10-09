/**
 * Falta de materia-prima (PVC) ou de budget para executar uma operacao.
 *
 * <p>Alem da mensagem, guarda o recurso, a quantidade necessaria e a
 * disponivel, para que quem trata a excecao possa mostrar ou usar os numeros
 * sem interpretar o texto.</p>
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 * @see MateriaPrima
 * @see GerenciadorProducao#comprarMateriaPrima(double)
 */
public class RecursoInsuficienteException extends PlantaException {
    private static final long serialVersionUID = 1L;

    /** Nome do recurso que faltou ("PVC" ou "budget"). */
    private final String recurso;
    /** Quantidade que a operacao exigia, na unidade do recurso. */
    private final double necessario;
    /** Quantidade que existia no momento da tentativa. */
    private final double disponivel;

    /**
     * Cria a excecao descrevendo o recurso que faltou.
     *
     * @param mensagem   explicacao destinada ao usuario final
     * @param recurso    nome do recurso, por exemplo "PVC" ou "budget"
     * @param necessario quantidade exigida pela operacao
     * @param disponivel quantidade existente no momento da tentativa
     */
    public RecursoInsuficienteException(String mensagem, String recurso,
            double necessario, double disponivel) {
        super(mensagem);
        this.recurso = recurso;
        this.necessario = necessario;
        this.disponivel = disponivel;
    }

    /**
     * Informa qual recurso faltou.
     *
     * @return nome do recurso ("PVC" ou "budget")
     */
    public String getRecurso() {
        return recurso;
    }

    /**
     * Informa quanto a operacao exigia.
     *
     * @return quantidade necessaria, na unidade do recurso
     */
    public double getNecessario() {
        return necessario;
    }

    /**
     * Informa quanto havia disponivel na tentativa.
     *
     * @return quantidade disponivel, na unidade do recurso
     */
    public double getDisponivel() {
        return disponivel;
    }
}
