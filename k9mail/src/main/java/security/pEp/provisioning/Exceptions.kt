package security.pEp.provisioning

import java.lang.RuntimeException

class ProvisioningFailedException(
    message: String? = null,
    throwable: Throwable? = null
) : RuntimeException(message, throwable)

class InitializationFailedException(
    message: String? = null,
    throwable: Throwable? = null
) : RuntimeException(message, throwable)
