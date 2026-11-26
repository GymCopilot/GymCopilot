# GymCopilot - Sistema de Gestión para Gimnasios

Aplicación móvil integral para la gestión de alumnos, profesores, rutinas y tienda, con backend basado en Google Apps Script y Google Sheets.

---

## 🚨 PROTOCOLO OBLIGATORIO PARA AGENTES DE IA / AI AGENTS PROTOCOL 🚨

Este proyecto utiliza un sistema de **"Últimas Mejoras"** para mantener al usuario informado sobre el progreso del desarrollo.

**ES OBLIGATORIO** que todo agente de IA que realice una modificación importante, mejora visual o nueva funcionalidad, documente el cambio en el siguiente archivo:

📂 **Ruta:** `app/src/main/java/com/zexo/gymcopilot/utils/UpdatesRegistry.kt`

### Instrucciones de Registro:
1. Abre `UpdatesRegistry.kt`.
2. Añade el nuevo ítem al principio de la constante `LATEST_UPDATES`.
3. **FILTRO CRÍTICO**: Solo documenta funciones nuevas o mejoras generales genuinas. 
   - **NO** registres cambios menores (posiciones, tamaños, iconos).
   - **NO** registres reparaciones de funciones que ya existían y dejaron de andar por errores técnicos. Al usuario final solo le interesan las mejoras, no los fallos corregidos.
4. Mantén solo las últimas 5-7 entradas de alto valor.

---

## Arquitectura Técnica
- **UI**: Jetpack Compose (Material 3).
- **Lenguaje**: Kotlin.
- **Persistencia Local**: DataStore (Preferences).
- **Backend**: Google Apps Script (Web App) + Google Sheets como DB.
- **Asistente**: Sistema de ayuda visual con personaje "COPI".

## Cómo contribuir
Al realizar cambios en la interfaz, asegúrate de respetar los temas de color (`accentColor`) y los estilos de botones (`containerShape`) configurados dinámicamente por el administrador.
