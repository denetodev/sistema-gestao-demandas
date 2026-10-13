package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Area;
import dev.denetodev.sgd_api.entity.Demanda;
import dev.denetodev.sgd_api.entity.EscopoListagem;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.entity.PessoaDemanda;
import dev.denetodev.sgd_api.entity.StatusDemanda;
import dev.denetodev.sgd_api.service.support.BuscaTexto;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/** Peças da consulta de demandas. Cada uma devolve "sem restrição" quando o filtro não foi informado. */
public final class DemandaSpecs {

    private static final char ESCAPE = '\\';

    private DemandaSpecs() {
    }

    /** O que a pessoa pode ver em cada botão da lista (Minhas, Minha equipe, Meu núcleo, Todas). */
    public static Specification<Demanda> escopo(EscopoListagem escopo, Pessoa usuario, Area areaDaEquipe) {
        return switch (escopo) {
            case MINHAS -> comParticipanteAtivo((pd, cb) -> cb.equal(pd.get("pessoa").get("id"), usuario.getId()));
            case EQUIPE -> comParticipanteAtivo(
                    (pd, cb) -> cb.equal(pd.get("pessoa").get("area").get("id"), areaDaEquipe.getId()));
            case NUCLEO -> comParticipanteAtivo(
                    (pd, cb) -> cb.equal(pd.get("pessoa").get("area").get("nucleo").get("id"), usuario.getArea().getNucleo().getId()));
            case DIRETORIA -> (root, query, cb) ->
                    cb.equal(root.get("diretoria").get("id"), usuario.getArea().getDiretoria().getId());
            case TODAS -> Specification.unrestricted();
        };
    }

    /** Título ou código contém o texto, sem diferenciar maiúsculas nem acentos. */
    public static Specification<Demanda> busca(String texto) {
        String padrao = BuscaTexto.padraoLike(texto);
        if (padrao == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.or(
                cb.like(semAcento(root.get("titulo"), cb), padrao, ESCAPE),
                cb.like(semAcento(root.get("codigo"), cb), padrao, ESCAPE));
    }

    public static Specification<Demanda> status(StatusDemanda status) {
        return status == null ? Specification.unrestricted() : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /** Diretoria da demanda. */
    public static Specification<Demanda> diretoria(UUID diretoriaId) {
        return diretoriaId == null ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("diretoria").get("id"), diretoriaId);
    }

    /** Núcleo: alguém ativo de uma equipe desse núcleo está no time da demanda. */
    public static Specification<Demanda> nucleo(UUID nucleoId) {
        return nucleoId == null ? Specification.unrestricted()
                : comParticipanteAtivo((pd, cb) -> cb.equal(pd.get("pessoa").get("area").get("nucleo").get("id"), nucleoId));
    }

    /** Equipe: alguém ativo dessa área está no time da demanda. */
    public static Specification<Demanda> equipe(UUID areaId) {
        return areaId == null ? Specification.unrestricted()
                : comParticipanteAtivo((pd, cb) -> cb.equal(pd.get("pessoa").get("area").get("id"), areaId));
    }

    // ---- internos ----

    private interface CondicaoDoVinculo {
        Predicate para(Root<PessoaDemanda> vinculo, CriteriaBuilder cb);
    }

    private static Specification<Demanda> comParticipanteAtivo(CondicaoDoVinculo condicao) {
        return (root, query, cb) -> {
            Subquery<Integer> sub = query.subquery(Integer.class);
            Root<PessoaDemanda> pd = sub.from(PessoaDemanda.class);
            sub.select(cb.literal(1)).where(
                    cb.equal(pd.get("demanda"), root),
                    cb.isNull(pd.get("dataSaida")),
                    condicao.para(pd, cb));
            return cb.exists(sub);
        };
    }

    /** lower + translate das letras acentuadas: o mesmo que BuscaTexto faz no texto digitado. */
    private static Expression<String> semAcento(Expression<String> coluna, CriteriaBuilder cb) {
        return cb.function("translate", String.class,
                cb.lower(cb.coalesce(coluna, "")), cb.literal(BuscaTexto.ACENTOS), cb.literal(BuscaTexto.SEM_ACENTOS));
    }
}
