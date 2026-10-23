import { CommonModule } from '@angular/common';
import { Component, ElementRef, EventEmitter, HostListener, Input, Output, forwardRef, ChangeDetectionStrategy } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { SelectOption } from '../../models/ui.models';

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-select',
  standalone: true,
  imports: [CommonModule],
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => SelectComponent), multi: true }],
  template: `
    <div class="select-box">
      <button type="button" class="input" [id]="inputId" [disabled]="disabled" [attr.aria-expanded]="open" aria-haspopup="listbox" (click)="toggle()">
        {{ currentLabel }}
      </button>
      <div class="select-menu" *ngIf="open" role="listbox">
        <input class="input" type="search" [attr.aria-label]="'Buscar ' + placeholder" [value]="query" (input)="onQuery($event)" placeholder="Buscar…" />
        <button type="button" class="select-option" *ngIf="clearable" (click)="choose('')">{{ placeholder }}</button>
        <button
          type="button"
          class="select-option"
          *ngFor="let option of filtered"
          role="option"
          [attr.aria-selected]="option.value === value"
          (click)="choose(option.value)">
          {{ option.label }}
        </button>
        <p *ngIf="!filtered.length" class="field-hint" style="padding:8px 12px">Sin coincidencias</p>
      </div>
    </div>
  `,
})
export class SelectComponent implements ControlValueAccessor {
  @Input() options: SelectOption[] = [];
  @Input() placeholder = 'Seleccionar';
  @Input() inputId = '';
  @Input() clearable = true;
  @Output() search = new EventEmitter<string>();
  open = false;
  query = '';
  value = '';
  disabled = false;
  private onChange: (value: string) => void = () => undefined;
  private onTouched: () => void = () => undefined;

  constructor(private readonly host: ElementRef<HTMLElement>) {}

  get filtered(): SelectOption[] {
    const term = this.query.trim().toLowerCase();
    if (!term) return this.options;
    return this.options.filter((option) => option.label.toLowerCase().includes(term));
  }

  get currentLabel(): string {
    return this.options.find((option) => option.value === this.value)?.label || this.placeholder;
  }

  onQuery(event: Event): void {
    this.query = (event.target as HTMLInputElement).value;
    this.search.emit(this.query);
  }

  toggle(): void {
    if (this.disabled) return;
    this.open = !this.open;
  }

  choose(value: string): void {
    this.value = value;
    this.onChange(value);
    this.onTouched();
    this.open = false;
    this.query = '';
  }

  writeValue(value: string | null): void {
    this.value = value ?? '';
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(disabled: boolean): void {
    this.disabled = disabled;
  }

  @HostListener('document:click', ['$event'])
  closeOnOutside(event: MouseEvent): void {
    if (!this.host.nativeElement.contains(event.target as Node)) this.open = false;
  }
}
