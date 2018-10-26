Feature: Attachment

  Keys for these test users will be obtained from the test bot

  Background:
    Given I create an account
    Given I start test


  @login-scenarios
  Scenario: Test Attachment Send1FileTo1Contact (1/2)
  Description: Send 1 attachment to 1 contact
  Assumption: I already have the public key of the other contact
    and can send the message encrypted(user1)
  Expectation: Then message is in SENT items and I can open the attachments
    When I click message compose
    Then I send 1 message to bot1 with subject subject and body body
    Then I click message compose
    And I fill messageTo field with bot1
    And I fill messageSubject field with subject
    And I fill messageBody field with ThisIsTheBody
    Then I check if the privacy status is pep_yellow
    Then I attach MSoffice
    And I click send message button
    Then I wait for new message

  @login-scenarios
  Scenario: Test Attachment Send1FileTo1Contact (2/2)
  Description: Send 1 attachment to 1 contact
  Assumption: I already have the public key of the other contact
  and can send the message encrypted(user1)
  Expectation: Then message is in SENT items and I can open the attachments
    And I click message
    Then I open 1 attached files

  @login-scenarios
  Scenario: Test Attachment Send4FilesTo3Contacts (1/2)
  Description: Send 4 attachment to 3 contacts
  Assumption: I already have the public key of 3 contacts and can send the message encrypted
  Expectation: Then message is in SENT items and I can open the attachments
    When I click message compose
    Then I fill messageTo field with myself
    And I fill messageSubject field with subject
    And I fill messageBody field with body
    Then I check if the privacy status is pep_green
    And I attach PDF
    And I attach MSoffice
    And I attach settings
    And I attach picture
    And I click send message button
    And I wait for new message


  @login-scenarios
  Scenario: Test Attachment Send4FilesTo3Contacts (2/2)
  Description: Send 4 attachment to 3 contacts
  Assumption: I already have the public key of 3 contacts and can send the message encrypted
  Expectation: Then message is in SENT items and I can open the attachments
    Then I go to sent folder
    And I click message
    And I open 4 attached files

  @login-scenarios
  Scenario: Test Attachment SendEmbeddedPictureToMultipleContacts
  Description: Send message with an embedded picture to multiple contacts
  Assumptions: I already have the public key of 2 contacts and can send the message encrypted
  Expectation: Then message is in SENT items and I can read it including embedded picture
    Given I click message compose
    Then I fill messageTo field with bot1
    And I fill messageTo field with bot2


  @login-scenarios
  Scenario: Test Attachment ReceiveAttachmentsOneFile
  Description: Receive one attachment that has only been sent to me from another pEp user
  Assumptions: The communication partner has my private key
    and can send me encrypted messages with pEp.
  Expectation: I can read the email and attachment

  @login-scenarios
  Scenario: Test Attachment ReceiveAttachmentsThreeFiles
  Description: Receive three attachments that have been sent to me
    and another person from another pEp user
  Assumptions: The communication partner has my private key
    and can send me encrypted messages with pEp
  Expectation: I can read the email and attachment

  @login-scenarios
  Scenario: Test Attachment ReceiveMessageWithEmbeddedPicture
  Description: Receive an encrypted message with an embedded picture
  Assumptions: The communication partner has my private key
    and can send me encrypted messages with pEp
  Expectation: I can read the email and including the embedded picture
