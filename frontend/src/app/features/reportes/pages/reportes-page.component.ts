import { Component } from '@angular/core';
import { WorkspacePageComponent } from '../../workspace/workspace-page.component';

@Component({
  selector: 'app-reportes-page',
  standalone: true,
  imports: [WorkspacePageComponent],
  templateUrl: './reportes-page.component.html',
  styleUrls: ['./reportes-page.component.css'],
})
export class ReportesPageComponent {
  readonly moduleId = 'reportes';
}
