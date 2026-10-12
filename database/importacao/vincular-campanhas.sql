-- database/importacao/vincular-campanhas.sql
-- Cria as campanhas das planilhas importadas e vincula as demandas a elas.
--
-- As planilhas não trazem campanha. Aqui ela é inferida do título da demanda: o assunto que se repete
-- em várias demandas (por exemplo "BBCast Agro", "Desenrola Brasil", "BBBE Livelo") vira uma campanha.
-- Os nomes foram aprovados pelo Neto. Demandas que não casam com nenhuma regra ficam sem campanha.
--
-- Idempotente: rodar de novo não duplica campanha e só vincula demanda que ainda está SEM campanha,
-- então nunca sobrescreve uma escolha feita à mão no sistema.
-- Rode DEPOIS de importar (reprocessar a importação recria as demandas e o vínculo se perde).
--
-- Reversão: update demanda set campanha_id = null where campanha_id in
--   (select id from campanha where nome in (<nomes abaixo>));  e depois delete from campanha dos mesmos nomes.
--
-- Regras: prioridade menor vence quando um título casa com mais de uma. Regex sem diferenciar maiúsculas.

begin;

create temporary table regra_campanha (
    prio  int  not null,
    fonte text not null,   -- demanda.source_system
    nome  text not null,   -- nome da campanha
    rx    text not null    -- regex aplicada ao título da demanda
) on commit drop;

insert into regra_campanha (prio, fonte, nome, rx) values
 -- COE/CRM (planilha House). "CAM #n.n" é a squad, não a campanha.
 (1,  'planilha-house-crm', 'BBBE Benefícios Digitais',   'bbbe[ _]benef[ií]cios[ _]digitais'),
 (2,  'planilha-house-crm', 'BBBE Smiles',                'bbbe[ _]smiles'),
 (3,  'planilha-house-crm', 'BBBE Troca Fácil',           'bbbe[ _]troca[ _]f[aá]cil'),
 (4,  'planilha-house-crm', 'BBBE Livelo',                'bbbe[ _]livelo'),
 (5,  'planilha-house-crm', 'BBBE LATAM Pass',            'bbbe[ _]latam'),
 (6,  'planilha-house-crm', 'Desenrola Brasil',           'desenrola'),
 (7,  'planilha-house-crm', 'SHOPP BB',                   'shopp[ _]bb'),
 (8,  'planilha-house-crm', 'APP 5.0',                    'app[ _]5\.0'),
 (9,  'planilha-house-crm', 'Posicionamento BB Empresas', 'posicionamento'),
 (10, 'planilha-house-crm', 'Torcida BB Copa 2026',       'torcida[ _]bb'),
 (11, 'planilha-house-crm', 'Semana do Cliente',          'semana[ _]do[ _]cliente'),
 (12, 'planilha-house-crm', 'Migração de agências',       'migra[cç][ãa]o[ _]de[ _]ag[eê]ncias'),
 (13, 'planilha-house-crm', 'Exclusivo Conceito',         'exclusivo[ _]conceito'),
 (14, 'planilha-house-crm', 'EMC Imob',                   'emc[ _]imob'),
 -- UGR (planilha Multimídia). Título no formato data_ação_assunto.
 (1,  'planilha-multimidia-ugr', 'BBCast Agro',                  'bb[_ ]?cast[_ ]?agro'),
 (2,  'planilha-multimidia-ugr', 'Onde Investir',                'onde[_ ]investir'),
 (3,  'planilha-multimidia-ugr', 'PodCashBB',                    'pod[_ ]?cash[_ ]?bb'),
 (4,  'planilha-multimidia-ugr', 'Podcast Co.labbs',             'co\.labbs'),
 (5,  'planilha-multimidia-ugr', 'Podcast Summit',               'summit'),
 (6,  'planilha-multimidia-ugr', 'Trilhas de Conhecimento',      'trilhas|de_conhecimento'),
 (7,  'planilha-multimidia-ugr', 'COPOM',                        'copom'),
 (8,  'planilha-multimidia-ugr', 'Livecast',                     'livecast'),
 (9,  'planilha-multimidia-ugr', 'Gestão do Risco de Reputação', 'reputa[cç]'),
 (10, 'planilha-multimidia-ugr', 'Plano Safra',                  'plano[_ ]safra'),
 (11, 'planilha-multimidia-ugr', 'Invest Talking',               'talking'),
 (12, 'planilha-multimidia-ugr', 'Resultado 1T26',               'resultado[_ ]?1t26'),
 (13, 'planilha-multimidia-ugr', 'Espaço Lentes',                'lentes'),
 (14, 'planilha-multimidia-ugr', 'Podcast Super ADM',            'super[_ ]?adm'),
 (15, 'planilha-multimidia-ugr', 'ASG CODEI',                    '(^|[_ ])codei($|[_ ])'),
 (16, 'planilha-multimidia-ugr', 'Pautas AGN',                   'agn'),
 (17, 'planilha-multimidia-ugr', 'MP 1376',                      'mp[_ ]?1376|medida[_ ]provis'),
 (18, 'planilha-multimidia-ugr', 'Vôlei Sede BB',                'v[ôo]lei'),
 (19, 'planilha-multimidia-ugr', 'BB Private',                   'bb[_ ]?private'),
 (20, 'planilha-multimidia-ugr', 'Minuto Legal',                 'minuto[_ ]legal'),
 (21, 'planilha-multimidia-ugr', 'Drops USI',                    'drops[_ ]usi'),
 (22, 'planilha-multimidia-ugr', 'Presidência',                  'presi');

-- 1) campanhas que ainda não existem (pelo nome; sem projeto, sem datas)
insert into campanha (nome)
select distinct r.nome
from regra_campanha r
where not exists (select 1 from campanha c where c.nome = r.nome);

-- 2) vínculo: só demandas importadas ainda sem campanha; a regra de menor prioridade vence
update demanda d
set campanha_id = c.id
from (
    select distinct on (dm.id) dm.id as demanda_id, r.nome
    from demanda dm
    join regra_campanha r on r.fonte = dm.source_system and dm.titulo ~* r.rx
    where dm.campanha_id is null
    order by dm.id, r.prio
) m
join campanha c on c.nome = m.nome
where d.id = m.demanda_id;

-- conferência
select c.nome as campanha, count(d.id) as demandas
from campanha c
join demanda d on d.campanha_id = c.id
group by c.nome
order by demandas desc, c.nome;

commit;
