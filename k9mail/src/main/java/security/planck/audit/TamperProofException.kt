package security.planck.audit

class TamperProofException(
    critical: Boolean? = true,
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)