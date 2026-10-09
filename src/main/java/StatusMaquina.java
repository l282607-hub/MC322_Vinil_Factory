/**
 * Estado operacional de uma {@link Maquina}.
 *
 * @author Jeorde Antonio
 * @author Leo Bertoli
 */
public enum StatusMaquina {
    /** Parada, pronta para ser ligada. */
    DESLIGADA,
    /** Em operacao, aceitando discos. */
    LIGADA,
    /** Saude zerada; nao pode ser ligada nem operar. */
    QUEBRADA
}
