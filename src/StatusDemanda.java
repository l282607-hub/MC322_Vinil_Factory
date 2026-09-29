public enum StatusDemanda {
    PENDENTE("Pendente"), EM_PRODUCAO("Em producao"),
    CONCLUIDA("Concluida"), CANCELADA("Cancelada");

    private final String descricao;

    StatusDemanda(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
