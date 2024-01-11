package com.fsck.k9.planck.infrastructure.modules

import android.content.Context
import com.fsck.k9.mailstore.MessageViewInfoExtractor
import com.fsck.k9.message.extractors.AttachmentInfoExtractor
import com.fsck.k9.message.html.DisplayHtml
import com.fsck.k9.planck.infrastructure.ComposeView
import com.fsck.k9.planck.infrastructure.MessageView
import com.fsck.k9.ui.helper.DisplayHtmlUiFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
class InfoExtractorModule {
    @Provides
    @ComposeView
    fun provideDisplayHtmlForCompose(factory: DisplayHtmlUiFactory): DisplayHtml {
        return factory.createForMessageCompose()
    }

    @Provides
    @MessageView
    fun provideDisplayHtmlForMessageView(factory: DisplayHtmlUiFactory): DisplayHtml {
        return factory.createForMessageView()
    }

    @Provides
    fun provideAttachmentInfoExtractor(): AttachmentInfoExtractor {
        return AttachmentInfoExtractor.getInstance()
    }

    @Provides
    fun provideMessageViewInfoExtractor(
        @ApplicationContext context: Context,
        attachmentInfoExtractor: AttachmentInfoExtractor,
        @MessageView displayHtml: DisplayHtml
    ): MessageViewInfoExtractor {
        return MessageViewInfoExtractor(context, attachmentInfoExtractor, displayHtml)
    }
}