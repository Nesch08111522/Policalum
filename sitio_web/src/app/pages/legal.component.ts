import { Component } from '@angular/core';

@Component({
  selector: 'app-privacidad',
  template: `
    <article class="legal section">
      <h1>Política de privacidad</h1>
      <p>POLICALUM trata los datos que dejas en el catálogo, la cotización y el formulario de contacto para responder tu pedido y llevar el seguimiento comercial.</p>
      <p>Pedimos nombre, RUC, razón social, correo, teléfono y el detalle de productos. Esos datos se guardan en el ERP, en cotizaciones o en la bandeja de mensajes web.</p>
      <p>No vendemos esa información. El acceso lo tiene el equipo de ventas y administración de la empresa. Los precios de costo y los proveedores no se publican en este sitio.</p>
      <p>Puedes pedir la actualización o el retiro de tus datos de contacto escribiendo al correo publicado en la página de contacto. Conservamos la cotización el tiempo que la operación comercial y la contabilidad lo exijan.</p>
      <p>Este sitio no procesa pagos. Si más adelante se habilita una pasarela, esta política se ampliará antes de cobrar en línea.</p>
    </article>
  `,
})
export class PrivacidadComponent {}

@Component({
  selector: 'app-terminos',
  template: `
    <article class="legal section">
      <h1>Términos y condiciones</h1>
      <p>El catálogo muestra precios de venta y disponibilidad tomados del inventario. Una ficha en línea no reserva stock ni constituye una venta cerrada.</p>
      <p>El carrito de WhatsApp arma un mensaje para el equipo de ventas. El acuerdo, el comprobante y la entrega se confirman por ese canal o en tienda.</p>
      <p>La cotización formal es una propuesta. Su vigencia es la fecha que devuelve el sistema. POLICALUM puede ajustarla si el stock o el precio cambian antes de aceptarla.</p>
      <p>Las imágenes y descripciones orientan la compra. La medida, el color y el corte se confirman en la cotización o en el pedido atendido por ventas.</p>
      <p>El uso de este sitio implica aceptar estas condiciones y la política de privacidad.</p>
    </article>
  `,
})
export class TerminosComponent {}
