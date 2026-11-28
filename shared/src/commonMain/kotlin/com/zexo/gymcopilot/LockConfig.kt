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
     * 0: Muestra todos los roles en la Web (durante desarrollo).
     * 1: Muestra únicamente el perfil del alumno (socio) en la Web.
     */
    const val WEB_ROLE_LOCK_ENABLED = 1

    /**
     * Restricción de plataforma para la App Web (PWA).
     * 0: Permitir acceso web desde cualquier dispositivo (Android, PC, iOS).
     * 1: Permitir acceso web EXCLUSIVAMENTE desde dispositivos iOS.
     */
    const val WEB_IOS_ONLY_ENABLED = 0

    /**
     * Modelo de cobro de licencia de GymCopilot.
     * "admin"  : El gimnasio (Admin) paga la cuota de la plataforma (botón visible en Admin debajo de "Ingresos").
     * "member" : Los alumnos pagan la cuota de la app (botón visible en tarjeta "Estado de Membresía" del alumno).
     */
    const val LICENSE_MODEL_PAYER = "admin"

    /**
     * Control de bloqueo del modelo de licenciamiento para pruebas de desarrollo.
     * 0: Desactivado el bloqueo / Desbloqueado (permite cambiar modalidades libremente para pruebas).
     * 1: Activado el bloqueo (fijación permanente activa, requiere clave dinámica para cambiar).
     */
    const val LICENSE_MODEL_LOCK_ENABLED = 0

    const val MP_ENABLED = true
    const val BILLING_ENABLED = 0

    val isMpButtonVisibleForAdmin: Boolean
        get() = MP_ENABLED && LICENSE_MODEL_PAYER == "admin"

    val isMpButtonVisibleForMember: Boolean
        get() = MP_ENABLED && LICENSE_MODEL_PAYER == "member"

    /**
     * Define el entorno operativo de la aplicación.
     * 0: Entorno de Pruebas (Staging).
     * 1: Entorno de Trabajo (Producción).
     */
    const val APP_ENVIRONMENT = 0

    const val TEST_API_URL = "https://script.google.com/macros/s/AKfycbwq0ckKGgVuyM-HyShu85KLv0y7BbGsokX4vAU7VdtHH27seFYRqu61sedLlp3wgApG/exec"
    const val PRODUCTION_API_URL = "https://script.google.com/macros/s/AKfycbyz74p36j9wu0c8R4Qzs0sxYuFPfN68lboA0K-2Cvzhvnt8pQ394diablRYw4B-TPs0/exec"

    /**
     * Estado de actualización de la App Web.
     * 0: Desaprobada (hay nueva actualización pendiente para actualizar).
     * 1: Aprobada (al día).
     */
    const val WEB_UPDATE_STATUS = 1

    /**
     * Devuelve la URL activa según el entorno configurado (0 = Pruebas, 1 = Trabajo).
     */
    val activeApiUrl: String
        get() = if (APP_ENVIRONMENT == 0) TEST_API_URL else PRODUCTION_API_URL
}
