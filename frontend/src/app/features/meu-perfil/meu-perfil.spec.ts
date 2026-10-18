import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { provideTesteApp } from '../../../testing/providers';
import { AuthService } from '../../core/auth/auth.service';

import { MeuPerfil } from './meu-perfil';

describe('MeuPerfil', () => {
  let component: MeuPerfil;
  let fixture: ComponentFixture<MeuPerfil>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MeuPerfil],
      providers: provideTesteApp(),
    })
    .compileComponents();

    fixture = TestBed.createComponent(MeuPerfil);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

describe('MeuPerfil (foto de perfil)', () => {
  let component: MeuPerfil;
  let fixture: ComponentFixture<MeuPerfil>;
  let auth: AuthService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [MeuPerfil], providers: provideTesteApp() }).compileComponents();
    fixture = TestBed.createComponent(MeuPerfil);
    component = fixture.componentInstance;
    auth = TestBed.inject(AuthService);
    (auth as unknown as { me: { reload: () => void } }).me = { reload: jasmine.createSpy('reload') };
    fixture.detectChanges();
  });

  function evento(arquivo: File | null): Event {
    return { target: { files: arquivo ? [arquivo] : [], value: 'x' } } as unknown as Event;
  }

  async function imagemPng(): Promise<File> {
    const tela = document.createElement('canvas');
    tela.width = 50;
    tela.height = 30;
    tela.getContext('2d')!.fillRect(0, 0, 50, 30);
    const blob = await new Promise<Blob>((r) => tela.toBlob((b) => r(b!), 'image/png'));
    return new File([blob], 'eu.png', { type: 'image/png' });
  }

  it('sem arquivo escolhido, nada acontece', async () => {
    const enviar = spyOn(auth, 'enviarFotoDePerfil');
    await component.escolherFoto(evento(null));
    expect(enviar).not.toHaveBeenCalled();
  });

  it('recusa arquivo que não é imagem e avisa na própria tela', async () => {
    const enviar = spyOn(auth, 'enviarFotoDePerfil');
    await component.escolherFoto(evento(new File(['oi'], 'x.txt', { type: 'text/plain' })));
    expect(enviar).not.toHaveBeenCalled();
    expect(component.erroFoto()).toContain('JPEG, PNG ou WebP');
  });

  it('reduz a imagem em JPEG antes de enviar e recarrega a pessoa', async () => {
    const enviar = spyOn(auth, 'enviarFotoDePerfil').and.returnValue(of({}) as never);

    await component.escolherFoto(evento(await imagemPng()));

    expect(enviar).toHaveBeenCalledTimes(1);
    const enviado = enviar.calls.mostRecent().args[0] as Blob;
    expect(enviado.type).toBe('image/jpeg');
    expect(auth.me.reload).toHaveBeenCalled();
    expect(component.enviandoFoto()).toBeFalse();
    expect(component.erroFoto()).toBeNull();
  });

  it('mostra o motivo que o servidor devolveu quando o envio falha', async () => {
    spyOn(auth, 'enviarFotoDePerfil').and.returnValue(throwError(() => ({ error: { mensagem: 'Imagem acima de 2 MB' } })) as never);

    await component.escolherFoto(evento(await imagemPng()));

    expect(component.erroFoto()).toBe('Imagem acima de 2 MB');
    expect(component.enviandoFoto()).toBeFalse();
  });

  it('remover chama o servidor e recarrega a pessoa', async () => {
    const remover = spyOn(auth, 'removerFotoDePerfil').and.returnValue(of({}) as never);

    await component.removerFoto();

    expect(remover).toHaveBeenCalled();
    expect(auth.me.reload).toHaveBeenCalled();
  });
});
