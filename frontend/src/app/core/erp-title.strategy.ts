import { Injectable } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';
import { MarcaService } from '../shared/services/marca.service';

@Injectable()
export class ErpTitleStrategy extends TitleStrategy {
  constructor(private readonly title: Title, private readonly marca: MarcaService) {
    super();
  }

  override updateTitle(snapshot: RouterStateSnapshot): void {
    const page = this.buildTitle(snapshot);
    const marca = this.marca.nombre();
    this.title.setTitle(page ? `${page} · ${marca} ERP` : `${marca} ERP`);
  }
}
