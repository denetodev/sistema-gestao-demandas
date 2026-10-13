package dev.denetodev.sgd_api.security;

import dev.denetodev.sgd_api.exception.TentativasExcedidasException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite simples de tentativas por chave (por exemplo, a conta que envia o cadastro com chave C), em janela deslizante.
 * Fica em memória: vale para uma instância só (Railway com um serviço). Se o backend passar a ter várias
 * instâncias, o limite vira por instância e este componente deve ir para o banco ou um cache compartilhado.
 */
@Component
public class LimitadorTentativas {

    private static final int LIMITE_DE_CHAVES = 10_000;

    private final int maximo;
    private final Duration janela;
    private final Clock relogio;
    private final Map<String, Deque<Instant>> tentativas = new ConcurrentHashMap<>();

    @Autowired
    public LimitadorTentativas(
            @Value("${app.limite.cadastro.maximo:5}") int maximo,
            @Value("${app.limite.cadastro.janela-minutos:60}") long janelaMinutos
    ) {
        this(maximo, Duration.ofMinutes(janelaMinutos), Clock.systemUTC());
    }

    public LimitadorTentativas(int maximo, Duration janela, Clock relogio) {
        this.maximo = maximo;
        this.janela = janela;
        this.relogio = relogio;
    }

    /** Conta mais uma tentativa e recusa se a chave já esgotou o limite na janela. */
    public void registrar(String chave) {
        Instant agora = relogio.instant();
        if (tentativas.size() > LIMITE_DE_CHAVES) {
            tentativas.values().removeIf(fila -> fila.isEmpty() || fila.peekLast().isBefore(agora.minus(janela)));
        }
        Deque<Instant> fila = tentativas.computeIfAbsent(chave, k -> new ArrayDeque<>());
        synchronized (fila) {
            while (!fila.isEmpty() && fila.peekFirst().isBefore(agora.minus(janela))) {
                fila.pollFirst();
            }
            if (fila.size() >= maximo) {
                throw new TentativasExcedidasException("Muitas tentativas. Aguarde um pouco e tente novamente.");
            }
            fila.addLast(agora);
        }
    }
}
