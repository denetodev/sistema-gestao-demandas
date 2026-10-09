package dev.denetodev.sgd_api.dto.response;

import dev.denetodev.sgd_api.entity.StatusDemanda;
import dev.denetodev.sgd_api.entity.VisaoDashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DashboardResponse(
        VisaoDashboard visao,
        String titulo,
        String mes,
        Resumo resumo,
        List<DiaAtividade> calendario,
        List<LinhaArea> porArea,
        List<LinhaPessoa> porPessoa
) {

    public record Resumo(
            int pessoas,
            Map<StatusDemanda, Long> demandasPorStatus,
            long demandasAtrasadas,
            long atividadesNoMes,
            BigDecimal valorNoMes,
            BigDecimal valorNoAno
    ) {
    }

    /** Dia com pelo menos uma atividade, dentro dos últimos 12 meses. */
    public record DiaAtividade(LocalDate data, long quantidade) {
    }

    public record LinhaArea(
            UUID areaId,
            String area,
            int pessoas,
            long demandasAtivas,
            long atividadesNoMes,
            long demandasAtrasadas,
            BigDecimal valorNoMes
    ) {
    }

    public record LinhaPessoa(
            UUID pessoaId,
            String nome,
            String area,
            String diretoria,
            long demandasAtivas,
            long atividadesNoMes,
            BigDecimal valorNoMes
    ) {
    }
}
