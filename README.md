# ErpPoliCalum — ERP Full Stack

ERP para ventas de policarbonato por fracciones, servicios, compras, inventario/Kardex, caja, RRHH, despachos, finanzas y reportes.

## Arquitectura

- `backend/`: Spring Boot 4, Java 17, PostgreSQL, JPA, JWT, validación y OpenAPI.
- `frontend/`: Angular standalone, TypeScript, RxJS y diseño responsive.
- `backend/docs/openapi.json`: contrato REST generado/documentado.
- `scripts/empaquetar_proyecto.py`: crea un ZIP limpio y reproducible.

## Requisitos

- Java 17 o superior compatible con el `pom.xml`.
- Node.js compatible con Angular 22 (`22.22.3+` o una versión LTS compatible).
- PostgreSQL 15+.
- Python 3.10+ para empaquetar.

## Ejecución local

### 1. Base de datos

```bash
docker compose up -d postgres
```

Si no usas Docker, crea una base PostgreSQL llamada `ERPpoli` y configura las variables del backend.

### 2. Backend

```bash
cd backend
cp .env.example .env 2>/dev/null || true
export SPRING_PROFILES_ACTIVE=dev
./mvnw spring-boot:run
```

API: `http://localhost:8080`  
OpenAPI: `http://localhost:8080/v3/api-docs`  
Swagger UI: `http://localhost:8080/swagger-ui.html`

### 3. Frontend

```bash
cd frontend
npm ci
npm start
```

Aplicación: `http://localhost:4200`

## Seguridad y configuración

No se deben commitear secretos reales. Usa variables de entorno para `DB_PASSWORD`, `JWT_SECRET`, `TOTP_PUBLIC_SECRET` y CORS. El perfil `prod` desactiva SQL visible, `ddl-auto=update` y documentación pública.

## Empaquetado

Desde la raíz:

```bash
python3 scripts/empaquetar_proyecto.py
```

El ZIP se crea en `dist/ErpPoliCalum-reconstruido.zip` excluyendo `.git`, `node_modules`, `target`, caches, credenciales locales y archivos compilados.

## Alcance de la reconstrucción

Se conserva la lógica de negocio existente y se añaden convenciones de ejecución, configuración y empaquetado. La fuente de verdad de permisos es `backend/docs/API-ROLES.md`; el contrato de integración es OpenAPI. Antes de producción deben ejecutarse los tests del backend con JDK 17 y el build Angular con una versión de Node compatible.
