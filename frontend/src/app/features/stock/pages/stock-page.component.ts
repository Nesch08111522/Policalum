import { Component } from '@angular/core';
import { WorkspacePageComponent } from '../../workspace/workspace-page.component';

@Component({
  selector: 'app-stock-page',
  standalone: true,
  imports: [WorkspacePageComponent],
  templateUrl: './stock-page.component.html',
  styleUrls: ['./stock-page.component.css'],
})
export class StockPageComponent {
  readonly moduleId = 'stock';
}
