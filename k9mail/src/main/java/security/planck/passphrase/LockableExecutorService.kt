package security.planck.passphrase

import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

class LockableExecutorService(
    private val service: ExecutorService
) {
    fun execute(runnable: Runnable) {
        if (PassphraseRepository.passphraseUnlocked) {
            service.execute(runnable)
        }
    }

    fun submit(runnable: Runnable): Future<*> {
        return service.submit(
            if (PassphraseRepository.passphraseUnlocked) runnable
            else Runnable {  }
        )
    }
}