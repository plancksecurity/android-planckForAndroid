package security.pEp.print

import android.content.Context
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.text.format.DateUtils
import android.webkit.WebSettings
import com.fsck.k9.K9
import com.fsck.k9.R
import com.fsck.k9.helper.Contacts
import com.fsck.k9.helper.MessageHelper
import com.fsck.k9.helper.SizeFormatter
import com.fsck.k9.mail.Message
import com.fsck.k9.mailstore.AttachmentResolver
import com.fsck.k9.mailstore.AttachmentViewInfo
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.pEp.ui.tools.ThemeManager
import com.fsck.k9.view.K9WebViewClient
import com.fsck.k9.view.MessageWebView
import security.pEp.permissions.PermissionChecker

class PrintMessage(private val context: Context,
                   permissionChecker: PermissionChecker,
                   private val attachmentResolver: AttachmentResolver,
                   private val attachments: MutableMap<Uri, AttachmentViewInfo>?,
                   private val message: LocalMessage,
                   private val html: String) : Print {
    private var webView: MessageWebView = MessageWebView(context)
    private var jobName: String = "${context.getString(R.string.app_name)} print_message"
    private val contacts = if (permissionChecker.hasContactsPermission() &&
            K9.showContactName()) Contacts.getInstance(context) else null

    init {
        setupWebView()
    }

    private fun setupWebView() {
        val webViewClient = K9WebViewClient.newInstance(attachmentResolver)
        webViewClient.setOnPageFinishedListener { view ->
            printWebView(view.createPrintDocumentAdapter(jobName))
        }
        webView.webViewClient = webViewClient

        webView.settings.apply {
            loadsImagesAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            allowFileAccess = true
        }
    }

    private fun printWebView(printAdapter: PrintDocumentAdapter) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager?
        printManager?.print(jobName, printAdapter, PrintAttributes.Builder().build())
    }

    override fun print() {
        val htmlWithHeader = html.replaceFirst("</head><body>", buildHeader(message))
        var htmlWithHeaderAndCss = htmlWithHeader.replaceFirst("<style type=\"text/css\">", buildCss())
        if (ThemeManager.isDarkTheme())
            htmlWithHeaderAndCss = htmlWithHeaderAndCss.replaceFirst("* {", "")
        val htmlWithHeaderCssAndAttachments = htmlWithHeaderAndCss.replaceFirst("</body>", buildAttachments())
        webView.loadDataWithBaseURL("about:blank", htmlWithHeaderCssAndAttachments, "text/html", "utf-8", null)
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
                "}\n" +
                ".lineBreakSmall{\n" +
                "   height:2px;\n" +
                "   border-width:0;\n" +
                "   color:gray;\n" +
                "   background-color:silver;\n" +
                "   max-width: 400px;\n" +
                "   margin-left:0;\n" +
                "}\n" +
                ".attachments{\n" +
                "   margin-left:20;\n" +
                "}\n" +
                ".attachment {\n" +
                "   float: left;\n" +
                "   clear: none; \n" +
                "}\n" +
                ".noPadding{\n" +
                "   margin: 0;\n" +
                "   padding: 0; \n" +
                "}\n" +
                ".noPaddingBold{\n" +
                "   font-weight: bold;\n" +
                "   margin: 0;\n" +
                "   padding: 0; \n" +
                "}\n" +
                "ul.no_bullet {\n" +
                "   list-style-type: none;\n" +
                "   padding: 0;\n" +
                "   margin: 0;\n" +
                "}\n" +
                "li.icon {\n" +
                "   background: url('https://aux.iconspalace.com/uploads/document-icon-256-545819468.png') no-repeat left;\n" +
                "   background-size: 15px;\n" +
                "   padding-left: 20px;\n" +
                "   padding-top: 7px;\n" +
                "}"
    }

    private fun buildAttachments(): String {
        val attachmentsList = notInlineNorpEpAttachments()
        return if (attachmentsList?.isNotEmpty() == true) {
            "<div class=\"attachments\">\n" +
                    "<hr class=\"lineBreakSmall\">\n" +
                    attachmentsTitle(attachmentsList.size) +
                    "<ul class=\"no_bullet\">\n" +
                    buildAttachmentsList(attachmentsList) +
                    "</ul>\n" +
                    "</div>\n" +
                    "</body>"
        } else "</body>"
    }

    private fun buildAttachmentsList(attachmentsList: List<AttachmentViewInfo>): String {
        return attachmentsList.joinToString("") { entry -> buildAttachment(entry) }
    }

    private fun notInlineNorpEpAttachments() = attachments
            ?.map { entry -> entry.value }
            ?.filter { entry -> !entry.inlineAttachment && !entry.ispEpAttachment() }


    private fun buildAttachment(attachmentInfo: AttachmentViewInfo): String =
            "   <li class=\"icon\">\n" +
                    "   <div>\n" +
                    "       <p class=\"noPaddingBold\">${attachmentInfo.displayName}</p>\n" +
                    "       <p class=\"noPadding\">${setAttachmentSize(attachmentInfo.size)}</p>\n" +
                    "   </div>\n" +
                    "</li>\n"

    private fun attachmentsTitle(attachmentsCount: Int): String =
            "<span class=\"bold\">${context.resources.getQuantityString(R.plurals.attachment_count, attachmentsCount, attachmentsCount)}</span>\n"

    private fun setAttachmentSize(size: Long): String {
        return if (size == AttachmentViewInfo.UNKNOWN_SIZE) "" else SizeFormatter.formatSize(context, size)
    }
}

