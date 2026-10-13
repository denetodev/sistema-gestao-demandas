package dev.denetodev.sgd_api.security;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Confere no Supabase Auth se a conta já confirmou o e-mail. Com a opção "Confirm email" ligada o Supabase nem emite
 * sessão antes da confirmação; esta checagem é uma segunda barreira, independente dessa configuração.
 * O backend conecta como dono do banco, então consegue ler auth.users.
 */
@Component
public class ConfirmacaoEmail {

    private final EntityManager entityManager;

    public ConfirmacaoEmail(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public boolean confirmado(UUID authUserId) {
        List<?> resultado = entityManager
                .createNativeQuery("select (email_confirmed_at is not null) from auth.users where id = :id")
                .setParameter("id", authUserId)
                .getResultList();
        return !resultado.isEmpty() && Boolean.TRUE.equals(resultado.get(0));
    }
}
