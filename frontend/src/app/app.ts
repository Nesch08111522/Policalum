import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastHostComponent } from './shared/components/toast/toast-host.component';
import { MarcaService } from './shared/services/marca.service';

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ToastHostComponent],
  template: `<router-outlet></router-outlet><app-toast-host></app-toast-host>`,
  styleUrls: ['./app.css']
})
export class App {
  title = 'POLICALUM';
  constructor() {
    inject(MarcaService).cargar();
  }
}