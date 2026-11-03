import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CmsStore } from '../core/cms.store';
import { EmpresaStore } from '../core/empresa.store';
import { ProyectoPublico } from '../core/models';
import { PortalApi, mensajeError } from '../core/portal.api';

@Component({
  selector: 'app-privacidad',
  template: `
    <article class="legal section">
      <h1>Política de privacidad</h1>
      @if (partes().length) {
        @for (parrafo of partes(); track $index) { <p>{{ parrafo }}</p> }
      } @else {
        <p>{{ marca.empresa().nombreComercial }} trata los datos que dejas en el catálogo, la cotización y el contacto para responder tu pedido y llevar el seguimiento comercial.</p>
        <p>Pedimos nombre, RUC, razón social, correo, teléfono, distrito de entrega y el detalle de productos. Esos datos se guardan en el ERP, en cotizaciones o en Mensajes web. El teléfono, el correo y el RUC de la empresa que ves en el sitio son los mismos que figuran en la configuración del ERP.</p>
        <p>Usamos esa información para armar la cotización, avisar a ventas, coordinar la entrega y atender un reclamo. No la vendemos ni la publicamos. El acceso lo tiene el equipo de ventas y administración.</p>
        <p>Los precios de costo, los proveedores y el margen interno no se muestran en este sitio. El carrito queda guardado en tu navegador para que puedas retomarlo; no se envía hasta que cotizas o escribes por el chat.</p>
        <p>Puedes pedir la actualización o el retiro de tus datos de contacto escribiendo al correo publicado en Contacto. Conservamos la cotización el tiempo que la operación comercial y la contabilidad lo exijan.</p>
        <p>Este sitio no procesa pagos. Si más adelante se habilita un cobro en línea, esta política se ampliará antes de usarlo.</p>
      }
    </article>
  `,
})
export class PrivacidadComponent {
  readonly cms = inject(CmsStore);
  readonly marca = inject(EmpresaStore);
  partes(): string[] { return lineasDe(this.cms.paquete()?.textoPrivacidad); }
}

@Component({
  selector: 'app-terminos',
  template: `
    <article class="legal section">
      <h1>Términos y condiciones</h1>
      @if (partes().length) {
        @for (parrafo of partes(); track $index) { <p>{{ parrafo }}</p> }
      } @else {
        <p>El catálogo muestra precios de venta y disponibilidad tomados del inventario de {{ marca.empresa().nombreComercial }}. Una ficha en línea no reserva stock ni cierra una venta.</p>
        <p>Agregar un producto al carrito lo guarda en este navegador. La cotización formal recalcula materiales, utilidad, movilidad y otros cargos con el formato publicado en el ERP. Esa propuesta tiene la vigencia que devuelve el sistema.</p>
        <p>Ventas puede corregir utilidad, pasaje u otros cargos. Si lo hace, el documento indica qué cambió. El acuerdo, el comprobante y la entrega se confirman por WhatsApp, por el chat o en sede.</p>
        <p>La movilidad depende del distrito de Trujillo y de la cantidad de piezas, según la tarifa vigente en el ERP. Un distrito sin tarifa no se puede cotizar.</p>
        <p>Las imágenes y descripciones orientan la compra. La medida, el corte y la disponibilidad se confirman en la cotización atendida por ventas. Este sitio no cobra en línea.</p>
        <p>Usar el sitio implica aceptar estas condiciones y la política de privacidad.</p>
      }
    </article>
  `,
})
export class TerminosComponent {
  readonly cms = inject(CmsStore);
  readonly marca = inject(EmpresaStore);
  partes(): string[] { return lineasDe(this.cms.paquete()?.textoTerminos); }
}

function lineasDe(texto?: string): string[] {
  return (texto ?? '').split(/\n+/).map((linea) => linea.trim()).filter(Boolean);
}

@Component({
  selector: 'app-envios',
  template: `
    <article class="legal section">
      <h1>Envíos y cambios</h1>
      <p>Este sitio no cobra en línea. El despacho, el recojo en sede y cualquier cambio de medida se confirman con ventas por WhatsApp, por el formulario de contacto o al aceptar la cotización.</p>
      <p>El stock publicado es el del almacén al momento de consultar. Una ficha no aparta el material.</p>
      <p>Si el producto llega con un daño de origen, escríbenos con tus datos y el número de cotización o de pedido para revisarlo.</p>
    </article>
  `,
})
export class EnviosComponent {}

@Component({
  selector: 'app-reclamaciones',
  template: `
    <article class="legal section">
      <h1>Libro de reclamaciones</h1>
      <p>Puedes dejar un reclamo o un requerimiento con tu nombre, documento, correo, teléfono y el detalle de lo ocurrido. El mensaje llega a la bandeja de Mensajes web del ERP para que el equipo lo atienda.</p>
      <p>También puedes usar el formulario de contacto y poner en el asunto «Reclamo». Conserva el número de cotización si ya enviaste una.</p>
      <p><a href="/contacto">Ir al formulario de contacto</a></p>
    </article>
  `,
})
export class ReclamacionesComponent {}

@Component({
  selector: 'app-nosotros',
  template: `
    <article class="legal section">
      <p class="kicker">Empresa</p>
      <h1>{{ marca.empresa().nombreComercial }}</h1>
      <p>{{ marca.empresa().razonSocial || marca.empresa().nombreComercial }} atiende obras e industria con policarbonato y accesorios. El catálogo, el stock y la cotización salen del mismo ERP que usa el equipo de ventas.</p>
      @if (marca.empresa().direccion) { <p>Dirección: {{ marca.empresa().direccion }}</p> }
      @if (marca.empresa().telefono) { <p>Teléfono: {{ marca.empresa().telefono }}</p> }
      @if (marca.empresa().email) { <p>Correo: {{ marca.empresa().email }}</p> }
      <p><a routerLink="/contacto">Hablar con el equipo</a></p>
    </article>
  `,
  imports: [RouterLink],
})
export class NosotrosComponent {
  readonly marca = inject(EmpresaStore);
}

@Component({
  selector: 'app-garantia',
  template: `
    <article class="legal section">
      <h1>Garantía de fábrica</h1>
      @if (partes().length) {
        @for (parrafo of partes(); track $index) { <p>{{ parrafo }}</p> }
      } @else {
        <p>La garantía cubre defectos de fabricación del material, no el daño por un corte, una fijación o un almacenaje distintos a la recomendación de ventas.</p>
        <p>Para revisarla necesitamos la cotización o el pedido, fotos del material y la fecha de entrega. El aviso entra por Contacto o por el libro de reclamaciones y queda en Mensajes web.</p>
        <p>El cambio o la reposición se confirma después de esa revisión. Este sitio no reemplaza el comprobante de venta.</p>
      }
    </article>
  `,
})
export class GarantiaComponent {
  readonly cms = inject(CmsStore);
  partes(): string[] { return lineasDe(this.cms.paquete()?.textoGarantia); }
}

@Component({
  selector: 'app-faq',
  template: `
    <article class="legal section">
      <h1>Preguntas frecuentes</h1>
      @if (partes().length) {
        @for (parrafo of partes(); track $index) { <p>{{ parrafo }}</p> }
      } @else {
        <p><strong>¿El precio de la ficha es el final?</strong> Es el precio de venta del almacén. La cotización suma la utilidad, la movilidad del distrito y los otros cargos que estén publicados en el ERP.</p>
        <p><strong>¿Agregar al carrito aparta el stock?</strong> No. El carrito queda en este navegador. El stock se mueve cuando ventas convierte la cotización aceptada en una venta.</p>
        <p><strong>¿Puedo cotizar sin pagar en línea?</strong> Sí. Envías la cotización y el equipo la ve en el ERP, con un aviso. Puedes escribir por WhatsApp desde el mismo resultado.</p>
        <p><strong>¿Qué pasa si el sitio no carga el catálogo?</strong> Si ya lo habías abierto, se muestra el último catálogo guardado en el navegador y un aviso de que no hay conexión con el servidor.</p>
      }
    </article>
  `,
})
export class FaqComponent {
  readonly cms = inject(CmsStore);
  partes(): string[] { return lineasDe(this.cms.paquete()?.textoFaq); }
}

@Component({
  selector: 'app-blog',
  imports: [RouterLink],
  template: `
    <article class="section">
      <p class="kicker">Noticias</p>
      <h1>Blog y obras</h1>
      <p class="lead">Las obras publicadas salen del portafolio del ERP. Si el servidor no responde, lo verás aquí.</p>
      @if (error()) { <p class="alert" role="alert">{{ error() }}</p> }
      @if (!obras().length && !error()) { <p class="muted">Todavía no hay notas publicadas.</p> }
      <div class="product-grid">
        @for (obra of obras(); track obra.id) {
          <article class="tile">
            <h3>{{ obra.titulo }}</h3>
            <p class="muted">{{ obra.descripcion }}</p>
          </article>
        }
      </div>
      <p><a routerLink="/portafolio">Ver proyectos</a></p>
    </article>
  `,
})
export class BlogComponent implements OnInit {
  private readonly api = inject(PortalApi);
  readonly obras = signal<ProyectoPublico[]>([]);
  readonly error = signal('');

  ngOnInit(): void {
    this.api.portafolio().subscribe({
      next: (lista) => this.obras.set(lista),
      error: (error: unknown) => this.error.set(mensajeError(error)),
    });
  }
}
