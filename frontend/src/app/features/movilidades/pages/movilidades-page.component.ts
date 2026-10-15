import { Component } from '@angular/core';
import { WorkspacePageComponent } from '../../workspace/workspace-page.component';

@Component({
  selector: 'app-movilidades-page',
  standalone: true,
  imports: [WorkspacePageComponent],
  templateUrl: './movilidades-page.component.html',
  styleUrls: ['./movilidades-page.component.css'],
})
export class MovilidadesPageComponent {
  readonly moduleId = 'movilidades';
}
