/** Motor que fornece movimento aos mecanismos de uma maquina de vinil. */
final class MotorAcionamento implements Auditavel {
    private double saude = 100.0;
    private boolean ligado;

    void ligar() { ligado = saude > 0; }

    void desligar() { ligado = false; }

    boolean estaLigado() { return ligado; }

    double getSaude() { return saude; }

    void desgastar(double desgaste) {
        if (!Double.isFinite(desgaste) || desgaste < 0) {
            throw new IllegalArgumentException("Desgaste do motor deve ser finito e nao negativo.");
        }
        saude = Math.max(0, saude - desgaste);
        if (saude == 0) { desligar(); }
    }

    @Override
    public boolean precisaManutencao() { return saude < 30; }

    @Override
    public String gerarRelatorioDiagnostico() {
        return String.format("Motor de acionamento: saude %.1f/100 | %s", saude,
                saude == 0 ? "QUEBRADO" : ligado ? "ligado" : "desligado");
    }
}
