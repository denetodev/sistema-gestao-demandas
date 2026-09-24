import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { MessageService } from 'primeng/api';

export const erroHttpInterceptor: HttpInterceptorFn = (req, next) => {
    const messageService = inject(MessageService);

    return next(req).pipe(
        catchError((erro: HttpErrorResponse) => {
            // /pessoas/me tem tratamento próprio no shell, não vira toast
            if (!req.url.endsWith('/pessoas/me')) {
                messageService.add({
                    severity: 'error',
                    summary: tituloPorStatus(erro.status),
                    detail: detalhePorStatus(erro),
                    life: 6000,
                });
            }
            return throwError(() => erro);
        }),
    );
};

function tituloPorStatus(status: number): string {
    switch (status) {
        case 0: return 'Sem conexão';
        case 400: return 'Dados inválidos';
        case 401: return 'Sessão expirada';
        case 403: return 'Sem permissão';
        case 404: return 'Não encontrado';
        case 409: return 'Conflito';
        default: return 'Erro';
    }
}

function detalhePorStatus(erro: HttpErrorResponse): string {
    if (erro.status === 0) return 'Não foi possível falar com o servidor.';
    if (erro.status === 403) return 'Você não tem permissão para esta ação.';
    if (erro.status === 409) return erro.error?.message ?? 'Este registro está em uso e não pode ser alterado.';
    if (erro.status === 400) return erro.error?.message ?? 'Verifique os campos preenchidos.';
    return erro.error?.message ?? 'Tente novamente em instantes.';
}