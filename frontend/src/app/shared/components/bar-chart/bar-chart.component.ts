import { CommonModule } from '@angular/common';
import { Component, Input, ChangeDetectionStrategy } from '@angular/core';

export interface ChartPoint {
  label: string;
  value: number;
}

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-bar-chart',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="bar-chart" role="img" [attr.aria-label]="label">
      <div class="bar-col" *ngFor="let point of seguros">
        <span class="text-xs">{{ point.value }}</span>
        <div class="bar" [style.height.px]="height(point.value)"></div>
        <span class="bar-label">{{ point.label }}</span>
      </div>
      <p *ngIf="!seguros.length" class="text-muted">Sin datos para graficar.</p>
    </div>
  `,
})
export class BarChartComponent {
  @Input() points: ChartPoint[] | null = [];
  @Input() label = 'Gráfico de barras';

  get seguros(): ChartPoint[] {
    return (this.points ?? []).filter((point) => !!point).map((point) => ({
      label: point.label || '—',
      value: Number.isFinite(Number(point.value)) ? Number(point.value) : 0,
    }));
  }

  height(value: number): number {
    const max = Math.max(...this.seguros.map((point) => point.value), 1);
    return Math.max(8, Math.round((value / max) * 110));
  }
}
