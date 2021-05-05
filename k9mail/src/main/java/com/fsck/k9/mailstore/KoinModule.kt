package com.fsck.k9.mailstore

import org.koin.dsl.module

val mailStoreModule = module {
    single { FolderRepositoryManager() }
}
