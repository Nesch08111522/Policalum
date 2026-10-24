import { CommonModule } from '@angular/common';
import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterModule } from '@angular/router';
import { QUICK_LINKS } from '../../navigation';

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.html',
  styleUrls: ['./navbar.css'],
})
export class NavbarComponent {
  readonly links = QUICK_LINKS;
}
