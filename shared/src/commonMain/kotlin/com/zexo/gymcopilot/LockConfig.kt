package com.zexo.gymcopilot

/**
 * Configuración global del sistema, bloqueo y entorno de la aplicación (Web y Android).
 */
object LockConfig {
    const val LOCK_DAYS = 0

    /**
     * Bloqueo de perfiles específico para Android.
     * 0: Desactivado.
     * 1: Activado.
     */
    const val ANDROID_ROLE_LOCK_ENABLED = 0

    /**
     * Bloqueo / Visibilidad de perfiles específico para la App Web.
     * 0: Muestra todos los roles en la Web.
     * 1: Muestra únicamente el perfil del alumno (socio) en la Web (durante desarrollo).
     */
    const val WEB_ROLE_LOCK_ENABLED = 1

    /**
     * Define el entorno operativo de la aplicación.
     * 0: Entorno de Pruebas (Staging).
     * 1: Entorno de Trabajo (Producción).
     */
    const val APP_ENVIRONMENT = 0

    private const val TEST_API_URL = "https://script.google.com/macros/s/AKfycbx_dcSbPWVqvV48_hCp0L2u5KQ3AWqtfi0e5_wwwD0Fjtbf9FbmKRzZCPCSYw4pvtkz/exec"
    private const val PRODUCTION_API_URL = "https://script.google.com/macros/s/AKfycbzXYYMu43-ATeyxQmwdF3CuRYXi7KbpChyTQEBFIsWh7SCHv2XugxsWen61oBeDdzw/exec"

    /**
     * Devuelve la URL activa según el entorno configurado (0 = Pruebas, 1 = Trabajo).
     */
    val activeApiUrl: String
        get() = if (APP_ENVIRONMENT == 0) TEST_API_URL else PRODUCTION_API_URL
}
