import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { TabItem } from '../../models/ui.models';

@Component({
  selector: 'app-tabs',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="tabs" role="tablist">
      <button
        *ngFor="let tab of tabs"
        type="button"
        class="tab"
        role="tab"
        [class.active]="tab.id === active"
        [attr.aria-selected]="tab.id === active"
        (click)="activeChange.emit(tab.id)">
        {{ tab.label }}
      </button>
    </div>
  `,
})
export class TabsComponent {
  @Input() tabs: TabItem[] = [];
  @Input() active = '';
  @Output() activeChange = new EventEmitter<string>();
}
