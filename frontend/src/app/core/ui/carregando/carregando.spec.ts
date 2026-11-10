import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Carregando } from './carregando';

describe('Carregando', () => {
  let fixture: ComponentFixture<Carregando>;

  beforeEach(() => {
    fixture = TestBed.createComponent(Carregando);
  });

  it('mostra a barra indeterminada só enquanto está ativo', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('p-progressbar')).toBeNull();

    fixture.componentRef.setInput('ativo', true);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('p-progressbar')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[role="status"]')).not.toBeNull();
  });
});
