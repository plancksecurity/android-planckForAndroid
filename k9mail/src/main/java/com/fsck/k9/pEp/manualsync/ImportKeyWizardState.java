package com.fsck.k9.pEp.manualsync;

public enum ImportKeyWizardState {
    INIT {
        @Override
        public ImportKeyWizardState next() {
            return BEACON_SENT;
        }
    }, BEACON_SENT {
        @Override
        public ImportKeyWizardState next() {
            return HANDSHAKE_REQUESTED;
        }
    }, BEACON_RECEIVED {
        @Override
        public ImportKeyWizardState next() {
            return HANDSHAKE_REQUESTED;
        }
    }, HANDSHAKE_REQUESTED {
        @Override
        public ImportKeyWizardState next() {
            return PRIVATE_KEY_WAITING;
        }
    }, PRIVATE_KEY_WAITING {
        @Override
        public ImportKeyWizardState next() {
            return INIT;
        }
    };

    public ImportKeyWizardState cancel() {
        return INIT;
    }

    public ImportKeyWizardState start() {
        return BEACON_RECEIVED;
    }

    public abstract ImportKeyWizardState next();


    @Override
    public String toString() {
        return name();
    }
}
