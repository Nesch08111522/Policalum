import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges, OnDestroy, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { PenPipe } from '../../../shared/pipes/format.pipes';
import { ToastService } from '../../../shared/services/toast.service';
import { problemText } from '../../../shared/utils/feedback';
import { ComprobanteSunat, FacturacionElectronicaApiService, FacturacionElectronicaPanel } from '../data-access/facturacion-electronica.api';

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-facturacion-sunat',
  standalone: true,
  imports: [CommonModule, RouterLink, PenPipe],
  templateUrl: './facturacion-sunat.component.html',
  styleUrls: ['./facturacion-sunat.component.css'],
})
export class FacturacionSunatComponent implements OnInit, OnChanges, OnDestroy {
  @Input() sedeId = '';
  panel: FacturacionElectronicaPanel | null = null;
  error: string | null = null;
  private readonly destroy$ = new Subject<void>();
  private readonly recarga$ = new Subject<void>();

  private iniciado = false;

  constructor(private readonly api: FacturacionElectronicaApiService, private readonly toast: ToastService) {}

  ngOnInit(): void { this.iniciado = true; this.cargar(); }
  ngOnChanges(): void { if (this.iniciado) this.cargar(); }
  ngOnDestroy(): void { this.recarga$.next(); this.recarga$.complete(); this.destroy$.next(); this.destroy$.complete(); }

  get rucVisible(): string {
    const ruc = this.panel?.ruc?.trim();
    return ruc ? ruc : '20·········';
  }

  estadoClase(estado: string | undefined): string { return estado || 'PENDIENTE'; }
  montoClase(row: ComprobanteSunat): string { return row.estadoCdr === 'RECHAZADO' || row.estadoCdr === 'ANULADO' ? 'monto-bad' : 'monto-ok'; }

  cargar(): void {
    this.recarga$.next();
    this.api.panel(this.sedeId ? { sedeId: this.sedeId } : {}).pipe(takeUntil(this.recarga$), takeUntil(this.destroy$)).subscribe({
      next: (panel) => { this.panel = panel; this.error = null; },
      error: (error: unknown) => { this.error = problemText(error); },
    });
  }

  emitir(): void {
    this.api.emitir().pipe(takeUntil(this.destroy$)).subscribe({
      next: () => this.toast.success('Comprobante enviado a SUNAT.'),
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }

  consultar(row?: ComprobanteSunat): void {
    this.api.consultarValidez(row?.serie ?? '', row?.documentoCliente ?? '').pipe(takeUntil(this.destroy$)).subscribe({
      next: () => this.toast.success('SUNAT respondió la consulta de validez.'),
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }
}
