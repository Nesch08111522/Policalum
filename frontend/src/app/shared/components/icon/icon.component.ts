import { Component, Input, ChangeDetectionStrategy } from '@angular/core';

const ICONS: Record<string, string> = {
  plus: 'M12 5v14M5 12h14',
  search: 'M11 19a8 8 0 1 1 0-16 8 8 0 0 1 0 16zM21 21l-4.35-4.35',
  edit: 'M4 20h4L18 10l-4-4L4 16v4z',
  trash: 'M4 7h16M9 7V5h6v2M7 7l1 13h8l1-13',
  x: 'M6 6l12 12M18 6L6 18',
  chevron: 'M9 6l6 6-6 6',
  chevronDown: 'M6 9l6 6 6-6',
  check: 'M5 12l5 5L20 7',
  filter: 'M4 5h16l-6 7v6l-4 2v-8z',
  refresh: 'M20 12a8 8 0 1 1-2.3-5.6M20 4v6h-6',
  sun: 'M12 4v2M12 18v2M4 12H2M22 12h-2M6 6l1.5 1.5M16.5 16.5L18 18M18 6l-1.5 1.5M7.5 16.5L6 18M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8z',
  moon: 'M21 14.5A8.5 8.5 0 1 1 9.5 3 7 7 0 0 0 21 14.5z',
  bell: 'M6 16V11a6 6 0 1 1 12 0v5l2 2H4l2-2zM10 19a2 2 0 0 0 4 0',
  logout: 'M10 7V5H4v14h6v-2M10 12h10M16 8l4 4-4 4',
  menu: 'M4 7h16M4 12h16M4 17h16',
  home: 'M4 11l8-7 8 7v9H4v-9z',
  cart: 'M4 6h2l2 11h10l2-8H8',
  users: 'M9 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM3 19a6 6 0 0 1 12 0M17 11a3 3 0 1 0-2-5M21 19a6 6 0 0 0-4-5.6',
  file: 'M7 3h7l5 5v13H7zM14 3v5h5',
  clipboard: 'M9 4h6l1 2h3v14H5V6h3zM9 4a2 2 0 0 1 2-1h2a2 2 0 0 1 2 1',
  box: 'M3 8l9-5 9 5-9 5-9-5zM3 8v8l9 5 9-5V8M12 13v8',
  wrench: 'M14 7a4 4 0 0 0-5.7 5.2L4 16.5 7.5 20l4.3-4.3A4 4 0 0 0 17 10l-3 3-3-3 3-3z',
  folder: 'M3 7h6l2 2h10v10H3z',
  grid: 'M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h6v6h-6z',
  tag: 'M3 12V4h8l10 10-8 8zM8 8h.01',
  layers: 'M12 3l9 5-9 5-9-5 9-5zM3 13l9 5 9-5M3 17l9 5 9-5',
  warehouse: 'M3 10l9-6 9 6v10H3zM9 20v-6h6v6',
  truck: 'M3 7h11v10H3zM14 11h4l3 3v3h-7zM7 19a2 2 0 1 0 0-4 2 2 0 0 0 0 4zM17 19a2 2 0 1 0 0-4 2 2 0 0 0 0 4z',
  building: 'M4 20V6l8-3 8 3v14H4zM9 20v-4h6v4',
  pin: 'M12 21s7-6 7-11a7 7 0 1 0-14 0c0 5 7 11 7 11zM12 10.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z',
  car: 'M4 16v-4l2-5h12l2 5v4M4 16h16M7 16v2M17 16v2M6 12h12',
  cash: 'M3 7h18v10H3zM12 14a2 2 0 1 0 0-4 2 2 0 0 0 0 4z',
  card: 'M3 7h18v10H3zM3 11h18',
  chart: 'M4 19V5M4 19h16M8 15v-4M12 15V8M16 15v-6',
  people: 'M8 12a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM16 13a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM2 20a6 6 0 0 1 12 0M14 20a6 6 0 0 1 8 0',
  report: 'M6 3h9l5 5v13H6zM15 3v5h5M8 13h8M8 17h5',
  settings: 'M12 15.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7zM4 12h2M18 12h2M12 4v2M12 18v2',
  shield: 'M12 3l8 4v6c0 5-3.5 7.5-8 8-4.5-.5-8-3-8-8V7l8-4z',
  map: 'M9 4l-6 2v14l6-2 6 2 6-2V4l-6 2-6-2zM9 4v14M15 6v14',
  alert: 'M12 4l9 16H3zM12 10v4M12 17h.01',
  eye: 'M2 12s4-6 10-6 10 6 10 6-4 6-10 6S2 12 2 12zM12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z',
  eyeOff: 'M3 3l18 18M10.6 10.6A3 3 0 0 0 14.5 14.5M9.9 5.2A10.8 10.8 0 0 1 12 5c6 0 10 7 10 7a18.4 18.4 0 0 1-3.1 3.8M6.1 6.2C3.6 8 2 12 2 12s4 7 10 7a10 10 0 0 0 4.1-.9',
  calendar: 'M7 3v3M17 3v3M4 8h16M5 5h14a1 1 0 0 1 1 1v13H4V6a1 1 0 0 1 1-1z',
  clock: 'M12 7v5l3 2M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z',
  user: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM4 20a8 8 0 0 1 16 0',
  chevronLeft: 'M15 6l-6 6 6 6',
  star: 'M12 3l2.4 5.2L20 9l-4 3.8.9 5.7L12 16l-4.9 2.5.9-5.7L4 9l5.6-.8z',
  qr: 'M4 4h6v2H6v4H4zM14 4h6v6h-2V6h-4zM4 14h2v4h4v2H4zM18 14h2v6h-6v-2h4zM9 9h6v6H9z',
  hash: 'M9 4l-1 16M16 4l-1 16M4 9h16M3 15h16',
  device: 'M4 5h16v10H4zM8 19h8M12 15v4',
};

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-icon',
  standalone: true,
  template: `
    <svg class="icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path [attr.d]="path" />
    </svg>
  `,
})
export class IconComponent {
  @Input() name = 'box';

  get path(): string {
    return ICONS[this.name] ?? ICONS['box'];
  }
}
