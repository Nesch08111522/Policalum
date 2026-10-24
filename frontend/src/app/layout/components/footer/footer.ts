import { Component, ChangeDetectionStrategy } from '@angular/core';
import { APP_VERSION, MarcaService } from '../../../shared/services/marca.service';

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-footer',
  standalone: true,
  templateUrl: './footer.html',
  styleUrls: ['./footer.css'],
})
export class FooterComponent {
  readonly year = new Date().getFullYear();
  readonly version = APP_VERSION;
  constructor(readonly marca: MarcaService) {}
}
