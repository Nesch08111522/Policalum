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
  ['El Porvenir', -8.0839, -79.0031],
  ['Florencia de Mora', -8.0828, -79.0242],
  ['La Esperanza', -8.0706, -79.0464],
  ['Huanchaco', -8.0814, -79.1214],
  ['Víctor Larco Herrera', -8.1292, -79.0472],
  ['Víctor Larco', -8.1292, -79.0472],
  ['Moche', -8.1714, -79.0092],
  ['Laredo', -8.0897, -78.9603],
  ['Salaverry', -8.2214, -78.9764],
  ['Poroto', -8.0111, -78.7669],
  ['Simbal', -7.9736, -78.8131],
  ['El Milagro', -8.0558, -79.0675],
  ['Alto Trujillo', -8.0694, -78.9906],
  ['Ascope', -7.7144, -79.1078],
  ['Chocope', -7.7914, -79.2217],
  ['Casa Grande', -7.7411, -79.1883],
  ['Paiján', -7.7336, -79.3028],
  ['Chicama', -7.8406, -79.1464],
  ['Santiago de Cao', -7.9583, -79.2394],
  ['Magdalena de Cao', -7.8806, -79.2833],
  ['Rázuri', -7.7014, -79.4375],
  ['Cartavio', -7.8903, -79.2211],
  ['Chepén', -7.2275, -79.4286],
  ['Pacanga', -7.1664, -79.4831],
  ['Pueblo Nuevo', -7.1881, -79.5169],
  ['Pacasmayo', -7.4006, -79.5714],
  ['San Pedro de Lloc', -7.4286, -79.5036],
  ['Guadalupe', -7.2431, -79.4708],
  ['Jequetepeque', -7.3372, -79.5631],
  ['Virú', -8.4153, -78.7522],
  ['Chao', -8.5372, -78.6772],
  ['Guadalupito', -8.4319, -78.6403],
  ['Otuzco', -7.9022, -78.5656],
  ['Huamachuco', -7.8122, -78.0489],
  ['Santiago de Chuco', -8.1458, -78.1733],
  ['Quiruvilca', -8.0014, -78.3092],
  ['Cascas', -7.4781, -78.8194],
  ['Julcán', -8.0428, -78.4861],
  ['Bolívar', -7.1531, -77.7008],
  ['Tayabamba', -8.2753, -77.2967],
  ['Parcoy', -8.0333, -77.4833],
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
