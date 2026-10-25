import { CommonModule } from '@angular/common';
import { Component, HostListener, Input } from '@angular/core';
import { ToastService } from '../../services/toast.service';
import { ReportService } from '../../services/report.service';

@Component({
  selector: 'app-export-buttons',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="export-wrap">
      <button type="button" class="btn btn-ghost" (click)="abierto = !abierto" [disabled]="generando" aria-haspopup="menu" [attr.aria-expanded]="abierto">
        <span class="spinner" *ngIf="generando" aria-hidden="true"></span>
        {{ generando ? 'Generando...' : 'Exportar' }}
      </button>
      <div class="export-menu" *ngIf="abierto" role="menu">
        <button type="button" role="menuitem" (click)="bajar('pdf')">📄 Descargar PDF</button>
        <button type="button" role="menuitem" (click)="bajar('xlsx')">📊 Descargar Excel</button>
      </div>
    </div>
  `,
  styles: [`
    .export-wrap { position: relative; }
    .export-menu {
      position: absolute;
      right: 0;
      z-index: 30;
      min-width: 12rem;
      margin-top: 0.35rem;
      padding: 0.35rem;
      border: 1px solid var(--color-border);
      border-radius: 12px;
      background: var(--color-surface);
      box-shadow: var(--shadow-lg);
    }
    .export-menu button {
      display: block;
      width: 100%;
      text-align: left;
      border: 0;
      background: transparent;
      color: var(--color-text);
      border-radius: 8px;
      padding: 0.55rem 0.7rem;
      cursor: pointer;
    }
    .export-menu button:hover { background: var(--color-bg-subtle); }
    .spinner {
      width: 0.8rem;
      height: 0.8rem;
      border: 2px solid currentColor;
      border-right-color: transparent;
      border-radius: 50%;
      display: inline-block;
      animation: giro 0.7s linear infinite;
    }
    @keyframes giro { to { transform: rotate(360deg); } }
  `],
})
export class ExportButtonsComponent {
  @Input() modulo = 'ventas';
  @Input() filtros = '';
  abierto = false;
  generando = false;

  constructor(private readonly reportes: ReportService, private readonly toast: ToastService) {}

  bajar(formato: 'pdf' | 'xlsx'): void {
    this.abierto = false;
    this.generando = true;
    this.reportes.descargar(this.modulo, formato, this.filtros).subscribe({
      next: () => { this.generando = false; this.toast.success('Reporte descargado.'); },
      error: () => { this.generando = false; this.toast.error('No se pudo generar el reporte.'); },
    });
  }

  @HostListener('document:click', ['$event'])
  cerrar(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('app-export-buttons')) this.abierto = false;
  }
}
