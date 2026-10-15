import { CommonModule } from '@angular/common';
import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { BackendApiService, QueryValue } from '../../core/api/backend-api.service';
import { EndpointDefinition } from '../../core/api/api-endpoints.generated';
import { ApiProblem } from '../../core/api/openapi.types';
import { endpointsForModule, moduleById, WorkspaceModule } from './workspace.config';

@Component({
  selector: 'app-workspace-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './workspace-page.component.html',
  styleUrls: ['./workspace-page.component.css'],
})
export class WorkspacePageComponent implements OnInit, OnDestroy {
  @Input() moduleId = '';
  module: WorkspaceModule = moduleById('general');
  endpoints: EndpointDefinition[] = [];
  selected: EndpointDefinition | null = null;
  routeJson = '{\n  "page": 0,\n  "size": 20\n}';
  bodyJson = '{}';
  response: unknown = null;
  error: ApiProblem | null = null;                      // ✅ CAMBIO: ya no es unión
  loading = false;
  private subscription?: Subscription;

  constructor(private readonly route: ActivatedRoute, private readonly api: BackendApiService) {}

  ngOnInit(): void {
    if (this.moduleId) {
      this.loadModule(this.moduleId);
      return;
    }
    this.subscription = this.route.paramMap.subscribe((params) => this.loadModule(params.get('module') ?? 'general'));
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  loadModule(moduleId: string): void {
    this.module = moduleById(moduleId);
    this.endpoints = endpointsForModule(moduleId);
    this.selected = this.endpoints.find((endpoint) => endpoint.method === 'GET') ?? this.endpoints[0] ?? null;
    this.response = null;
    this.error = null;
    this.bodyJson = '{}';
  }

  selectEndpoint(endpoint: EndpointDefinition): void {
    this.selected = endpoint;
    this.response = null;
    this.error = null;
    this.bodyJson = '{}';
  }

  run(): void {
    if (!this.selected) return;
    this.loading = true;
    this.error = null;
    let query: Record<string, unknown>;
    let body: unknown = undefined;
    try {
      query = JSON.parse(this.routeJson || '{}') as Record<string, unknown>;
      body = this.selected.bodyType ? JSON.parse(this.bodyJson || '{}') : undefined;
    } catch {
      this.loading = false;
      this.error = { message: 'El JSON de parámetros o payload no es válido.' };
      return;
    }
    const routeValues = this.selected.parameters.reduce<Record<string, QueryValue>>((values, parameter) => {
      if (this.selected?.path.includes(`{${parameter}}`)) values[parameter] = query[parameter] as QueryValue;
      return values;
    }, {});
    const queryValues: Record<string, QueryValue> = Object.fromEntries(
      Object.entries(query).filter(([key]) => !this.selected?.path.includes(`{${key}}`))
    ) as Record<string, QueryValue>;

    this.api.execute<unknown>(this.selected, routeValues, queryValues, body).subscribe({
      next: (value) => { this.response = value; this.loading = false; },
      error: (value: ApiProblem) => { this.error = value; this.loading = false; },   // ✅ CAMBIO: sin unión
    });
  }

  responseRows(): Array<Record<string, unknown>> {
    if (Array.isArray(this.response)) return this.response.filter((value): value is Record<string, unknown> => typeof value === 'object' && value !== null);
    if (this.response && typeof this.response === 'object') {
      const value = this.response as Record<string, unknown>;
      if (Array.isArray(value['content'])) return value['content'].filter((item): item is Record<string, unknown> => typeof item === 'object' && item !== null);
      return [value];
    }
    return [];
  }

  responseColumns(): string[] {
    const rows = this.responseRows();
    return rows.length ? Object.keys(rows[0]).slice(0, 8) : [];
  }

  format(value: unknown): string {
    if (value === null || value === undefined) return '—';
    if (typeof value === 'object') return JSON.stringify(value);
    return String(value);
  }
}