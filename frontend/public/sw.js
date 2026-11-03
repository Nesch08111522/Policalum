self.addEventListener('push', (event) => {
  let datos = { title: 'POLICALUM', body: 'Tienes un aviso nuevo.', url: '/modules/notificaciones' };
  try {
    datos = { ...datos, ...(event.data ? event.data.json() : {}) };
  } catch (error) {
    datos.body = event.data ? event.data.text() : datos.body;
  }
  event.waitUntil(self.registration.showNotification(datos.title, {
    body: datos.body,
    data: { url: datos.url },
  }));
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const destino = event.notification.data && event.notification.data.url ? event.notification.data.url : '/modules/notificaciones';
  event.waitUntil(clients.openWindow(destino));
});
