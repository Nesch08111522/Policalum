# Frontend completo por módulos

El frontend ahora contiene clases físicas y rutas independientes para cada dominio del backend.

## Estructura por módulo

Cada módulo contiene:

```text
features/<modulo>/
├── data-access/
│   └── <modulo>.api.ts
└── pages/
    ├── <modulo>-page.component.ts
    ├── <modulo>-page.component.html
    └── <modulo>-page.component.css
```

Los módulos con pantalla propia son:

`dashboard`, `ventas`, `clientes`, `cotizaciones`, `preformas`, `devoluciones`, `reembolsos`, `productos`, `servicios`, `categorias`, `catalogo-fracciones`, `stock`, `kardex`, `almacenes`, `compras`, `proveedores`, `despachos`, `movilidades`, `turnos-caja`, `cuentas-por-pagar`, `finanzas`, `facturacion-electronica`, `rrhh`, `reportes`, `auditoria`, `configuracion`, `users`, `roles`, `sedes` y `perfil`.

## Clases comunes

- `core/api/openapi.types.ts`: 246 esquemas TypeScript derivados del backend.
- `core/api/api-endpoints.generated.ts`: 273 operaciones REST.
- `core/api/backend-api.service.ts`: sustitución de parámetros, query string y body HTTP.
- `features/<modulo>/data-access/<modulo>.api.ts`: servicio Angular explícito por dominio, con métodos tipados para sus endpoints.
- `features/<modulo>/pages/<modulo>-page.component.ts`: componente standalone independiente por dominio.
- `app/module.routes.ts`: rutas reales de cada módulo bajo `/modules/<modulo>`. El panel está en `/inicio` y el perfil en `/cuenta/perfil`.

## Ejemplos de uso

```ts
import { VentasApiService } from './features/ventas/data-access/ventas.api';
import { RegistrarVentaRequestDTO } from './core/api/openapi.types';

export class VentaFacade {
  constructor(private readonly ventasApi: VentasApiService) {}

  registrar(request: RegistrarVentaRequestDTO) {
    return this.ventasApi.PostApiV1Ventas({}, request);
  }
}
```

```text
/inicio
/modules/ventas
/modules/compras
/cuenta/perfil
```

Las rutas reales disponibles son `/modules/<id>` usando los IDs listados arriba.

## Regeneración

Desde la raíz:

```bash
python3 scripts/generar_frontend_desde_openapi.py
cd frontend
node ./node_modules/typescript/bin/tsc -p tsconfig.app.json --noEmit
npm run build
```

El build Angular requiere la versión de Node soportada por Angular 22.
