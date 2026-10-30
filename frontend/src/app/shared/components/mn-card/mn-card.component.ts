import { Component, Input } from '@angular/core';

@Component({
  selector: 'mn-card',
  standalone: true,
  template: `
    <article class="card" [class.card-raised]="raised">
      @if (title) {
        <header class="card-header"><h2>{{ title }}</h2></header>
      }
      <div class="card-body"><ng-content></ng-content></div>
    </article>
  `,
})
export class MnCardComponent {
  @Input() title = '';
  @Input() raised = false;
}
