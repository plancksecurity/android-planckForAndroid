package com.fsck.k9.pEp.ui.handshake;

import com.fsck.k9.pEp.models.PEpIdentity;
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;

import java.util.List;

public class ExpandablePEpIdentity extends ExpandableGroup<PEpIdentity> {

    public ExpandablePEpIdentity(String title, List<PEpIdentity> items) {
        super(title, items);
    }
}
