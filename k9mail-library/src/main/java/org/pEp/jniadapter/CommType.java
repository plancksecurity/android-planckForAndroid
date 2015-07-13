package org.pEp.jniadapter;

public enum CommType {
    PEP_ct_unknown (0),

    // range 0x01 to 0x09: no encryption, 0x0a to 0x0e: nothing reasonable

    PEP_ct_no_encryption (0x01),                // generic
    PEP_ct_no_encrypted_channel (0x02),
    PEP_ct_key_not_found (0x03),
    PEP_ct_key_expired (0x04),
    PEP_ct_key_revoked (0x05),
    PEP_ct_key_b0rken (0x06),
    PEP_ct_my_key_not_included (0x09),

    PEP_ct_security_by_obscurity (0x0a),
    PEP_ct_b0rken_crypto (0x0b),
    PEP_ct_key_too_short (0x0e),

    PEP_ct_compromized (0x0f),                  // known compromized connection

    // range 0x10 to 0x3f: unconfirmed encryption

    PEP_ct_unconfirmed_encryption (0x10),       // generic
    PEP_ct_OpenPGP_weak_unconfirmed (0x11),	    // RSA 1024 is weak

    PEP_ct_to_be_checked (0x20),                // generic
    PEP_ct_SMIME_unconfirmed (0x21),
    PEP_ct_CMS_unconfirmed (0x22),

    PEP_ct_strong_but_unconfirmed (0x30),       // generic
    PEP_ct_OpenPGP_unconfirmed (0x38),          // key at least 2048 bit RSA or EC
    PEP_ct_OTR_unconfirmed (0x3a),

    // range 0x40 to 0x7f: unconfirmed encryption and anonymization

    PEP_ct_unconfirmed_enc_anon (0x40),         // generic
    PEP_ct_PEP_unconfirmed (0x7f),

    PEP_ct_confirmed (0x80),                    // this bit decides if trust is confirmed

    // range 0x81 to 0x8f: reserved
    // range 0x90 to 0xbf: confirmed encryption

    PEP_ct_confirmed_encryption (0x90),         // generic
	PEP_ct_OpenPGP_weak (0x91),                 // RSA 1024 is weak

    PEP_ct_to_be_checked_confirmed (0xa0),      //generic
    PEP_ct_SMIME (0xa1),
    PEP_ct_CMS (0xa2),

    PEP_ct_strong_encryption (0xb0),            // generic
	PEP_ct_OpenPGP (0xb8),                      // key at least 2048 bit RSA or EC
	PEP_ct_OTR (0xba),

    // range 0xc0 to 0xff: confirmed encryption and anonymization

    PEP_ct_confirmed_enc_anon (0xc0),           // generic
	PEP_ct_pEp (0xff);

    public final int value;

    CommType(int value) {
        this.value = value;
    }
}

