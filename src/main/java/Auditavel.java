/**
 * Contrato comum para diagnosticar equipamentos e discos.
 *
 * <p>{@link Maquina} e {@link Produto} pertencem a hierarquias diferentes, mas
 * ambos sao auditaveis. Isso permite que o gerenciador percorra uma unica
 * colecao de {@code Auditavel} e trate os dois tipos de forma polimorfica.</p>
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 */
public interface Auditavel {
    /**
     * Monta uma linha de diagnostico legivel para o relatorio de auditoria.
     *
     * @return texto com o estado atual do componente
     */
    String gerarRelatorioDiagnostico();

    /**
     * Indica se o componente exige intervencao: manutencao, no caso de uma
     * maquina, ou revisao, no caso de um disco.
     *
     * @return {@code true} se ha um alerta a mostrar na auditoria
     */
    boolean precisaManutencao();
}
