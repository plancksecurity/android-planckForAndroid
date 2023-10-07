package security.planck.audit

class LogBadlyFormattedException(
    log: String,
    override val cause: Throwable?
): RuntimeException("Log is badly formatted: $log", cause)