import { ProyectoPublico } from './models';

export const LINEAS_OBRA: ProyectoPublico[] = [
  {
    id: 'naves',
    titulo: 'Naves y almacenes',
    descripcion: 'Cubiertas corridas para espacios que necesitan luz de día sin abrir filtraciones.',
    orden: 1,
  },
  {
    id: 'fachadas',
    titulo: 'Cerramientos',
    descripcion: 'Fachadas y divisiones que protegen el viento y mantienen la visibilidad.',
    orden: 2,
  },
  {
    id: 'vivienda',
    titulo: 'Techos de vivienda',
    descripcion: 'Terrazas, cocheras y patios con cortes de plancha completa a un cuarto.',
    orden: 3,
  },
  {
    id: 'cenital',
    titulo: 'Tragaluces',
    descripcion: 'Franjas de luz cenital para talleres donde la lámpara no alcanza.',
    orden: 4,
  },
];
