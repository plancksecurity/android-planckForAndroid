package com.fsck.k9.message.html;


import org.junit.Test;

import static com.fsck.k9.message.html.UriParserTestHelper.assertLinkOnly;
import static org.junit.Assert.assertEquals;


public class BitcoinUriParserTest {
    private final BitcoinUriParser parser = new BitcoinUriParser();
    private final StringBuffer outputBuffer = new StringBuffer();


    @Test
    public void basicBitcoinUri() {
        assertLinkify("bitcoin:19W6QZkx8SYPG7BBCS7odmWGRxqRph5jFU");
    }

    @Test
    public void bitcoinUriWithAmount() {
        assertLinkify("bitcoin:12A1MyfXbW6RhdRAZEqofac5jCQQjwEPBu?amount=1.2");
    }

    @Test
    public void bitcoinUriWithQueryParameters() {
        assertLinkify("bitcoin:12A1MyfXbW6RhdRAZEqofac5jCQQjwEPBu?amount=1.2" +
                "&message=Payment&label=Satoshi&extra=other-param");
    }

    @Test
    public void uriInMiddleOfInput() {
        String prefix = "prefix ";
        String uri = "bitcoin:12A1MyfXbW6RhdRAZEqofac5jCQQjwEPBu?amount=1.2";
        String text = prefix + uri;

        parser.linkifyUri(text, prefix.length(), outputBuffer);

        assertLinkOnly(uri, outputBuffer);
    }

    @Test
    public void invalidScheme() {
        assertNotLinkify("bitcion:19W6QZkx8SYPG7BBCS7odmWGRxqRph5jFU");
    }

    @Test
    public void invalidAddress() {
        assertNotLinkify("bitcoin:[invalid]");
    }

    @Test
    public void invalidBitcoinUri_shouldReturnStartingPosition() {
        String uri = "bitcoin:[invalid]";

        int newPos = linkify(uri);

        assertEquals(0, newPos);
    }

    @Test
    public void invalidBitcoinUri_shouldNotWriteToOutputBuffer() {
        String uri = "bitcoin:[invalid]";

        linkify(uri);

        assertEquals(0, outputBuffer.length());
    }


    private int linkify(String uri) {
        return parser.linkifyUri(uri, 0, outputBuffer);
    }

    private void assertLinkify(String uri) {
        linkify(uri);
        assertLinkOnly(uri, outputBuffer);
    }

    private void assertNotLinkify(String text) {
        int newPos = linkify(text);
        assertEquals(0, newPos);
    }
}
