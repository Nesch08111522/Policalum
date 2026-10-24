import { Component, ChangeDetectionStrategy } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { DashboardRedirectComponent } from './dashboard-redirect';

@Component({
  changeDetection: ChangeDetectionStrategy.Default, standalone: true, template: '' })
class InicioStub {}

describe('DashboardRedirectComponent', () => {
  let component: DashboardRedirectComponent;
  let fixture: ComponentFixture<DashboardRedirectComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardRedirectComponent],
      providers: [provideRouter([{ path: 'inicio', component: InicioStub }])],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardRedirectComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
