#!/usr/bin/env python3
"""Genera la colección de Postman y recorre los GET del ERP contra localhost."""
import json
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

BASE = "http://localhost:8080"
ROOT = Path("/workspace/backend/src/main/java/com/policalum/erp/infrastructure/controller")
AQUI = Path(__file__).resolve().parent

SKIP = {
    "/api/v1/sedes/imagenes",
    "/api/v1/productos/imagenes",
    "/api/v1/almacenes/validar-nombre",
    "/api/v1/catalogo-fracciones/validar-factor",
}
ESPERADO = {
    "/api/v1/configuracion/logo": 404,
    "/api/v1/rrhh/legajos/me": 404,
    "/api/v1/perfil/foto": 404,
    "/api/v1/proveedores/documento": 404,
}
OVERRIDE = {
    "/api/v1/sedes/estado": "activa=true&page=0&size=5",
    "/api/v1/exportacion": "modulo=clientes&formato=xlsx",
    "/api/v1/productos/check-sku": "codigoSku=NO-EXISTE-QA",
    "/api/v1/finanzas/utilidad-neta/periodo": "periodo=2026-10",
    "/api/v1/rrhh/comisiones/periodo": "periodo=2026-10",
    "/api/v1/clientes/validar/documento": "numeroDocumento=00000000&tipoDocumento=DNI",
    "/api/v1/clientes/validar/email": "email=nadie@policalum.com",
    "/api/v1/proveedores/documento": "tipoDocumento=RUC&numeroDocumento=00000000000",
    "/api/v1/proveedores/validar/documento": "tipoDocumento=RUC&numeroDocumento=00000000000",
    "/api/v1/categorias/validar-slug": "slug=no-existe-qa",
    "/api/v1/servicios/check-nombre": "nombre=no-existe-qa",
    "/api/v1/reportes/ventas/vendedores": "desde=2026-01-01&hasta=2026-10-25&top=5",
    "/api/v1/reportes/ventas/resumen": "desde=2026-01-01&hasta=2026-10-25",
    "/api/v1/reportes/ventas": "desde=2026-01-01&hasta=2026-10-25&page=0&size=5",
    "/api/v1/finanzas/flujo-caja": "desde=2026-01-01&hasta=2026-10-25",
    "/api/v1/finanzas/flujo-caja/devengado": "desde=2026-01-01&hasta=2026-10-25",
    "/api/v1/finanzas/flujo-caja/proyeccion": "desde=2026-01-01&hasta=2026-10-25",
    "/api/v1/finanzas/dashboard/evolucion-mensual": "anio=2026",
    "/api/v1/finanzas/gastos": "page=0&size=5",
}


def rutas():
    found = []
    for java in sorted(ROOT.glob("*Controller.java")):
        text = java.read_text()
        base = re.search(r'@RequestMapping\("([^"]+)"\)', text)
        if not base:
            continue
        prefix = base.group(1)
        for match in re.finditer(r'@GetMapping(?:\("([^"]*)"\))?', text):
            path = prefix + (match.group(1) or "")
            if "{" in path or path in SKIP:
                continue
            found.append(path)
    return found


def url_de(path):
    query = OVERRIDE.get(path, "page=0&size=5")
    return path + (("?" + query) if query else "")


def call(method, path, token, payload=None):
    data = None if payload is None else json.dumps(payload).encode()
    headers = {"Accept": "*/*"}
    if payload is not None:
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=40) as res:
            return res.status, res.read()
    except urllib.error.HTTPError as err:
        return err.code, err.read()


def login(email, password):
    code, body = call("POST", "/api/v1/auth/login", None, {"email": email, "password": password})
    data = json.loads(body)
    return code, data["token"], data.get("refreshToken")


def escribir_coleccion(paths):
    def item(name, method, path, token_var, tests, body=None):
        headers = [{"key": "Accept", "value": "*/*"}]
        if token_var:
            headers.append({"key": "Authorization", "value": "Bearer {{" + token_var + "}}"})
        if body is not None:
            headers.append({"key": "Content-Type", "value": "application/json"})
        return {
            "name": name,
            "request": {
                "method": method,
                "header": headers,
                "url": "{{baseUrl}}" + path,
                **({"body": {"mode": "raw", "raw": body}} if body is not None else {}),
            },
            "event": [{"listen": "test", "script": {"type": "text/javascript", "exec": tests}}],
        }

    login_test = [
        "pm.test('login 200', () => pm.response.to.have.status(200));",
        "var sesionA = pm.response.json();",
        "pm.environment.set('tokenA', sesionA.token);",
        "pm.environment.set('refreshA', sesionA.refreshToken);",
    ]
    login_b = [
        "pm.test('segunda sesion 200', () => pm.response.to.have.status(200));",
        "var sesionB = pm.response.json();",
        "pm.expect(sesionB.token).to.not.eql(pm.environment.get('tokenA'));",
        "pm.environment.set('tokenB', sesionB.token);",
        "pm.environment.set('refreshB', sesionB.refreshToken);",
    ]
    gets = []
    for path in paths:
        esperado = ESPERADO.get(path, 200)
        gets.append(item(
            f"GET {path}",
            "GET",
            url_de(path),
            "tokenA",
            [f"pm.test('GET {path}', () => pm.response.to.have.status({esperado}));"],
        ))
    sesion = [
        item("Login sede 1", "POST", "/api/v1/auth/login", None, login_test,
             '{"email":"{{email}}","password":"{{password}}"}'),
        item("Login sede 2", "POST", "/api/v1/auth/login", None, login_b,
             '{"email":"{{email}}","password":"{{password}}"}'),
        *gets,
        item("Empleado no entra a gastos", "POST", "/api/v1/auth/login", None, [
            "pm.test('empleado entra', () => pm.response.to.have.status(200));",
            "var sesionEmpleado = pm.response.json();",
            "pm.environment.set('tokenEmpleado', sesionEmpleado.token);",
        ], '{"email":"{{emailEmpleado}}","password":"{{passwordEmpleado}}"}'),
        item("Gastos con empleado", "GET", "/api/v1/finanzas/gastos?page=0&size=5", "tokenEmpleado",
             ["pm.test('RBAC 403', () => pm.response.to.have.status(403));"]),
        item("Cerrar solo la sesion A", "POST", "/api/v1/auth/logout", "tokenA",
             ["pm.test('logout 204', () => pm.response.to.have.status(204));"],
             '{"refreshToken":"{{refreshA}}"}'),
        item("La sesion A ya no responde", "GET", "/api/v1/perfil", "tokenA",
             ["pm.test('A cerrada', () => pm.response.to.have.status(401));"]),
        item("La sesion B sigue viva", "GET", "/api/v1/perfil", "tokenB",
             ["pm.test('B viva', () => pm.response.to.have.status(200));"]),
        item("El refresh A quedo revocado", "POST", "/api/v1/auth/refresh", None,
             ["pm.test('refresh A invalido', () => pm.expect(pm.response.code).to.be.oneOf([401,403]));"],
             '{"refreshToken":"{{refreshA}}"}'),
        item("El refresh B sigue vigente", "POST", "/api/v1/auth/refresh", None,
             ["pm.test('refresh B vigente', () => pm.response.to.have.status(200));"],
             '{"refreshToken":"{{refreshB}}"}'),
    ]
    coleccion = {
        "info": {
            "name": "POLICALUM ERP",
            "description": "Barrido de consultas y dos sesiones simultaneas del mismo administrador. Importar en la extension de Postman junto con el entorno local.",
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
        },
        "item": sesion,
    }
    entorno = {
        "name": "POLICALUM local",
        "values": [
            {"key": "baseUrl", "value": BASE, "enabled": True},
            {"key": "email", "value": "admin@policalum.com", "enabled": True},
            {"key": "password", "value": "admin123", "enabled": True},
            {"key": "emailEmpleado", "value": "empleado@policalum.com", "enabled": True},
            {"key": "passwordEmpleado", "value": "empleado123", "enabled": True},
            {"key": "tokenA", "value": "", "enabled": True},
            {"key": "tokenB", "value": "", "enabled": True},
            {"key": "tokenEmpleado", "value": "", "enabled": True},
            {"key": "refreshA", "value": "", "enabled": True},
            {"key": "refreshB", "value": "", "enabled": True},
        ],
    }
    (AQUI / "POLICALUM.postman_collection.json").write_text(json.dumps(coleccion, indent=2, ensure_ascii=False) + "\n")
    (AQUI / "POLICALUM.postman_environment.json").write_text(json.dumps(entorno, indent=2, ensure_ascii=False) + "\n")


def main():
    paths = rutas()
    escribir_coleccion(paths)
    if "--solo-coleccion" in sys.argv:
        print(f"coleccion {len(paths)} GET")
        return
    code_a, token_a, refresh_a = login("admin@policalum.com", "admin123")
    code_b, token_b, refresh_b = login("admin@policalum.com", "admin123")
    print(f"login A {code_a} login B {code_b} distintos {token_a != token_b}")
    fallos = []
    for path in paths:
        code, body = call("GET", url_de(path), token_a)
        esperado = ESPERADO.get(path, 200)
        if code != esperado:
            fallos.append(f"{code}!={esperado} {path} {body[:160]!r}")
    code_e, token_e, _ = login("empleado@policalum.com", "empleado123")
    code_g, _ = call("GET", "/api/v1/finanzas/gastos?page=0&size=5", token_e)
    if code_e != 200 or code_g != 403:
        fallos.append(f"rbac empleado {code_e} gastos {code_g}")
    code_out, _ = call("POST", "/api/v1/auth/logout", token_a, {"refreshToken": refresh_a})
    code_pa, _ = call("GET", "/api/v1/perfil", token_a)
    code_pb, _ = call("GET", "/api/v1/perfil", token_b)
    code_ra, _ = call("POST", "/api/v1/auth/refresh", None, {"refreshToken": refresh_a})
    code_rb, _ = call("POST", "/api/v1/auth/refresh", None, {"refreshToken": refresh_b})
    if not (code_out == 204 and code_pa == 401 and code_pb == 200 and code_ra in (401, 403) and code_rb == 200):
        fallos.append(f"sesiones logout {code_out} A {code_pa} B {code_pb} refreshA {code_ra} refreshB {code_rb}")
    print(f"GET {len(paths) - len([f for f in fallos if f.startswith(('4','5'))])} rutas, fallos {len(fallos)}")
    for fallo in fallos:
        print(fallo)
    if fallos:
        sys.exit(1)


if __name__ == "__main__":
    main()
