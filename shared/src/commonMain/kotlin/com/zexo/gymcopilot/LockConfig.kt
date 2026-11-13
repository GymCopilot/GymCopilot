package com.zexo.gymcopilot

/**
 * Configuración global del sistema, bloqueo y entorno de la aplicación (Web y Android).
 */
object LockConfig {
    /**
     * Define cuántos días permanecerá desbloqueada la aplicación.
     * 0: Desactiva el sistema de bloqueo.
     * 1+: Días permitidos antes de solicitar el código de desbloqueo.
     * Número negativo (ej. -1): Bloqueo inmediato al ingresar por primera vez.
     */
    const val LOCK_DAYS = 0

    /**
     * Define si el sistema de bloqueo de perfil está activo.
     * 0: Desactivado (Se muestran todos los roles).
     * 1: Activado (Solo se muestra el rol seleccionado previamente).
     */
    const val IS_ROLE_LOCK_ENABLED = 0

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
