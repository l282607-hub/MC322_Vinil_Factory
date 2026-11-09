/** Sensor que confere o posicionamento do disco durante uma operacao. */
final class SensorAlinhamento implements Auditavel {
    private double calibracao = 100.0;

    void desgastar(double desgaste) {
        if (!Double.isFinite(desgaste) || desgaste < 0) {
            throw new IllegalArgumentException("Desgaste do sensor deve ser finito e nao negativo.");
        }
        calibracao = Math.max(0, calibracao - desgaste);
    }

    boolean estaOperante() { return calibracao > 0; }

    double getCalibracao() { return calibracao; }

    double calcularRiscoDesalinhamento() { return (100 - calibracao) / 100 * 0.05; }

    @Override
    public boolean precisaManutencao() { return calibracao < 30; }

    @Override
    public String gerarRelatorioDiagnostico() {
        return String.format("Sensor de alinhamento: calibracao %.1f/100 | %s", calibracao,
                !estaOperante() ? "SEM LEITURA" : precisaManutencao() ? "recalibrar" : "OK");
    }
}
