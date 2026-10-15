import { Component } from '@angular/core';
import { WorkspacePageComponent } from '../../workspace/workspace-page.component';

@Component({
  selector: 'app-kardex-page',
  standalone: true,
  imports: [WorkspacePageComponent],
  templateUrl: './kardex-page.component.html',
  styleUrls: ['./kardex-page.component.css'],
})
export class KardexPageComponent {
  readonly moduleId = 'kardex';
}
