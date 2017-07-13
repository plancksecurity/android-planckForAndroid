package com.fsck.k9.pEp.models.mappers;

import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.models.PEpIdentity;

import org.pEp.jniadapter.Identity;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class PEpIdentityMapper {

    private PEpProvider pEpProvider;

    @Inject
    public PEpIdentityMapper() {
    }

    public void initialize(PEpProvider pEpProvider) {
        this.pEpProvider = pEpProvider;
    }

    public List<PEpIdentity> mapRecipients(List<Identity> recipients) {
        List<PEpIdentity> pEpIdentities = new ArrayList<>(recipients.size());
        for (Identity recipient : recipients) {
            pEpIdentities.add(mapRecipient(recipient));
        }
        return pEpIdentities;
    }

    public PEpIdentity mapRecipient(Identity recipient) {
        PEpIdentity pEpIdentity = new PEpIdentity();
        pEpIdentity.address = recipient.address;
        pEpIdentity.comm_type = recipient.comm_type;
        pEpIdentity.flags = recipient.flags;
        pEpIdentity.fpr = recipient.fpr;
        pEpIdentity.lang = recipient.lang;
        pEpIdentity.me = recipient.me;
        pEpIdentity.user_id = recipient.user_id;
        pEpIdentity.username = recipient.username;
        pEpIdentity.setRating(pEpProvider.getRating(recipient));
        return pEpIdentity;
    }
}
