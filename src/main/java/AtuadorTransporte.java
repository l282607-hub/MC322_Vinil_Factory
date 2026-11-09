/** Um dos mecanismos que posicionam e movimentam o disco dentro da maquina. */
final class AtuadorTransporte implements Auditavel {
    private final int numero;
    private double integridade = 100.0;

    AtuadorTransporte(int numero) {
        if (numero <= 0) { throw new IllegalArgumentException("Numero do atuador deve ser positivo."); }
        this.numero = numero;
    }

    void desgastar(double desgaste) {
        if (!Double.isFinite(desgaste) || desgaste < 0) {
            throw new IllegalArgumentException("Desgaste do atuador deve ser finito e nao negativo.");
        }
        integridade = Math.max(0, integridade - desgaste);
    }

    boolean estaOperante() { return integridade > 0; }

    double getIntegridade() { return integridade; }

    @Override
    public boolean precisaManutencao() { return integridade < 30; }

    @Override
    public String gerarRelatorioDiagnostico() {
        return String.format("Atuador de transporte %d: integridade %.1f/100 | %s", numero, integridade,
                !estaOperante() ? "TRAVADO" : precisaManutencao() ? "revisar" : "OK");
    }
}
