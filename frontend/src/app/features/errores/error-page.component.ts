import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MarcaService } from '../../shared/services/marca.service';
import { TokenService } from '../../services/token.service';

interface PaginaError {
  codigo: string;
  kicker: string;
  titulo: string;
  detalle: string;
}

const PAGINAS: Record<string, PaginaError> = {
  '404': {
    codigo: '404',
    kicker: 'Página no encontrada',
    titulo: 'Esta dirección no existe',
    detalle: 'El enlace está mal escrito o el módulo cambió de lugar. Vuelve al panel y entra desde el menú.',
  },
  '500': {
    codigo: '500',
    kicker: 'Error del servidor',
    titulo: 'Algo no salió bien',
    detalle: 'El sistema no pudo completar la acción. Espera un momento y vuelve a intentar. Si se repite, avisa al administrador.',
  },
  permisos: {
    codigo: '403',
    kicker: 'Error de permisos',
    titulo: 'No puedes entrar a esta sección',
    detalle: 'Tu rol no incluye este módulo. Un administrador puede habilitarlo desde Usuarios.',
  },
};

@Component({
  selector: 'app-error-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './error-page.component.html',
  styleUrls: ['./error-page.component.css'],
})
export class ErrorPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly token = inject(TokenService);
  readonly marca = inject(MarcaService);

  get pagina(): PaginaError {
    const codigo = String(this.route.snapshot.data['codigo'] ?? '404');
    return PAGINAS[codigo] ?? PAGINAS['404'];
  }

  get destino(): string {
    return this.token.isAuthenticated() ? '/inicio' : '/login';
  }

  get etiquetaDestino(): string {
    return this.token.isAuthenticated() ? 'Volver al panel' : 'Ir al acceso';
  }

  reintentar(): void {
    const desde = this.route.snapshot.queryParamMap.get('desde');
    if (desde && !desde.startsWith('/error')) {
      void this.router.navigateByUrl(desde);
      return;
    }
    if (window.history.length > 1) {
      window.history.back();
      return;
    }
    void this.router.navigateByUrl(this.destino);
  }
}
