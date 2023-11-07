Feature: Mail to multiple contacts
  Background:


    #Summary: messages sent to multiple contacts with diverse privacy.
    # Description: send messages to multiple contacts with different privacy and trust settings. It covers message composition, recipient privacy statuses, trust confirmation, and message delivery verification.

  Scenario: Cucumber Mail to multiple contacts

    And I click compose message
    Then I check the privacy status is Undefined
    When I send 1 message to bot1 with subject multiple and body multipleContacts1
    And I click the last message received
    Then I check the privacy status is Encrypted
    And I confirm trust words match
    When I click confirm trust words
    And I press back
    And I click compose message
    And I send 1 message to bot2 with subject multiple2 and body multipleContacts
    And I click the last message received
    Then I check the privacy status is Encrypted
    When I press back
    And I click compose message
    And I enter bot1 in the messageTo field
    Then I check the privacy status is Trusted
    And I enter bot2 in the messageTo field
    Then I check the privacy status is Encrypted
    When I enter TM-11B in the messageSubject field
    And I enter TM-11B in the messageBody field
    And I enter empty in the messageTo field
    Then I check the privacy status is Undefined
    When I enter bot1 in the messageTo field
    And I enter empty in the messageSubject field
    And I enter empty in the messageBody field
    Then I check the privacy status is Trusted
    And I enter bot2 in the messageTo field
    Then I check the privacy status is Encrypted
    And I enter bot3 in the messageTo field
    Then I check the privacy status is NotEncrypted
    When I enter multiple3 in the messageSubject field
    And I enter multipleContacts in the messageBody field
    When I click the send message button
    And I wait for the new message
    And I go to sent folder from navigation menu
    And I click the first message
    Then I check the privacy status is NotEncrypted
    And I compare messageBody with multipleContacts
    And I go back to the Inbox

