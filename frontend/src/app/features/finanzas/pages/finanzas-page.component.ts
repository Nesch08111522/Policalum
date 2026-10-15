import { Component } from '@angular/core';
import { WorkspacePageComponent } from '../../workspace/workspace-page.component';

@Component({
  selector: 'app-finanzas-page',
  standalone: true,
  imports: [WorkspacePageComponent],
  templateUrl: './finanzas-page.component.html',
  styleUrls: ['./finanzas-page.component.css'],
})
export class FinanzasPageComponent {
  readonly moduleId = 'finanzas';
}
