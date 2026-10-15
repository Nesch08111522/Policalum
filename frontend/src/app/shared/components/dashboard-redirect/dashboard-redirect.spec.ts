import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DashboardRedirect } from '../dashboard-redirect';

describe('DashboardRedirect', () => {
  let component: DashboardRedirect;
  let fixture: ComponentFixture<DashboardRedirect>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardRedirect],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardRedirect);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
