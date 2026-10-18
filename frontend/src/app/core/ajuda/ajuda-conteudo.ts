/**
 * Textos de ajuda de cada tela, em formato de pergunta e resposta curta.
 *
 * É o único lugar para revisar e ajustar a linguagem do sistema: o componente <app-ajuda> só
 * mostra o que está aqui. Cada tela usa uma chave (ver ChaveAjuda).
 * Rascunho inicial: o padrão de preenchimento é definido pela equipe, então edite à vontade.
 */

export interface ItemAjuda {
  pergunta: string;
  resposta: string;
}

export interface Ajuda {
  /** Título da faixa recolhível. */
  titulo: string;
  itens: ItemAjuda[];
}

export type ChaveAjuda =
  | 'dashboard'
  | 'demandas'
  | 'demanda-detalhe'
  | 'demandantes'
  | 'projetos'
  | 'campanhas'
  | 'campanha-detalhe'
  | 'tipos-peca'
  | 'pessoas'
  | 'estrutura'
  | 'cargos'
  | 'relatorio'
  | 'emprestimos'
  | 'inventario'
  | 'agenda';

export const AJUDA: Record<ChaveAjuda, Ajuda> = {
  dashboard: {
    titulo: 'Como ler este painel?',
    itens: [
      {
        pergunta: 'Quanto trabalho eu registrei ao longo do ano?',
        resposta:
          'O mapa de atividades mostra um quadrado por dia. Quanto mais escuro, mais atividades foram registradas naquele dia.',
      },
      {
        pergunta: 'O que é o "valor gerado"?',
        resposta:
          'É a soma de quantidade × valor unitário de todas as peças produzidas. É uma referência de mercado (quanto esse trabalho custaria), não é salário nem custo real. Demandas canceladas ficam de fora.',
      },
      {
        pergunta: 'Quando uma demanda conta como atrasada?',
        resposta:
          'Quando o prazo já passou e ela ainda não está concluída nem cancelada. Demandas sem prazo nunca aparecem como atrasadas.',
      },
      {
        pergunta: 'Por que vejo só os meus números?',
        resposta:
          'O painel respeita o seu perfil. Todo profissional vê os próprios números. Quem é Referência de equipe vê também a equipe da sua área. Gestor e Admin veem por diretoria e por pessoa.',
      },
    ],
  },

  demandas: {
    titulo: 'Como funcionam as demandas?',
    itens: [
      {
        pergunta: 'O que é uma demanda?',
        resposta:
          'Um pedido de trabalho que chegou até você ou sua equipe: um e-mail marketing, um podcast, uma sessão de fotos. Tudo o que você faz (atividades) e entrega (peças) fica registrado dentro de uma demanda.',
      },
      {
        pergunta: 'Qual a diferença entre Minhas, Minha equipe, Meu núcleo, Minha diretoria e Todas?',
        resposta:
          'Minhas: demandas em que você participa. Minha equipe: as que têm alguém da sua equipe no time. Meu núcleo: as que têm alguém do seu núcleo. Minha diretoria: todas da sua diretoria. Todas: o sistema inteiro (só Gestor e Admin).',
      },
      {
        pergunta: 'O que significam os status?',
        resposta:
          'Não iniciada: ainda não começou. Em andamento: está sendo produzida. Em aprovação: foi entregue e aguarda o retorno de quem pediu. Concluída: finalizada. Cancelada: não vai mais acontecer (fica só para consulta).',
      },
      {
        pergunta: 'Preciso preencher projeto e campanha?',
        resposta:
          'Não, são opcionais. Preencha quando a demanda fizer parte de uma ação maior, para depois conseguir enxergar o conjunto.',
      },
    ],
  },

  'demanda-detalhe': {
    titulo: 'O que registrar nesta demanda?',
    itens: [
      {
        pergunta: 'Qual a diferença entre atividade e peça?',
        resposta:
          'Atividade é o trabalho que você fez, com a data (por exemplo: criei o layout, editei o vídeo). Peça é o que foi entregue, com tipo e quantidade (por exemplo: 3 e-mails). É a peça que gera valor, pela quantidade vezes o valor unitário do tipo.',
      },
      {
        pergunta: 'Por que o valor da peça não muda quando o tipo de peça muda?',
        resposta:
          'O valor unitário é congelado no momento do lançamento. Assim o que foi entregue no passado continua com o valor da época.',
      },
      {
        pergunta: 'Para que servem os participantes e os papéis?',
        resposta:
          'Mostram quem está envolvido e com qual responsabilidade. Quem cria a demanda entra como Responsável principal. Só o Responsável principal, o Gestor ou o Admin cancelam a demanda.',
      },
      {
        pergunta: 'Quem pode alterar esta demanda?',
        resposta:
          'Quem participa dela, a Referência da área de alguém do time, o Gestor e o Admin. Isso vale para editar, mudar o status e lançar atividades e peças. Se você precisa mexer e não está no time, use o botão "Participar desta demanda" no topo da página.',
      },
      {
        pergunta: 'O que é uma evidência?',
        resposta:
          'A prova do que foi feito: uma imagem (JPEG ou PNG, até 5 MB) ou um link para o material. Anexe em uma atividade ou peça para o trabalho poder ser conferido depois.',
      },
      {
        pergunta: 'Onde vejo quem mudou o status e quando?',
        resposta:
          'No histórico da demanda. Cada mudança de status registra quem fez, quando e o motivo informado.',
      },
    ],
  },

  cargos: {
    titulo: 'Para que servem os cargos?',
    itens: [
      {
        pergunta: 'O que é um cargo?',
        resposta:
          'A função da pessoa no time, como Designer, Operador ou Videomaker. Aparece no cadastro da pessoa e no rodapé do menu.',
      },
      {
        pergunta: 'O cargo depende da equipe ou do núcleo?',
        resposta:
          'Não. O cargo é uma lista única e solta: qualquer pessoa pode ter qualquer cargo, em qualquer equipe.',
      },
      {
        pergunta: 'Posso desativar um cargo?',
        resposta:
          'Só quando nenhuma pessoa ativa o ocupa. Para cargos em uso, mude o cargo das pessoas antes, em Pessoas. Criar um cargo com o nome de um desativado traz ele de volta.',
      },
    ],
  },

  estrutura: {
    titulo: 'Como a estrutura se organiza?',
    itens: [
      {
        pergunta: 'Qual a diferença entre diretoria, núcleo e equipe?',
        resposta:
          'Diretoria é a área do banco que pede ou recebe o trabalho (UGR, CRM, COMIN...). Núcleo é o grupo de produção dentro dela (House-CRM, Multimídia...). Equipe é a especialidade dentro do núcleo (Design, HTML, Videomaker, Motion...).',
      },
      {
        pergunta: 'Como crio um núcleo ou uma equipe?',
        resposta:
          'Abra a diretoria na lista e use + Núcleo. Dentro do núcleo, use + Equipe. Dá para renomear no lápis e desativar na lixeira. Um núcleo só desativa sem equipes, e uma equipe só desativa sem pessoas ativas. Se a diretoria não aparece, escolha Todas as diretorias ou crie com Nova diretoria.',
      },
      {
        pergunta: 'Para que serve ligar a equipe a um núcleo?',
        resposta:
          'É o que faz o botão Meu núcleo e o filtro por núcleo funcionarem. Equipe sem núcleo fica de fora deles.',
      },
    ],
  },

  demandantes: {
    titulo: 'O que é um demandante?',
    itens: [
      {
        pergunta: 'Quem é o demandante?',
        resposta:
          'Quem pediu o trabalho. Pode ser uma pessoa, uma área, uma unidade ou alguém de fora da empresa.',
      },
      {
        pergunta: 'Por que registrar o demandante?',
        resposta:
          'Para saber de onde vêm os pedidos e para quem o trabalho foi feito, sem depender da memória de cada profissional.',
      },
    ],
  },

  projetos: {
    titulo: 'Para que serve um projeto?',
    itens: [
      {
        pergunta: 'O que é um projeto?',
        resposta:
          'Um objetivo maior, de prazo mais longo, que reúne campanhas e demandas ligadas entre si. É opcional.',
      },
      {
        pergunta: 'Qual a diferença entre projeto e campanha?',
        resposta:
          'O projeto é o objetivo geral. A campanha é uma ação específica dentro dele, com datas de início e fim.',
      },
      {
        pergunta: 'O que significam Ativo, Pausado e Encerrado?',
        resposta:
          'Ativo: em andamento. Pausado: parado por enquanto. Encerrado: concluído, mantido só para consulta.',
      },
    ],
  },

  campanhas: {
    titulo: 'Para que serve uma campanha?',
    itens: [
      {
        pergunta: 'O que é uma campanha?',
        resposta:
          'Uma ação que mobiliza várias frentes de trabalho ao mesmo tempo, como e-mail, peças gráficas, vídeo e podcast. Ela reúne essas demandas para você enxergar o todo.',
      },
      {
        pergunta: 'Por que vincular as demandas a uma campanha?',
        resposta:
          'Para conseguir responder perguntas como: quantas pessoas participaram da campanha? Quantas demandas ela teve? Quanto trabalho ela gerou? Sem o vínculo, cada demanda aparece isolada.',
      },
      {
        pergunta: 'O que mostram as colunas Demandas, Pessoas e Valor gerado?',
        resposta:
          'O que a campanha reuniu até agora: quantas demandas (sem as canceladas), quantas pessoas diferentes participaram e a soma do valor das peças. Clique no nome da campanha para ver o detalhe.',
      },
      {
        pergunta: 'O que é o código da campanha?',
        resposta:
          'Um identificador livre, se a sua área usa um (por exemplo BC2606CR). Não precisa ser único e nem todo setor usa.',
      },
      {
        pergunta: 'Preciso informar uma campanha em toda demanda?',
        resposta:
          'Não. Só quando a demanda fizer parte de uma ação maior. Demandas avulsas ficam sem campanha.',
      },
    ],
  },

  'campanha-detalhe': {
    titulo: 'Como ler a visão da campanha?',
    itens: [
      {
        pergunta: 'Quantos funcionários participam desta campanha?',
        resposta:
          'O indicador Pessoas conta cada pessoa uma vez, mesmo que ela esteja em várias demandas da campanha. Quem só aparece em demanda cancelada não entra.',
      },
      {
        pergunta: 'Quanto trabalho a campanha gerou?',
        resposta:
          'Valor gerado é a soma do valor das peças de todas as demandas da campanha, e Peças e Atividades mostram o volume. É uma referência de mercado, não um custo real.',
      },
      {
        pergunta: 'Por que o número de demandas é menor que o total?',
        resposta:
          'As demandas canceladas aparecem na lista e na faixa de situação, mas não entram nos indicadores, para não inflar o resultado.',
      },
      {
        pergunta: 'Como uma demanda entra nesta campanha?',
        resposta:
          'Escolha a campanha no campo Campanha ao criar ou editar a demanda. Demandas avulsas ficam sem campanha e não aparecem aqui.',
      },
    ],
  },

  'tipos-peca': {
    titulo: 'O que é um tipo de peça?',
    itens: [
      {
        pergunta: 'O que é um tipo de peça?',
        resposta:
          'Um item do catálogo de entregas de cada área (por exemplo e-mail, vídeo, foto), com um valor de referência por unidade. É escolhido ao lançar uma peça.',
      },
      {
        pergunta: 'O que é o valor de referência?',
        resposta:
          'O valor de mercado de uma unidade daquela entrega. O valor da peça é a quantidade vezes esse valor. Mudar o valor aqui não altera peças já lançadas.',
      },
      {
        pergunta: 'Quem pode criar e editar tipos de peça?',
        resposta:
          'Gestor e Admin, em qualquer área. A Referência de equipe, só nos tipos da própria área.',
      },
    ],
  },

  pessoas: {
    titulo: 'Como funcionam as pessoas e os perfis?',
    itens: [
      {
        pergunta: 'O que cada perfil pode fazer?',
        resposta:
          'Profissional: registra o próprio trabalho. Visualizador: só consulta, sem relatório. Gestor: aprova cadastros e gerencia pessoas, menos Admins. Admin: tudo, inclusive conceder o perfil Admin.',
      },
      {
        pergunta: 'O que é Referência de equipe?',
        resposta:
          'Não é um perfil: é um Profissional que referencia a própria área. Ele edita e cancela atividades, peças e evidências dos colegas da área e gerencia os tipos de peça dela. Defina ao aprovar ou ao editar a pessoa.',
      },
      {
        pergunta: 'Como aprovar um cadastro novo?',
        resposta:
          'Abra a pessoa com cadastro pendente, confira área e cargo, escolha o perfil e aprove. Um cadastro rejeitado não pode ser aprovado depois.',
      },
      {
        pergunta: 'Alguém já está no sistema mas criou uma conta nova. O que faço?',
        resposta:
          'Abra o cadastro pendente da conta nova e use "Esta pessoa já existe no sistema?": escolha o cadastro que já existe (por exemplo, importado da planilha) e vincule. A conta passa a ser dessa pessoa, com todo o histórico, e o cadastro novo é removido. Se você aprovar sem vincular, a pessoa fica duplicada. Quando a pessoa informa a chave C de um cadastro importado, a conta já chega com um aviso "Pediu para assumir um cadastro importado": confira nome, e-mail e equipe e use Confirmar vínculo. Só depois disso ela enxerga o histórico.',
      },
      {
        pergunta: 'Para que serve a chave C?',
        resposta:
          'Para ligar a conta de quem se cadastra ao registro que já existe (pessoas importadas da planilha). A chave C é a matrícula: C seguida de 7 números para contratados, F seguida de 7 números para funcionários do Banco. Se a chave já estiver num cadastro importado, um Gestor ou Admin confirma o vínculo.',
      },
    ],
  },

  relatorio: {
    titulo: 'Como funciona o relatório mensal?',
    itens: [
      {
        pergunta: 'O que é o relatório mensal?',
        resposta:
          'O resumo do que você produziu no mês, montado automaticamente a partir das suas atividades e peças. Você confere, ajusta e aprova.',
      },
      {
        pergunta: 'Posso tirar algo do relatório?',
        resposta:
          'Sim. Desmarque as atividades que não devem entrar e escreva as observações gerais. Isso só vale para o relatório, não apaga nada do sistema.',
      },
      {
        pergunta: 'O que acontece quando eu aprovo?',
        resposta:
          'O conteúdo fica congelado: mesmo que atividades sejam editadas depois, o relatório assinado não muda. Só um Gestor ou Admin consegue reabrir.',
      },
      {
        pergunta: 'Como envio o relatório?',
        resposta:
          'Depois de aprovado, use Baixar DOCX ou Imprimir / Salvar PDF.',
      },
    ],
  },
  emprestimos: {
    titulo: 'Como funciona o empréstimo de equipamentos?',
    itens: [
      {
        pergunta: 'Qual é o caminho de um empréstimo?',
        resposta:
          'O Operador registra o pedido, com retirada e devolução prevista, e o termo (OK na tela). O Gestor de equipamentos autoriza ou recusa. Autorizado, o Operador entrega e, na volta, recebe a devolução. Todo empréstimo passa por autorização, dentro ou fora do prédio.',
      },
      {
        pergunta: 'Posso reservar para outro dia?',
        resposta:
          'Sim. Informe a retirada prevista (vazio significa agora) e a devolução: a lista de equipamentos mostra só o que está livre em todo esse período. Um equipamento que volta amanhã pode ser reservado para a semana que vem. Dois pedidos não podem ocupar a mesma peça ao mesmo tempo.',
      },
      {
        pergunta: 'O que significa EXTERNO e a linha em vermelho?',
        resposta:
          'EXTERNO é o equipamento que sai do prédio. Pedido externo esperando decisão e empréstimo atrasado ficam em destaque e vão para o topo da fila.',
      },
      {
        pergunta: 'Posso corrigir ou prorrogar?',
        resposta:
          'Enquanto o pedido espera autorização dá para corrigir os dados (lápis). Quem já está com o equipamento pode ter a devolução prorrogada (calendário); fica registrado quem prorrogou e por quê. Para trocar os equipamentos, cancele e registre de novo.',
      },
      {
        pergunta: 'Quem vê o quê?',
        resposta:
          'Só entram no módulo o Operador de estúdio, o Gestor de equipamentos, o Gestor e o Admin, e todos veem todos os empréstimos. As fotos de saída e de devolução são opcionais e ficam nos detalhes (olho).',
      },
    ],
  },
  inventario: {
    titulo: 'Como funciona o inventário?',
    itens: [
      {
        pergunta: 'Individual ou por quantidade?',
        resposta:
          'Na categoria individual cada peça tem o seu registro (câmera, lente), e dá para saber com quem ela está. Na categoria por quantidade o lote é um registro só (cabos, pilhas) e cada retirada diz quantas unidades saíram.',
      },
      {
        pergunta: 'Quem edita o inventário e o termo?',
        resposta:
          'Gestor de equipamentos, Gestor e Admin. Mudar o termo cria uma versão nova: os empréstimos antigos guardam a versão que foi aceita.',
      },
      {
        pergunta: 'Posso apagar um equipamento?',
        resposta:
          'Ele é desativado, não apagado, para o histórico continuar completo. Não dá para desativar enquanto estiver em um empréstimo em aberto.',
      },
    ],
  },
  agenda: {
    titulo: 'Como ler a agenda?',
    itens: [
      {
        pergunta: 'O que cada barra mostra?',
        resposta:
          'Uma barra é um período de uso: da retirada até a devolução prevista. Verde é com a pessoa, azul é reservado (já autorizado), laranja espera autorização, vermelho é atrasado e cinza já foi devolvido. A linha vermelha vertical é o agora.',
      },
      {
        pergunta: 'Como vejo com quem o equipamento está?',
        resposta:
          'Clique no nome do equipamento: aparecem quem está com ele agora, as reservas e o que voltou recentemente. Clicar numa barra abre o período, com a pessoa, as datas e o local.',
      },
      {
        pergunta: 'Por que o atrasado continua depois da data prevista?',
        resposta:
          'Porque o equipamento ainda não voltou: a faixa listrada vai até agora. Enquanto não for devolvido, ele não pode ser reservado para outra pessoa.',
      },
      {
        pergunta: 'Quem vê o quê?',
        resposta:
          'Quem tem acesso ao módulo vê reservas, empréstimos e devoluções recentes. Sem o papel de Operador de estúdio ou de Gestor de equipamentos (ou perfil Gestor ou Admin), a pessoa não entra.',
      },
    ],
  },
};
