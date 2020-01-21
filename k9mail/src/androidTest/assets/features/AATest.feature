Feature: Test
  Background:
    Given I created an account

	#Description: Test if pEp changes to Privacy Status from “Unknown” to “Unsecure”
	# when entering the email address of a new contact bot1.
	# Also verify if pEp attaches my public key to outgoing messages
	# Assumption: No emails with the communication partner have been exchanged so far
	# Expectation: Privacy Status of outgoing message is “Unsecure”
  @TM-6 @TM-1 @TM-559
  Scenario Outline: Cucumber Mail to new contact

    When I select account <account>
    When I click compose message
    And I check if the privacy status is pEpRatingUndefined
    And I enter bot1 in the messageTo field
    And I enter mailToNewContact in the messageSubject field
    And I enter bodyMailToNewContact in the messageBody field
    Then I check in the handshake dialog if the privacy status is pEpRatingUnencrypted
    When I enter empty in the messageTo field
    And I enter empty in the messageSubject field
    And I enter empty in the messageBody field
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    When I enter bot1 in the messageTo field
    And I enter empty in the messageSubject field
    And I enter empty in the messageBody field
    Then I check in the handshake dialog if the privacy status is pEpRatingUnencrypted
    When I enter bodyMailToNewContact2 in the messageBody field
    And I click the send message button
    And I wait for the new message
    And I go to the sent folder
    And I click the first message
    Then I compare messageBody with bodyMailToNewContact2
    Examples:
      |account|
      |  0    |

  @TM-152 @TM-1
  Scenario Outline: Cucumber Send Unencrypted email with long text

    When I select account <account>
    When I click compose message
    And I check if the privacy status is pEpRatingUndefined
    And I enter bot2 in the messageTo field
    And I enter TM152 in the messageSubject field
    And I enter longText in the messageBody field
    And I click the send message button
    And I wait for the message and click it
    Then I compare messageBody from json file with longText
    And I go to the sent folder
    And I click the first message
    Then I compare messageBody with longText
    Examples:
      |account|
      |  0    |

	#The long text should contain at least 15k characters.
  @TM-153 @TM-1
  Scenario Outline: Cucumber Send Encrypted email with long text

    When I select account <account>
    And I click compose message
    And I enter bot1 in the messageTo field
    Then I check if the privacy status is pep_yellow
    When I enter TM153 in the messageSubject field
    And I enter longText in the messageBody field
    And I click the send message button
    And I wait for the message and click it
    Then I compare messageBody from json file with longText
    Then I check if the privacy status is pep_yellow
    When I go back to the Inbox
    And I go to the sent folder
    And I click the first message
    Then I compare messageBody with longText
    Examples:
      |account|
      |  0    |

	#[^long.txt]
	#is the link to long text
  @TM-154 @TM-1
  Scenario Outline: Cucumber Save Draft email with long text

    When I select account <account>
    When I send 1 message to bot2 with subject saveDraft and body saveDraftBody
    When I click compose message
    And I check if the privacy status is pEpRatingUndefined
    And I enter bot2 in the messageTo field
    And I enter TM154A in the messageSubject field
    And I enter longText in the messageBody field
    And I save as draft
    And I go to the drafts folder
    And I click the first message
    And I enter TM154B in the messageSubject field
    And I enter longText in the messageBody field
    And I save as draft
    And I wait for the new message
    And I click message at position 2
    Then I compare messageBody with longText
    Examples:
      |account|
      |  0    |

	#Scenario: Test Sanity MailFromNewContactEncrypted
	#Description: You have a new communication partner using pEp.
	#This communication partner sends you a message from pEp.
	#Verify if the public key of the communication partner is imported and you can answer encrypted.
	#Assumption: You didn’t receive any message from the communication partner so far
	#Expectation: The public key of the communication partner is imported
	#The answer to my communication partners message is encrypted
  @TM-8 @TM-1 @TM-169
  Scenario Outline: Cucumber Mail from new contact encrypted

    When I select account <account>
    And I click compose message
    And I send 1 message to bot1 with subject mailFromNewContactEncryptedBody and body MailFromNewContactEncryptedBody
    And I click the last message received
    Then I compare messageBody from json file with MailFromNewContactEncryptedBody
    And I check if the privacy status is pep_yellow
    When I click reply message
    And I enter extraText in the messageSubject field
    And I enter bodyText in the messageBody field
    Then I check if the privacy status is pep_yellow
    And I click the send message button
    And I go back to the Inbox
    And I wait for the new message
    Examples:
      |account|
      |  0    |

	#Ensure mails are encrypted when pEp says so (SER-299)
  @TM-9 @TM-1
  Scenario Outline: Cucumber: Ensure Mails are encrypted when pEp says so

    When I select account <account>
    And I send 1 message to bot1 with subject mailsEncryptedWhenpEpSaysSo and body MailsAreEncryptedWhen_pEp_saysSo
    And I click the last message received
    And I click reply message
    Then I check if the privacy status is pep_yellow
    When I click the send message button
    And I press back
    And I wait for the message and click it
    And I compare rating_string from json file with PEP_rating_reliable
    Examples:
      |account|
      |  0    |

	#Description: Test if pEp encrypts the message if you send an email to someone
	#you already have the public key from, e.g. user1.
	#Assumption: You already have a public key from your communication partner
	#Expectation: The new message should be sent encrypted
  @TM-10 @TM-1
  Scenario Outline: Cucumber Mail to existing contact is encrypted

    When I select account <account>
    And I click compose message
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    When I send 1 message to bot1 with subject TM-10 and body TM-10
    And I click compose message
    And I enter bot1 in the messageTo field
    Then I check if the privacy status is pep_yellow
    When I enter empty in the messageTo field
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    When I send 1 message to bot1 with subject TM-10A and body TM-10Abody
    And I go to the sent folder
    And I click the last message received
    Then I check if the privacy status is pep_yellow
    And I compare messageBody with TM-10Abody
    Examples:
      |account|
      |  0    |

	#Description: Test if the Privacy Status of a message is correct,
	#if it is sent to multiple people and the public key is available (user1 and user2)
	#Assumption: You have public keys of (at least) 2 different communication partners.
	#Expectation: The new message should be sent encrypted
  @TM-11 @TM-1
  Scenario Outline: Cucumber Mail to multiple contacts encrypted

    When I select account <account>
    And I click compose message
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    When I send 1 message to bot1 with subject TM-11 and body TM-11
    And I click the last message received
    Then I check if the privacy status is pep_yellow
    When I press back
    And I click compose message
    And I send 1 message to bot2 with subject TM-11A and body TM-11A
    And I click the last message received
    Then I check if the privacy status is pep_yellow
    When I press back
    And I click compose message
    And I enter bot1 in the messageTo field
    And I enter bot2 in the messageTo field
    Then I check if the privacy status is pep_yellow
    When I enter TM-11B in the messageSubject field
    And I enter TM-11B in the messageBody field
    And I enter empty in the messageTo field
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    When I enter bot2 in the messageTo field
    And I enter empty in the messageSubject field
    And I enter empty in the messageBody field
    Then I check if the privacy status is pep_yellow
    And I enter TM-11C in the messageSubject field
    And I enter TM-11CBody in the messageBody field
    When I click the send message button
    And I wait for the new message
    And I go to the sent folder
    And I click the last message received
    Then I check if the privacy status is pep_yellow
    And I compare messageBody with TM-11CBody
    And I go back to the Inbox
    Examples:
      |account|
      |  0    |

	#Description:Test if the Privacy Status of a message is correct,
	#if it is sent to multiple people. For one of the recipients, there is no public key available.
	#Assumption: You have public keys of (at least) 2 different communication partners (user1 and user2).
	#You don’t have the key of user3.
	#Expectation: The message should be sent unencrypted
  @TM-12 @TM-1
  Scenario Outline: Cucumber Mail to multiple contacts (mixed)

    When I select account <account>
    And I click compose message
    And I send 1 message to bot1 with subject TM-12 and body TM-12
    And I click compose message
    And I send 1 message to bot2 with subject TM-12A and body TM-12A
    And I click compose message
    And I enter bot1 in the messageTo field
    And I enter bot2 in the messageTo field
    Then I check if the privacy status is pep_yellow
    When I enter bot5 in the messageTo field
    Then I check if the privacy status is pEpRatingUnencrypted
    When I enter TM-12B in the messageSubject field
    And I enter TM-12B in the messageBody field
    And I enter empty in the messageTo field
    And I enter bot1 in the messageTo field
    And I enter bot2 in the messageTo field
    Then I check if the privacy status is pep_yellow
    When I enter bot5 in the messageTo field
    Then I check if the privacy status is pep_no_color
    When I click the send message button
    And I wait for the new message
    And I go to the sent folder
    And I click the last message received
    Then I check if the privacy status is pep_no_color
    And I compare messageBody with TM-12B
    Examples:
      |account|
      |  0    |

	#Description: Send a message with disabled protection to a contact you have the public key (user1)
	#Assumption: You have public keys of (at least) 1 communication partner
	#Expectation: The message is sent unencrypted with no public key attached
	#
  @TM-13 @TM-1
  Scenario Outline: Cucumber Disable Protection

    When I select account <account>
    When I click compose message
    And I enter bot1 in the messageTo field
    And I select from message menu pep_force_unprotected
    And I enter TM-13 in the messageSubject field
    And I enter TM-13 in the messageBody field
    Then I check if the privacy status is pep_no_color
    When I click the send message button
    And I wait for the new message
    And I go to the sent folder
    And I click the last message received
    Then I check if the privacy status is pep_no_color
    Examples:
      |account|
      |  0    |

    	#Description: Make a handshake with a communication partner
	#Assumption: You didn’t make a handshake with communication partner user1 yet
	#Expectation: Privacy Status is “Secure & Trusted” for new messages with the trusted partner
  @TM-16 @TM-2
  Scenario Outline: Cucumber Handshake in new Message

    When I select account <account>
    When I click compose message
    And I send 1 message to bot3 with subject TM-16 and body TM-16body
    And I click the last message received
    And I go back to the Inbox
    When I click compose message
    And I enter bot3 in the messageTo field
    Then I check if the privacy status is pep_yellow
    And I confirm trust words match
    When I click confirm trust words
    Then I check if the privacy status is pEpRatingTrusted
    Examples:
      |account|
      |  0    |

	#Description: Make a handshake with a communication partner
	#Assumption: You didn’t exchange any message with user4 yet
	#Expectation: Privacy Status for the single existing message changes from “Secure…” to “Secure & Trusted”