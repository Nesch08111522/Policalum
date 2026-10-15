import { Component } from '@angular/core';
import { WorkspacePageComponent } from '../../workspace/workspace-page.component';

@Component({
  selector: 'app-turnos-caja-page',
  standalone: true,
  imports: [WorkspacePageComponent],
  templateUrl: './turnos-caja-page.component.html',
  styleUrls: ['./turnos-caja-page.component.css'],
})
export class TurnosCajaPageComponent {
  readonly moduleId = 'turnos-caja';
}
