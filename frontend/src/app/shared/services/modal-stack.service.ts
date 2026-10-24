import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ModalStackService {
  private readonly closers: Array<() => void> = [];

  register(close: () => void): void {
    this.closers.push(close);
    document.body.classList.add('modal-open');
  }

  unregister(close: () => void): void {
    const index = this.closers.lastIndexOf(close);
    if (index >= 0) this.closers.splice(index, 1);
    if (this.closers.length === 0) document.body.classList.remove('modal-open');
  }

  isTop(close: () => void): boolean {
    return this.closers.at(-1) === close;
  }
}
