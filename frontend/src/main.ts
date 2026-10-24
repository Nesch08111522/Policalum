// src/main.ts
import 'zone.js';
import { bootstrapApplication } from '@angular/platform-browser';
import { App } from './app/app'; // ← Importa App desde app.ts
import { appConfig } from './app/app.config';

bootstrapApplication(App, appConfig).catch((err) => console.error(err));
