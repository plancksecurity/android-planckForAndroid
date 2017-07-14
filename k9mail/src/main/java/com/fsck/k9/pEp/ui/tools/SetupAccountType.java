package com.fsck.k9.pEp.ui.tools;

import com.fsck.k9.Account;
import com.fsck.k9.helper.EmailHelper;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.setup.ServerNameSuggester;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;

import static com.fsck.k9.mail.ServerSettings.Type.SMTP;

public class SetupAccountType {

    @Inject
    public SetupAccountType() {
    }

    public void setupStoreAndSmtpTransport(Account account, ServerSettings.Type serverType, String schemePrefix) throws URISyntaxException {
        ServerNameSuggester serverNameSuggester = new ServerNameSuggester();

        String domainPart = EmailHelper.getDomainFromEmailAddress(account.getEmail());

        String suggestedStoreServerName = serverNameSuggester.suggestServerName(serverType, domainPart);
        URI storeUriForDecode = new URI(account.getStoreUri());
        URI storeUri = new URI(schemePrefix, storeUriForDecode.getUserInfo(), suggestedStoreServerName,
                storeUriForDecode.getPort(), null, null, null);
        account.setStoreUri(storeUri.toString());

        String suggestedTransportServerName = serverNameSuggester.suggestServerName(SMTP, domainPart);
        URI transportUriForDecode = new URI(account.getTransportUri());
        URI transportUri = new URI("smtp+tls+", transportUriForDecode.getUserInfo(), suggestedTransportServerName,
                transportUriForDecode.getPort(), null, null, null);
        account.setTransportUri(transportUri.toString());
    }
}
