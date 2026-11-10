-- Data de início da demanda (opcional). O prazo de entrega (data_prazo) continua existindo e é ele que marca a demanda como atrasada.
alter table demanda
    add column data_inicio date;
