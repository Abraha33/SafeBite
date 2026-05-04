# SafeBite: Asistente Inteligente de Alérgenos con IA 🛡️🍎

**Asignatura:** Tecnologías Móviles  
**Docente:** Fabian Enrique Suarez Carvajal  
**Institución:** Universidad Autónoma de Bucaramanga (UNAB)  
**Estudiante:** Abraham Caceres Salazar  

---

## 📌 1. Objetivo del Proyecto

* **Qué:** Desarrollo de una aplicación móvil nativa en Android con Kotlin que utiliza visión artificial para la detección de ingredientes.
* **Cómo:** Implementando **Google ML Kit (Text Recognition v2)** para el análisis de imágenes en tiempo real y **Room Database** para la persistencia de datos.
* **Para qué:** Proporcionar una herramienta de seguridad alimentaria que permita a usuarios con alergias o restricciones dietéticas identificar ingredientes peligrosos de forma instantánea y fiable.

---

## 🚀 2. Características Principales

* **Escaneo OCR en Tiempo Real:** Identificación de texto en etiquetas mediante la cámara del dispositivo a través de **CameraX**.
* **Gestión de Perfil de Alérgenos:** Formulario validado para configurar qué ingredientes específicos debe rastrear la IA.
* **Historial de Consultas:** Listado persistente de los productos analizados mediante un `RecyclerView`.
* **Alertas Visuales (HUD):** Interfaz dinámica que cambia de color según el nivel de riesgo detectado (verde para seguro, rojo para peligro).

---

## 🛠️ 3. Stack Tecnológico

* **Lenguaje:** Kotlin (100%).
* **Arquitectura:** MVVM (Model-View-ViewModel) para una separación de responsabilidades limpia.
* **IA de Google:** ML Kit Text Recognition para el motor de procesamiento.
* **Persistencia:** Room Database (SQLite) para el almacenamiento local de datos.
* **Cámara:** CameraX para la gestión del hardware de captura de imagen.
* **Interfaz:** Material Design 3 con componentes dinámicos y adaptables.

---

## ✅ 4. Cumplimiento de Requerimientos (Checklist)

| Requerimiento | Estado | Implementación Técnica |
| :--- | :---: | :--- |
| **Problemática Real** | ✅ | Soluciona el riesgo de salud por lectura errónea o dificultosa de etiquetas. |
| **Persistencia Local** | ✅ | Uso de Room para guardar el perfil del usuario y el historial de escaneos. |
| **Clases POO** | ✅ | Modelos de datos estructurados con atributos privados y métodos de acceso. |
| **RecyclerView** | ✅ | Listado dinámico de historial con eventos de interacción `onItemClick`. |
| **Formularios** | ✅ | Pantallas de registro con validaciones de entrada para integridad de datos. |
| **Cámara / IA** | ✅ | Integración de CameraX con el SDK de ML Kit para detección inteligente. |

---

## 📂 5. Estructura del Proyecto

```text
app/src/main/java/com/safebite/
├── data/           # Configuración de Room (Database, DAOs)
├── model/          # Clases Modelo POO (User, Allergen, ProductScan)
├── ui/             # Activities, Fragments y Adapters del RecyclerView
├── viewmodel/      # Lógica de negocio y estados de la UI
└── utils/          # Procesador de visión artificial y analizadores de imagen
```

---

## ⚙️ 6. Instalación y Configuración

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/tu-usuario/safebite.git
   ```
2. **Abrir en Android Studio:** Utilizar la versión Ladybug (2024.2.1) o superior.
3. **Sincronizar Gradle:** Asegurarse de descargar las dependencias de Google ML Kit y Room.
4. **Ejecución:** Se recomienda el uso de un dispositivo físico para probar la funcionalidad de la cámara y el rendimiento de la IA en tiempo real.

---

## 🎨 7. Diseño (Mockups)

El diseño de la aplicación se basó en el flujo de experiencia de usuario definido en Figma, priorizando la accesibilidad (A11y) y la respuesta visual inmediata.

[Enlace a los Mockups en Figma](TU_LINK_AQUI)

---

## 📄 8. Información de Entrega

* **Autor:** Abraham Caceres Salazar
* **Grupo:** Proyecto Final - Grupo 6 (10 AM)
* **Fecha de Entrega:** Mayo 2026
