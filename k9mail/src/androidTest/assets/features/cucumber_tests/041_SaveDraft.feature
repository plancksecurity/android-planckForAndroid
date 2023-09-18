Feature: Save Draft
  Background:



    #Summary: This Cucumber test describes a scenario where the user creates and saves two drafts of messages, one with an attached MS Office file and the other with an attached picture. The user then navigates to the drafts folder, selects the first draft, checks its content, attachment, and privacy status, and repeats the same for the second draft.
#Description: This Cucumber test scenario involves a series of actions related to composing, saving, and checking drafts in an email application. The test starts with the selection of an account and the clicking of the compose message button. Then, it sends a message to bot, followed by another message to another bot. Both messages have attachments - the first has MSoffice attached, and the second has a picture attached. After attaching the files, the test saves both messages as drafts.
#
#The test then goes to the drafts folder and checks the body of the message against the text that was entered while drafting. Additionally, it verifies that the attachments are present in the draft and that the privacy status is Encrypted.
#
#Next, the test goes back to the drafts folder this time to check the details of the first message that was drafted. The test checks the message body against the text that was entered while drafting, verifies that MSoffice is attached in the draft, and again checks that the privacy status is Encrypted. Finally, the test goes back to the drafts folder.
#
#Overall, the test scenario involves the use of various email features, including composing and sending messages, attaching files, saving drafts, and verifying that the privacy settings are secure.
  Scenario: Cucumber Save Draft

    And I click compose message
    And I send 1 message to bot1 with subject before and body savingTheDraft
    And I click compose message
    And I enter bot1 in the messageTo field
    And I enter saveDraft1 in the messageBody field
    And I attach MSoffice
    And I save as draft
    And I click compose message
    And I send 1 message to bot2 with subject before2 and body savingTheDraft2
    And I click compose message
    And I enter bot2 in the messageTo field
    And I enter saveDraft2 in the messageBody field
    And I attach picture
    And I save as draft
    And I go to drafts folder from navigation menu
    And I click message at position 1
    Then I compare messageBody with saveDraft2
    And I check picture is attached in draft
    And I check the privacy status is Encrypted
    And I go back to the Inbox
    And I go to drafts folder from navigation menu
    And I click message at position 1
    Then I compare messageBody with saveDraft1
    And I check MSoffice is attached in draft
    And I check the privacy status is Encrypted
    And I go back to the Inbox
    And I go to drafts folder from navigation menu


