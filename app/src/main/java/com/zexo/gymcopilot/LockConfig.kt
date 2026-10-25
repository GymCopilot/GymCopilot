package com.zexo.gymcopilot

/**
 * Configuración del sistema de bloqueo de la aplicación.
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
    const val IS_ROLE_LOCK_ENABLED = 1
}
