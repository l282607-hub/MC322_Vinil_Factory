package br.unicamp.ic.mc322.exception;

/** Falha de leitura, escrita ou validacao do arquivo da planta. */
public class PersistenciaException extends PlantaException {
    private static final long serialVersionUID = 1L;

    /**
     * Mantem uma mensagem compreensivel e a causa para diagnostico.
     * @param mensagem explicacao destinada ao usuario
     * @param causa erro original
     */
    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem);
        initCause(causa);
    }
}
