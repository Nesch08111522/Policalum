import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./pages/inicio.component').then((m) => m.InicioComponent), title: 'POLICALUM | Policarbonatos y cubiertas' },
  { path: 'catalogo', loadComponent: () => import('./pages/catalogo.component').then((m) => m.CatalogoComponent), title: 'Catálogo | POLICALUM' },
  { path: 'catalogo/:id', loadComponent: () => import('./pages/producto.component').then((m) => m.ProductoComponent), title: 'Producto | POLICALUM' },
  { path: 'carrito', loadComponent: () => import('./pages/carrito.component').then((m) => m.CarritoComponent), title: 'Carrito | POLICALUM' },
  { path: 'cotizar', loadComponent: () => import('./pages/cotizar.component').then((m) => m.CotizarComponent), title: 'Cotización | POLICALUM' },
  { path: 'servicios', loadComponent: () => import('./pages/servicios.component').then((m) => m.ServiciosComponent), title: 'Servicios | POLICALUM' },
  { path: 'portafolio', loadComponent: () => import('./pages/portafolio.component').then((m) => m.PortafolioComponent), title: 'Proyectos | POLICALUM' },
  { path: 'contacto', loadComponent: () => import('./pages/contacto.component').then((m) => m.ContactoComponent), title: 'Contacto | POLICALUM' },
  { path: 'privacidad', loadComponent: () => import('./pages/legal.component').then((m) => m.PrivacidadComponent), title: 'Política de privacidad | POLICALUM' },
  { path: 'terminos', loadComponent: () => import('./pages/legal.component').then((m) => m.TerminosComponent), title: 'Términos y condiciones | POLICALUM' },
  { path: 'envios', loadComponent: () => import('./pages/legal.component').then((m) => m.EnviosComponent), title: 'Envíos y cambios | POLICALUM' },
  { path: 'reclamaciones', loadComponent: () => import('./pages/legal.component').then((m) => m.ReclamacionesComponent), title: 'Libro de reclamaciones | POLICALUM' },
  { path: 'nosotros', loadComponent: () => import('./pages/legal.component').then((m) => m.NosotrosComponent), title: 'Nosotros | POLICALUM' },
  { path: 'blog', loadComponent: () => import('./pages/legal.component').then((m) => m.BlogComponent), title: 'Blog y noticias | POLICALUM' },
  { path: 'garantia', loadComponent: () => import('./pages/legal.component').then((m) => m.GarantiaComponent), title: 'Garantía | POLICALUM' },
  { path: 'faq', loadComponent: () => import('./pages/legal.component').then((m) => m.FaqComponent), title: 'Preguntas frecuentes | POLICALUM' },
  { path: '**', redirectTo: '' },
];
