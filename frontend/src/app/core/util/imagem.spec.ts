import { reduzirParaQuadrado } from './imagem';

async function imagem(largura: number, altura: number): Promise<Blob> {
  const tela = document.createElement('canvas');
  tela.width = largura;
  tela.height = altura;
  const ctx = tela.getContext('2d')!;
  ctx.fillStyle = '#ea580c';
  ctx.fillRect(0, 0, largura, altura);
  return new Promise((resolve) => tela.toBlob((b) => resolve(b!), 'image/png'));
}

describe('reduzirParaQuadrado', () => {
  it('devolve um JPEG quadrado do tamanho pedido, mesmo de uma imagem retangular', async () => {
    const saida = await reduzirParaQuadrado(await imagem(900, 400), 128);
    expect(saida.type).toBe('image/jpeg');

    const bitmap = await createImageBitmap(saida);
    expect(bitmap.width).toBe(128);
    expect(bitmap.height).toBe(128);
    bitmap.close();
  });

  it('a foto reduzida fica bem menor que a original grande', async () => {
    const original = await imagem(3000, 2000);
    const saida = await reduzirParaQuadrado(original, 320);
    expect(saida.size).toBeLessThan(original.size);
    expect(saida.size).toBeLessThan(2 * 1024 * 1024); // o limite do servidor
  });

  it('recusa o que não é imagem', async () => {
    await expectAsync(reduzirParaQuadrado(new Blob(['texto'], { type: 'text/plain' }))).toBeRejected();
  });
});
