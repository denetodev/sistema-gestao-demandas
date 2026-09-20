-- Perfil de acesso da pessoa, usado pelo Spring Security (M3 - autenticação).
alter table pessoa
    add column perfil varchar(20) not null default 'PROFISSIONAL'
        check (perfil in ('ADMIN', 'GESTOR', 'PROFISSIONAL', 'VISUALIZADOR'));