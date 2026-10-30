import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Routes } from '@angular/router';
import { environment } from '../../../environments/environment';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { FormFieldComponent } from '../../shared/components/form-field/form-field.component';
import { ToastService } from '../../shared/services/toast.service';
import { problemText } from '../../shared/utils/feedback';
import { TokenService } from '../../services/token.service';

export interface ProyectoWeb {
  id: string;
  titulo: string;
  descripcion: string;
  imagenUrl?: string;
  orden: number;
  publicado: boolean;
}

@Component({
  selector: 'app-portafolio-web-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, FormFieldComponent],
  templateUrl: './portafolio-web-page.component.html',
  styleUrls: ['./portafolio-web-page.component.css'],
})
export class PortafolioWebPageComponent implements OnInit {
  proyectos: ProyectoWeb[] = [];
  editando: string | null = null;
  guardando = false;
  readonly esAdmin: boolean;
  private readonly fb = inject(FormBuilder);
  readonly form = this.fb.nonNullable.group({
    titulo: ['', [Validators.required, Validators.maxLength(160)]],
    descripcion: ['', [Validators.required, Validators.maxLength(800)]],
    imagenUrl: ['', Validators.maxLength(500)],
    orden: [0, Validators.required],
    publicado: [true],
  });

  constructor(
    private readonly http: HttpClient,
    private readonly toast: ToastService,
    private readonly cdr: ChangeDetectorRef,
    tokens: TokenService,
  ) {
    this.esAdmin = tokens.getRol() === 'ROLE_ADMIN';
  }

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    this.http.get<ProyectoWeb[]>(`${environment.apiUrl}/api/v1/portafolio`).subscribe({
      next: (lista) => { this.proyectos = lista ?? []; this.cdr.detectChanges(); },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }

  editar(proyecto: ProyectoWeb): void {
    this.editando = proyecto.id;
    this.form.reset({
      titulo: proyecto.titulo,
      descripcion: proyecto.descripcion,
      imagenUrl: proyecto.imagenUrl ?? '',
      orden: proyecto.orden ?? 0,
      publicado: proyecto.publicado !== false,
    });
  }

  limpiar(): void {
    this.editando = null;
    this.form.reset({ titulo: '', descripcion: '', imagenUrl: '', orden: 0, publicado: true });
  }

  guardar(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.guardando = true;
    const cuerpo = this.form.getRawValue();
    const pedido = this.editando
      ? this.http.put<ProyectoWeb>(`${environment.apiUrl}/api/v1/portafolio/${this.editando}`, cuerpo)
      : this.http.post<ProyectoWeb>(`${environment.apiUrl}/api/v1/portafolio`, cuerpo);
    pedido.subscribe({
      next: () => {
        this.guardando = false;
        this.toast.success(this.editando ? 'Proyecto actualizado.' : 'Proyecto publicado en el portal.');
        this.limpiar();
        this.cargar();
      },
      error: (error: unknown) => {
        this.guardando = false;
        this.toast.error(problemText(error));
      },
    });
  }

  eliminar(proyecto: ProyectoWeb): void {
    this.http.delete(`${environment.apiUrl}/api/v1/portafolio/${proyecto.id}`).subscribe({
      next: () => { this.toast.success('Proyecto retirado del portafolio.'); this.cargar(); },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }
}

export const PORTAFOLIO_WEB_ROUTES: Routes = [
  { path: '', component: PortafolioWebPageComponent, title: 'Portafolio web', data: { section: 'Configuración', breadcrumb: 'Portafolio web' } },
];
