package com.fsck.k9.ui.messageview

import android.content.Context
import android.print.PrintDocumentAdapter
import android.text.format.DateUtils
import android.webkit.WebSettings
import com.fsck.k9.R
import com.fsck.k9.helper.Contacts
import com.fsck.k9.helper.MessageHelper
import com.fsck.k9.mail.Message
import com.fsck.k9.mailstore.AttachmentResolver
import com.fsck.k9.view.K9WebViewClient
import com.fsck.k9.view.MessageWebView

class PrintableMessage(private val context: Context,
                       private val contacts: Contacts?,
                       private val jobName: String,
                       private val printableWebViewCallback: PrintableWebViewCallback,
                       private val attachmentResolver: AttachmentResolver) {
    var webView: MessageWebView = MessageWebView(context)

    init {
        setupWebView()
    }

    private fun setupWebView() {
        val webViewClient = K9WebViewClient.newInstance(attachmentResolver)
        webViewClient.setOnPageFinishedListener { view ->
            printableWebViewCallback.adapterReady(view.createPrintDocumentAdapter(jobName))
        }
        webView.webViewClient = webViewClient

        webView.settings.apply {
            loadsImagesAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            allowFileAccess = true
        }
    }

    fun generatePrintableWebView(html: String, message: Message) {
        val htmlWithHeader = html.replaceFirst("</head><body>", buildHeader(message))
        val htmlWithHeaderAndCss = htmlWithHeader.replaceFirst("<style type=\"text/css\">", buildCss())
        webView.loadDataWithBaseURL("about:blank", htmlWithHeaderAndCss, "text/html", "utf-8", null)
    }

    private fun buildHeader(message: Message): String {
        val fromName: CharSequence = MessageHelper.toFriendly(message.from, contacts)
        val fromAddress: CharSequence = if (message.from.isNotEmpty()) message.from[0].address else ""
        val to = message.getRecipients(Message.RecipientType.TO).joinToString(",") { recipient -> recipient.address }
        val cc = message.getRecipients(Message.RecipientType.CC).joinToString(",") { recipient -> recipient.address }

        val dateTime = DateUtils.formatDateTime(context, message.sentDate.time,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL
                        or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_YEAR)
        return "</head><body>\n" +
                "<img src=\"https://pep.foundation/static/media/uploads/peplogo.svg\" width=\"50\" style=\"margin: 10px;\">\n" +
                "<hr class=\"lineBreak\">\n" +
                "<div class=\"subject\">${message.subject}</div>\n" +
                "<hr class=\"lineBreak\">\n" +
                "<article>\n" +
                fromHeader(fromAddress, fromName) +
                toHeader(to) +
                ccHeader(cc) +
                "</article>\n" +
                "<article class=\"right\">\n" +
                "   <div class=\"date\">$dateTime</div>\n" +
                "</article>\n" +
                " <hr style=\"color:transparent;\">\n"
    }

    private fun ccHeader(cc: String) = if (cc.isNotEmpty()) {
        "   <div>\n" +
                "       <span>${context.getString(R.string.recipient_cc)}:</span> \n" +
                "       <span>$cc</span> \n" +
                "   </div>\n"
    } else ""

    private fun toHeader(to: String) = if (to.isNotEmpty()) {
        "   <div>\n" +
                "       <span>${context.getString(R.string.recipient_to)}:</span> \n" +
                "       <span>$to</span> \n" +
                "   </div>\n"
    } else ""

    private fun fromHeader(fromAddress: CharSequence, fromName: CharSequence) = if (fromAddress.isEmpty()) {
        "   <div>\n" +
                "       <span class=\"fromLabel\">${context.getString(R.string.recipient_from)}:</span>\n" +
                "       <span class=\"bold\">$fromName</span>\n" +
                "       <span class=\"fromAddress\">&lt;$fromAddress&gt;</span>\n" +
                "   </div>\n"
    } else ""

    private fun buildCss(): String {
        return "<style type=\"text/css\">" +
                ".subject {\n" +
                "   font-size:      20px;\n" +
                "   font-weight:    bold;\n" +
                "}\n" +
                ".bold {\n" +
                "   font-weight:    bold;\n" +
                "}\n" +
                ".right{\n" +
                "   float:right\n" +
                "}\n" +
                "article{\n" +
                "   display:inline-block\n" +
                "}\n" +
                ".lineBreak{\n" +
                "   height:2px;\n" +
                "   border-width:0;\n" +
                "   color:gray;\n" +
                "   background-color:silver\n" +
                "}"
    }
}


interface PrintableWebViewCallback {

    fun adapterReady(printAdapter: PrintDocumentAdapter)
}