package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.response.NotificacaoResponse;
import dev.denetodev.sgd_api.dto.response.NotificacaoResponse.Item;
import dev.denetodev.sgd_api.dto.response.NotificacaoResponse.Tipo;
import dev.denetodev.sgd_api.dto.response.ResumoEmprestimosResponse;
import dev.denetodev.sgd_api.entity.PerfilPessoa;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.entity.PessoaDemanda;
import dev.denetodev.sgd_api.entity.StatusPessoa;
import dev.denetodev.sgd_api.repository.PessoaDemandaRepository;
import dev.denetodev.sgd_api.repository.PessoaRepository;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import dev.denetodev.sgd_api.service.support.EquipamentoPermissao;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Avisos de cada pessoa, calculados na hora (nada é guardado além de "até quando já vi"):
 * quem aprova cadastros vê quantos esperam, quem autoriza empréstimo vê os pedidos, quem opera o estúdio vê os
 * atrasados, e qualquer pessoa vê quando alguém a coloca no time de uma demanda.
 */
@Service
@Transactional
public class NotificacaoService {

    /** Por quantos dias um "te adicionaram" continua na lista. */
    static final int DIAS_DE_HISTORICO = 30;

    private final CurrentPessoaResolver currentPessoaResolver;
    private final PessoaRepository pessoaRepository;
    private final PessoaDemandaRepository pessoaDemandaRepository;
    private final EmprestimoService emprestimoService;
    private final Clock clock;

    public NotificacaoService(CurrentPessoaResolver currentPessoaResolver, PessoaRepository pessoaRepository,
                              PessoaDemandaRepository pessoaDemandaRepository, EmprestimoService emprestimoService,
                              Clock clock) {
        this.currentPessoaResolver = currentPessoaResolver;
        this.pessoaRepository = pessoaRepository;
        this.pessoaDemandaRepository = pessoaDemandaRepository;
        this.emprestimoService = emprestimoService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public NotificacaoResponse listar(Jwt jwt) {
        return listar(currentPessoaResolver.resolver(jwt));
    }

    NotificacaoResponse listar(Pessoa usuario) {
        List<Item> itens = new ArrayList<>();

        if (usuario.getPerfil() == PerfilPessoa.ADMIN || usuario.getPerfil() == PerfilPessoa.GESTOR) {
            long pendentes = pessoaRepository.findByAprovadoEmIsNullAndStatus(StatusPessoa.ATIVO).size();
            if (pendentes > 0) {
                itens.add(new Item("cadastros", Tipo.CADASTROS_PENDENTES, "Cadastros para aprovar",
                        plural(pendentes, "cadastro aguarda", "cadastros aguardam") + " a sua decisão.",
                        "/pessoas", pendentes, false, null));
            }
        }

        if (EquipamentoPermissao.opera(usuario)) {
            ResumoEmprestimosResponse resumo = emprestimoService.contar();
            if (EquipamentoPermissao.gerencia(usuario) && resumo.aguardandoAutorizacao() > 0) {
                String externos = resumo.aguardandoExterno() > 0
                        ? " (" + resumo.aguardandoExterno() + " para fora do prédio)" : "";
                itens.add(new Item("emprestimos-aguardando", Tipo.EMPRESTIMOS_AGUARDANDO, "Empréstimos para autorizar",
                        plural(resumo.aguardandoAutorizacao(), "pedido espera", "pedidos esperam") + " autorização" + externos + ".",
                        "/equipamentos", resumo.aguardandoAutorizacao(), false, null));
            }
            if (resumo.atrasados() > 0) {
                itens.add(new Item("emprestimos-atrasados", Tipo.EMPRESTIMOS_ATRASADOS, "Empréstimos atrasados",
                        plural(resumo.atrasados(), "equipamento passou", "equipamentos passaram") + " da data de devolução.",
                        "/equipamentos", resumo.atrasados(), false, null));
            }
        }

        OffsetDateTime agora = OffsetDateTime.now(clock);
        OffsetDateTime vistas = usuario.getNotificacoesVistasEm();
        for (PessoaDemanda v : pessoaDemandaRepository
                .findTop20ByPessoaIdAndAdicionadoEmAfterAndDataSaidaIsNullOrderByAdicionadoEmDesc(
                        usuario.getId(), agora.minusDays(DIAS_DE_HISTORICO))) {
            Pessoa quem = v.getAdicionadoPor();
            if (quem == null || quem.getId().equals(usuario.getId())) {
                continue; // quem entra na própria demanda não precisa de aviso
            }
            boolean lida = vistas != null && !v.getAdicionadoEm().isAfter(vistas);
            itens.add(new Item("demanda-" + v.getId(), Tipo.ADICIONADO_EM_DEMANDA, "Você entrou numa demanda",
                    quem.getNome() + " te adicionou em " + v.getDemanda().getTitulo() + ".",
                    "/demandas/" + v.getDemanda().getId(), 1, lida, v.getAdicionadoEm()));
        }

        long naoLidas = itens.stream().filter(i -> !i.lida()).mapToLong(Item::quantidade).sum();
        return new NotificacaoResponse(itens, naoLidas);
    }

    /** A pessoa abriu o sininho: os "te adicionaram" até agora passam a lidos. */
    public void marcarVistas(Jwt jwt) {
        currentPessoaResolver.resolver(jwt).setNotificacoesVistasEm(OffsetDateTime.now(clock));
    }

    private static String plural(long n, String singular, String plural) {
        return n + " " + (n == 1 ? singular : plural);
    }
}
