package security.planck.echo

interface EchoMessageReceivedListener {
    fun echoMessageReceived(from: String, to: String)
}
