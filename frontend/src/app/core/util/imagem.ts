/** Tipos que a foto de perfil aceita para escolher; o navegador converte tudo em JPEG antes de enviar. */
export const TIPOS_DE_FOTO = ['image/jpeg', 'image/png', 'image/webp'];

/** Foto original acima disso nem é lida: evita travar a aba com uma imagem enorme. */
export const TAMANHO_MAXIMO_ORIGINAL = 15 * 1024 * 1024;

/**
 * Recorta a imagem em quadrado, pelo centro, reduz para `lado` pixels e devolve um JPEG.
 * A foto de perfil fica com poucas dezenas de KB e o envio é rápido, mesmo de uma foto de celular.
 */
export async function reduzirParaQuadrado(arquivo: Blob, lado = 320, qualidade = 0.88): Promise<Blob> {
  const bitmap = await createImageBitmap(arquivo);
  try {
    const menor = Math.min(bitmap.width, bitmap.height);
    const origemX = (bitmap.width - menor) / 2;
    const origemY = (bitmap.height - menor) / 2;

    const tela = document.createElement('canvas');
    tela.width = lado;
    tela.height = lado;
    const contexto = tela.getContext('2d');
    if (!contexto) throw new Error('Não foi possível preparar a imagem');
    contexto.fillStyle = '#ffffff'; // PNG com transparência vira JPEG com fundo branco
    contexto.fillRect(0, 0, lado, lado);
    contexto.drawImage(bitmap, origemX, origemY, menor, menor, 0, 0, lado, lado);

    return await new Promise<Blob>((resolve, reject) =>
      tela.toBlob((blob) => (blob ? resolve(blob) : reject(new Error('Não foi possível preparar a imagem'))), 'image/jpeg', qualidade),
    );
  } finally {
    bitmap.close();
  }
}
