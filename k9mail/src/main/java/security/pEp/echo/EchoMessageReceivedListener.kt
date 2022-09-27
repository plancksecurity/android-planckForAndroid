package security.pEp.echo

interface EchoMessageReceivedListener {
    fun echoMessageReceived(from: String, to: String)
}
