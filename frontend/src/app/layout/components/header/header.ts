import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, HostListener, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import * as Api from '../../../core/api/openapi.types';
import { Notificacion, NotificacionesApi } from '../../../features/notificaciones/notificaciones.api';
import { AuthService } from '../../../services/auth.service';
import { TokenService } from '../../../services/token.service';
import { IconComponent } from '../../../shared/components/icon/icon.component';
import { LayoutService } from '../../../shared/services/layout.service';
import { LookupService } from '../../../shared/services/lookup.service';
import { CajaGateService } from '../../../shared/services/caja-gate.service';
import { AvatarService } from '../../../shared/services/avatar.service';
import { SedeFiltroService } from '../../../shared/services/sede-filtro.service';
import { ThemeService } from '../../../shared/services/theme.service';
import { NAV_SECTIONS, NavItem } from '../../navigation';

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule, IconComponent],
  templateUrl: './header.html',
  styleUrls: ['./header.css'],
})
export class HeaderComponent implements OnInit {
  query = '';
  results: NavItem[] = [];
  menuOpen = false;
  sedesAbiertas = false;
  avisosAbiertos = false;
  avisos: Notificacion[] = [];
  noLeidas = 0;
  sedes: Api.SedeResponseDTO[] = [];
  private items: NavItem[] = [];

  constructor(
    readonly theme: ThemeService,
    readonly sedeFiltro: SedeFiltroService,
    private readonly layout: LayoutService,
    private readonly auth: AuthService,
    private readonly tokens: TokenService,
    private readonly lookup: LookupService,
    private readonly caja: CajaGateService,
    readonly avatar: AvatarService,
    private readonly notificaciones: NotificacionesApi,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const role = this.tokens.session()?.rol ?? '';
    this.items = NAV_SECTIONS.flatMap((section) => section.items).filter((item) => !item.roles || item.roles.includes(role));
    this.lookup.loadSedes().subscribe((rows) => { this.sedes = rows.filter((sede) => sede.activa !== false); });
    this.cargarAvisos();
  }

  cargarAvisos(): void {
    this.notificaciones.listar().pipe(catchError(() => of([] as Notificacion[]))).subscribe((lista) => {
      const nuevas = lista.filter((aviso) => !aviso.leida && !aviso.eliminada);
      this.noLeidas = nuevas.length;
      this.avisos = nuevas.slice(0, 6);
      this.cdr.detectChanges();
    });
  }

  abrirAviso(aviso: Notificacion): void {
    this.avisosAbiertos = false;
    const ir = () => void this.router.navigateByUrl(aviso.ruta || '/modules/notificaciones');
    if (aviso.leida) { ir(); return; }
    this.notificaciones.leida(aviso.id).subscribe({ next: () => this.cargarAvisos(), error: () => undefined });
    ir();
  }

  elegirSede(id: string, nombre: string): void {
    this.sedeFiltro.elegir(id, nombre);
    this.sedesAbiertas = false;
    this.caja.revisar();
  }

  onQuery(event: Event): void {
    this.query = (event.target as HTMLInputElement).value;
    const term = this.query.trim().toLowerCase();
    this.results = term ? this.items.filter((item) => (item.label ?? '').toLowerCase().includes(term)).slice(0, 6) : [];
  }

  goFirst(): void {
    const target = this.results[0];
    if (!target) return;
    void this.router.navigateByUrl(target.path);
    this.clear();
  }

  clear(): void {
    this.query = '';
    this.results = [];
  }

  toggleMenu(): void {
    if (window.innerWidth <= 900) this.layout.toggleMobile();
    else this.layout.toggleCollapsed();
  }

  get userName(): string { return this.tokens.session()?.nombre || 'Usuario'; }
  get userEmail(): string { return this.tokens.session()?.email || ''; }
  get userRole(): string { return this.tokens.session()?.rol === 'ROLE_ADMIN' ? 'Administrador' : this.tokens.session() ? 'Multifuncional' : ''; }
  get initials(): string {
    const partes = (this.userName || 'P').trim().split(/\s+/).slice(0, 2);
    return partes.map((parte) => parte.charAt(0).toUpperCase()).join('') || 'P';
  }
  get esAdmin(): boolean { return this.tokens.session()?.rol === 'ROLE_ADMIN'; }

  ir(ruta: string): void {
    this.menuOpen = false;
    void this.router.navigateByUrl(ruta);
  }

  logout(): void {
    this.auth.logout();
    this.menuOpen = false;
  }

  @HostListener('document:click', ['$event'])
  closeMenus(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.dropdown')) {
      this.menuOpen = false;
      this.sedesAbiertas = false;
      this.avisosAbiertos = false;
    }
    if (!target.closest('.search-shell')) this.results = [];
  }
}
