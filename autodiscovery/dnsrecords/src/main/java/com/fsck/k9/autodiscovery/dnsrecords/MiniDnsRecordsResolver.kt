package com.fsck.k9.autodiscovery.dnsrecords

import androidx.annotation.WorkerThread
import org.minidns.hla.DnssecResolverApi
import org.minidns.record.MX
import timber.log.Timber

class MiniDnsRecordsResolver : DnsRecordsResolver {
    @WorkerThread
    override fun getRealDomain(domain: String): String {
        var realDomain = domain
        kotlin.runCatching { DnssecResolverApi.INSTANCE.resolve(domain, MX::class.java) }
            .onSuccess {
                val realDomains = it.answersOrEmptySet
                if (realDomains.isNotEmpty()) {
                    realDomain = realDomains.first().target.domainpart
                }
            }
            .onFailure { Timber.e(it) }
        return realDomain
    }
}
