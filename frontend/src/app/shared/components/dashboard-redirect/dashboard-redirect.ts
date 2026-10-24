import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-dashboard-redirect',
  standalone: true,
  template: '',
})
export class DashboardRedirectComponent implements OnInit {
  constructor(private readonly router: Router) {}

  ngOnInit(): void {
    void this.router.navigate(['/inicio']);
  }
}
