// src/app/components/asistencia/asistencia.spec.ts
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AsistenciaComponent } from './asistencia';  // ✅ Importa AsistenciaComponent
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { AsistenciaService } from '../../services/asistencia.service';
import { TokenService } from '../../services/token.service';

describe('AsistenciaComponent', () => {  // ✅ Cambia el nombre del describe
  let component: AsistenciaComponent;
  let fixture: ComponentFixture<AsistenciaComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        AsistenciaComponent,  // ✅ Usa AsistenciaComponent
        HttpClientTestingModule  // ✅ Para pruebas HTTP
      ],
      providers: [
        AsistenciaService,
        TokenService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AsistenciaComponent);  // ✅ Usa AsistenciaComponent
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});