package com.fsck.k9.ui.contacts

import org.koin.dsl.module.applicationContext

val contactsModule = applicationContext {
    bean { ContactLetterExtractor() }
    factory { ContactLetterBitmapConfig(get()) }
    factory { ContactLetterBitmapCreator(get(), get()) }
    factory { ContactPictureLoader(get(), get()) }
}
