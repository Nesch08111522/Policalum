import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { VentasApiService, Producto, Servicio, Fraccion, Cliente, Sede, Almacen, Turno } from './ventas-api.service';
interface Linea { tipo: 'PRODUCTO'|'SERVICIO'; id: string; nombre: string; precio: number; cantidad: number; fraccion?: Fraccion; }
@Component({ selector: 'app-pos', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './pos.component.html', styleUrl: './ventas.css' })
export class PosComponent implements OnInit {
  productos: Producto[]=[]; servicios: Servicio[]=[]; clientes: Cliente[]=[]; sedes: Sede[]=[]; almacenes: Almacen[]=[]; carrito: Linea[]=[]; sedeId=''; almacenId=''; clienteId=''; turno?: Turno; montoInicial=0; busqueda=''; mensaje=''; error='';
  constructor(private api: VentasApiService) {}
  ngOnInit(): void { this.api.sedes().subscribe({next:p=>{this.sedes=p.content.filter(x=>x.activa); if(this.sedes[0])this.seleccionarSede(this.sedes[0].id)},error:()=>this.error='No se pudieron cargar las sedes.'}); this.cargarCatalogos(); this.api.clientes().subscribe(p=>this.clientes=p.content); }
  cargarCatalogos():void { this.api.productos(this.busqueda).subscribe(p=>this.productos=p.content); this.api.servicios(this.busqueda).subscribe(p=>this.servicios=p.content); }
  seleccionarSede(id:string):void { this.sedeId=id; this.almacenId=''; this.api.almacenes(id).subscribe(a=>this.almacenes=a.filter(x=>x.activo)); }
  agregarProducto(p:Producto):void { if(p.esFraccionable) this.api.fracciones(p.id).subscribe(f=>{if(!f.length){this.error='El producto fraccionable no tiene fracciones activas.';return}this.agregar({tipo:'PRODUCTO',id:p.id,nombre:p.nombre,precio:f[0].precioVenta,cantidad:1,fraccion:f[0]})}); else this.agregar({tipo:'PRODUCTO',id:p.id,nombre:p.nombre,precio:p.precioVentaBase,cantidad:1}); }
  agregarServicio(s:Servicio):void { this.agregar({tipo:'SERVICIO',id:s.id,nombre:s.nombre,precio:s.precio,cantidad:1}); }
  agregar(l:Linea):void { const x=this.carrito.find(v=>v.tipo===l.tipo&&v.id===l.id&&v.fraccion?.id===l.fraccion?.id); if(x)x.cantidad+=1;else this.carrito=[...this.carrito,l]; }
  total():number{return this.carrito.reduce((a,x)=>a+x.precio*x.cantidad,0)}
  abrirCaja():void { if(!this.sedeId||this.montoInicial<0)return; this.api.abrirCaja({sedeId:this.sedeId,montoInicial:this.montoInicial}).subscribe({next:t=>{this.turno=t;this.mensaje='Caja abierta correctamente.'},error:e=>this.error=e.error?.message||'No se pudo abrir la caja.'}); }
  cobrar():void { this.error='';if(!this.turno||!this.almacenId||!this.carrito.length){this.error='Abre caja, selecciona almacén y agrega al menos una línea.';return}const total=+this.total().toFixed(2),data={sedeId:this.sedeId,almacenId:this.almacenId,clienteId:this.clienteId||null,turnoCajaId:this.turno.id,tipoComprobante:'BOLETA',canalVenta:'LOCAL',lineas:this.carrito.map(x=>x.tipo==='PRODUCTO'?{productoId:x.id,fraccionId:x.fraccion?.id||null,cantidad:x.cantidad}:{servicioId:x.id,cantidad:x.cantidad}),pagos:[{metodoPago:'EFECTIVO',monto:total}]};this.api.registrarVenta(data).subscribe({next:()=>{this.carrito=[];this.mensaje='Venta registrada y Kardex actualizado.'},error:e=>this.error=e.error?.message||'No se pudo registrar la venta.'}); }
}
