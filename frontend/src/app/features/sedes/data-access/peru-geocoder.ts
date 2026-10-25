/** Ciudades y distritos del Perú. La búsqueda es local: no llama a internet. */
const LUGARES: ReadonlyArray<readonly [string, number, number]> = [
  ['Lima', -12.0464, -77.0428],
  ['Miraflores', -12.1211, -77.0297],
  ['San Isidro', -12.0969, -77.0365],
  ['Barranco', -12.1439, -77.0203],
  ['Surco', -12.146, -76.991],
  ['Santiago de Surco', -12.146, -76.991],
  ['San Borja', -12.096, -76.9956],
  ['La Molina', -12.0796, -76.9163],
  ['Surquillo', -12.1136, -77.012],
  ['Magdalena', -12.0918, -77.0715],
  ['Pueblo Libre', -12.0776, -77.0628],
  ['Jesús María', -12.075, -77.0433],
  ['Lince', -12.084, -77.031],
  ['San Miguel', -12.0778, -77.0936],
  ['Breña', -12.059, -77.048],
  ['La Victoria', -12.065, -77.031],
  ['Rímac', -12.027, -77.035],
  ['Los Olivos', -11.959, -77.0708],
  ['San Martín de Porres', -12.012, -77.060],
  ['Independencia', -11.996, -77.054],
  ['Comas', -11.933, -77.058],
  ['San Juan de Lurigancho', -11.991, -76.999],
  ['San Juan de Miraflores', -12.160, -76.97],
  ['Villa El Salvador', -12.213, -76.937],
  ['Villa María del Triunfo', -12.162, -76.94],
  ['Chorrillos', -12.176, -77.017],
  ['Ate', -12.026, -76.917],
  ['Santa Anita', -12.043, -76.971],
  ['Callao', -12.0566, -77.1181],
  ['Ventanilla', -11.877, -77.126],
  ['Arequipa', -16.409, -71.5375],
  ['Trujillo', -8.1116, -79.0288],
  ['Cusco', -13.5319, -71.9675],
  ['Cuzco', -13.5319, -71.9675],
  ['Piura', -5.1945, -80.6328],
  ['Chiclayo', -6.7714, -79.8409],
  ['Iquitos', -3.7437, -73.2516],
  ['Huancayo', -12.0651, -75.2045],
  ['Tacna', -18.0066, -70.2463],
  ['Chimbote', -9.0745, -78.5936],
  ['Pucallpa', -8.3791, -74.5539],
  ['Juliaca', -15.4997, -70.1297],
  ['Ica', -14.0755, -75.7342],
  ['Cajamarca', -7.1617, -78.5128],
  ['Puno', -15.8402, -70.0219],
  ['Ayacucho', -13.1631, -74.2246],
  ['Huánuco', -9.9306, -76.2422],
  ['Huaraz', -9.5278, -77.5278],
  ['Tarapoto', -6.4825, -76.373],
  ['Tumbes', -3.5669, -80.4515],
  ['Huacho', -11.107, -77.605],
  ['Chincha', -13.417, -76.132],
  ['Pisco', -13.71, -76.203],
  ['Moquegua', -17.193, -70.935],
  ['Puerto Maldonado', -12.593, -69.189],
];

export interface LugarPeru {
  nombre: string;
  latitud: number;
  longitud: number;
}

export function buscarLugar(texto: string): LugarPeru | null {
  const consulta = normalizar(texto);
  if (!consulta) return null;
  let mejor: LugarPeru | null = null;
  for (const [nombre, latitud, longitud] of LUGARES) {
    const clave = normalizar(nombre);
    if (!contienePalabra(consulta, clave)) continue;
    if (!mejor || clave.length > normalizar(mejor.nombre).length) mejor = { nombre, latitud, longitud };
  }
  return mejor;
}

function contienePalabra(texto: string, clave: string): boolean {
  let desde = 0;
  while (desde <= texto.length - clave.length) {
    const indice = texto.indexOf(clave, desde);
    if (indice < 0) return false;
    const izquierda = indice === 0 || !esLetra(texto[indice - 1]);
    const fin = indice + clave.length;
    const derecha = fin === texto.length || !esLetra(texto[fin]);
    if (izquierda && derecha) return true;
    desde = indice + 1;
  }
  return false;
}

function esLetra(caracter: string): boolean {
  return /[a-z0-9]/i.test(caracter);
}

function normalizar(valor: string): string {
  return valor
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '');
}
