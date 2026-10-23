import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-filter-bar',
  standalone: true,
  template: `<div class="card card-body"><div class="filter-bar"><ng-content></ng-content></div></div>`,
})
export class FilterBarComponent {}
