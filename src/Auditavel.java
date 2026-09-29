/** Contrato comum para diagnosticar equipamentos e discos. */
public interface Auditavel {
    String gerarRelatorioDiagnostico();
    boolean precisaManutencao();
}
