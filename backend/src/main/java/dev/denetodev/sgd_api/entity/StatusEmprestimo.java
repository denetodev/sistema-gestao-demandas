package dev.denetodev.sgd_api.entity;

/** Atrasado não é um status: é RETIRADO com a devolução prevista já vencida (calculado). */
public enum StatusEmprestimo {
    AGUARDANDO_AUTORIZACAO, AUTORIZADO, RECUSADO, RETIRADO, DEVOLVIDO, CANCELADO;

    /** Ocupa os equipamentos: eles não podem ser pedidos de novo enquanto o empréstimo estiver nestes estados. */
    public boolean reservaEquipamentos() {
        return this == AGUARDANDO_AUTORIZACAO || this == AUTORIZADO || this == RETIRADO;
    }
}
