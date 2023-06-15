Feature: Test
  Background:
    Given I created an account


  Scenario: Cucumber Reset Partner Key
    And I send 1 messages to bot2 with subject ResetKey and body ResetPartnersKey
    And I click the last message received
    Then I reset partner key
    When I press back
    When I click compose message
    And I enter bot2 in the messageTo field
    And I enter ResetKey2 in the messageSubject field
    And I enter NewMessageAfterReset in the messageBody field
    Then I check the privacy status is NotEncrypted
    When I click the send message button
    And I wait for the message and click it
    Then I check the privacy status is Encrypted



  Scenario: Cucumber Copy email to a folder
    When I send 1 message to bot1 with subject copyThisMessage and body ThisMessageWillCopiedToAnotherFolder
    And I click the last message received
    And I copy the message to the folder spam
    Then I compare messageBody from json file with ThisMessageWillCopiedToAnotherFolder
    When I go back to the Inbox
    And I go to spam folder from navigation menu
    And I click the first message
    Then I compare messageBody from json file with ThisMessageWillCopiedToAnotherFolder
    And I press back
    And I go to inbox folder from navigation menu


  Scenario: Cucumber Copy email with attachment to a folder
    When I click compose message
    And I enter myself in the messageTo field
    And I enter copyThisMessageWithAttachments in the messageSubject field
    And I enter ThisMessageWithAttachmentWillBeCopiedToAnotherFolder in the messageBody field
    And I attach PDF
    And I attach MSoffice
    And I attach picture
    When I click the send message button
    And I wait for the message and click it
    And I copy the message to the folder spam
    Then I check the privacy status is Trusted
    And I compare messageBody from json file with ThisMessageWithAttachmentWillBeCopiedToAnotherFolder
    And I open 3 attached files
    When I go back to the Inbox
    And I go to spam folder from navigation menu
    And I click the first message
    And I compare messageBody from json file with ThisMessageWithAttachmentWillBeCopiedToAnotherFolder
    Then I check the privacy status is Trusted
    And I open 3 attached files
    And I press back
    And I go to inbox folder from navigation menu


  Scenario: Cucumber Move email to a folder
    When I send 1 message to bot1 with subject moveThisMessage and body ThisMessageWillMovedToAnotherFolder
    And I click the last message received
    And I move the message to the folder spam
    And I go back to the Inbox
    And I go to spam folder from navigation menu
    And I click the first message
    Then I compare messageBody from json file with ThisMessageWillMovedToAnotherFolder
    And I press back
    And I go to inbox folder from navigation menu


  Scenario: Cucumber Move email with attachments to a folder
    When I click compose message
    And I enter myself in the messageTo field
    And I enter moveThisMessageWithAttachments in the messageSubject field
    And I enter ThisMessageWithAttachmentWillBeMovedToAnotherFolder in the messageBody field
    And I attach PDF
    And I attach MSoffice
    And I attach picture
    When I click the send message button
    And I wait for the message and click it
    And I move the message to the folder spam
    And I go back to the Inbox
    And I go to spam folder from navigation menu
    And I click the first message
    Then I compare messageBody from json file with ThisMessageWithAttachmentWillBeMovedToAnotherFolder
    And I check the privacy status is Trusted
    And I open 3 attached files
    And I press back
    And I go to inbox folder from navigation menu

#Summary: This Cucumber test involves testing the privacy status and message sending functionality of a mail application. The test includes sending messages to existing and new contacts, checking the privacy status of messages, and verifying the message body of sent messages.
#Description: This Cucumber test involves composing and sending messages through an email client. The test includes multiple steps, such as checking the privacy status of a message, entering recipients, subjects and message bodies, sending messages, waiting for new messages to arrive, and comparing the message bodies with expected values. The test also covers scenarios where the recipient or subject fields are empty, and it verifies the privacy status of the messages in different scenarios.
#
#In summary, this test checks the basic functionality of composing and sending messages, as well as the privacy and security of the messages, ensuring they are sent to the correct recipient with the correct subject and message body.
  Scenario: Cucumber Mail to new contact

    When I click compose message
    And I check the privacy status is Undefined
    And I enter bot1 in the messageTo field
    And I enter newContact in the messageSubject field
    And I enter bodyMailToNewContact in the messageBody field
    Then I check the privacy status is NotEncrypted
    When I enter empty in the messageTo field
    And I enter empty in the messageSubject field
    And I enter empty in the messageBody field
    Then I check the privacy status is Undefined
    When I enter bot1 in the messageTo field
    And I enter empty in the messageSubject field
    And I enter empty in the messageBody field
    Then I check the privacy status is NotEncrypted
    When I enter bodyMailToNewContact2 in the messageBody field
    And I click the send message button
    And I wait for the new message
    And I go to the sent folder
    And I click the first message
    Then I compare messageBody with bodyMailToNewContact2


#Summary: This Cucumber test involves sending an email with a long text message to a bot and then verifying that the message was sent and received correctly.
#Description: This Cucumber test involves sending an email with a long body of text to a bot, checking the privacy status of the email, and then comparing the message body with the original text. The test also checks whether the message is saved and sent correctly by going to the sent folder and comparing the message body with the original text. The test is conducted on a specified account and uses the subject and message body fields to send the email. Overall, the test ensures that emails can be sent and saved correctly with the desired privacy settings.
  Scenario: Cucumber Send Unencrypted email with long text

    When I click compose message
    And I check the privacy status is Undefined
    And I enter bot2 in the messageTo field
    And I enter TM152 in the messageSubject field
    And I enter longText in the messageBody field
    And I click the send message button
    And I wait for the message and click it
    Then I compare messageBody from json file with longText
    And I go to the sent folder
    And I click the first message
    Then I compare messageBody with longText


#Summary: This Cucumber test involves sending an encrypted message to bot1 and verifying that the message is received with the correct privacy status and content. It also checks that the sent message is stored correctly in the sent folder.
#Description: This Cucumber test is designed to test the messaging functionality of an email service. The test involves sending encrypted messages to a bot and verifying that the privacy status of the messages is Encrypted. There are also tests to verify that messages can be saved as drafts, and that the privacy status of drafts and sent messages is Encrypted. The test involves using various features of the email service, such as composing a message, sending a message, saving a draft, and navigating through different folders in the email service. The test also involves comparing message content with expected values stored in a JSON file.
  Scenario: Cucumber Send Encrypted email with long text

    When I click compose message
    And I send 1 message to bot1 with subject sendEncrypted and body sendEncryptedTest
    And I click compose message
    And I enter bot1 in the messageTo field
    Then I check the privacy status is Encrypted
    When I enter TM153 in the messageSubject field
    And I enter longText in the messageBody field
    And I click the send message button
    And I wait for the message and click it
    Then I compare messageBody from json file with longText
    Then I check the privacy status is Encrypted
    When I go back to the Inbox
    And I go to the sent folder
    And I click the first message
    Then I compare messageBody with longText


#Summary: This Cucumber test case describes a scenario in which a user saves a draft message, goes to the drafts folder to verify it, and then discards it.
#Description: This Cucumber test involves a series of actions related to composing, sending, and saving draft messages in an email application. The test is focused on verifying the functionality related to the privacy status and content of the messages.
#
#The first step is selecting an email account, followed by sending a message to a specific bot with a given subject and body. The next step is to compose another message to the same bot, with a different subject and body, and check that the privacy status is Encrypted. After sending the message, the test verifies that the sent message has the correct content and privacy status.
#
#In a subsequent test, the user saves a draft message with a specific subject and body, verifies that the privacy status is Undefined, and then goes to the drafts folder to open the saved message. The test checks that the message content matches what was saved and that the privacy status has been updated to secure. Finally, the user discards the message and returns to the Inbox.
  Scenario: Cucumber Save Draft email with long text

    When I send 1 message to bot2 with subject saveDraft and body saveDraftBody
    When I click compose message
    And I check the privacy status is Undefined
    And I enter bot2 in the messageTo field
    And I enter TM154A in the messageSubject field
    And I enter longText in the messageBody field
    And I save as draft
    And I go to the drafts folder
    And I click message at position 1
    Then I compare messageBody with longText
    And I check the privacy status is Encrypted
    And I discard the message
    And I go back to the Inbox



#Summary: This Cucumber test is testing the functionality of sending and receiving encrypted messages with a bot. The test includes composing and sending encrypted messages, verifying the privacy status of messages, and comparing message content.
#Description: This Cucumber test is a series of steps that test the functionality of sending and receiving encrypted messages. The test involves selecting an account, composing and sending a message to a bot, checking the privacy status, and comparing the message body with a specific string. The test also includes actions such as saving a draft message, discarding a message, and verifying the privacy status of sent messages. Overall, the test aims to ensure that the messaging system is functioning correctly and that messages are being sent and received securely.
  Scenario: Cucumber Send Encrypted email with long word

    When I click compose message
    And I send 1 message to bot1 with subject sendEncrypted and body sendEncryptedTest
    And I click compose message
    And I enter bot1 in the messageTo field
    Then I check the privacy status is Encrypted
    When I enter EFA-1976 in the messageSubject field
    And I enter longWord in the messageBody field
    And I click the send message button
    And I wait for the message and click it
    Then I compare messageBody with longWord
    Then I check the privacy status is Encrypted
    When I go back to the Inbox
    And I go to the sent folder
    And I click the first message
    Then I compare messageBody with longWord
 

#Summary: This Cucumber test describes a scenario where a user selects an account and performs various actions related to composing and managing messages. The test covers sending messages to bots, checking the privacy status of messages, replying to messages, saving messages as drafts, and discarding drafts.
#Description: This Cucumber test is focused on testing various aspects of sending and receiving emails using an email client. The test covers scenarios such as selecting accounts, sending messages to bots, checking privacy status, replying to messages, saving drafts, and discarding drafts. The test also involves comparing message bodies with expected values, checking the privacy status of messages, and navigating between different folders such as Inbox, Sent, and Drafts.
#
#The overall objective of the test is to ensure that the email client functions as expected, providing a secure and reliable way for users to send and receive messages.
  Scenario: Cucumber Save Draft email with long word

    When I send 1 message to bot2 with subject saveDraft and body saveDraftBody
    When I click compose message
    And I check the privacy status is Undefined
    And I enter bot2 in the messageTo field
    And I enter TM154A in the messageSubject field
    And I enter longWord in the messageBody field
    And I save as draft
    And I go to the drafts folder
    And I click message at position 1
    Then I compare messageBody with longWord
    And I check the privacy status is Encrypted
    And I discard the message
    And I go back to the Inbox

 

#Summary: This Cucumber test checks the encryption and privacy status of a message sent to a bot, verifies the message contents and then replies to it with additional text.
#Description: This Cucumber test is a set of steps that a series of actions to test the behavior of an email client. The test focuses on the encryption and privacy features of the email client, and involves sending and receiving emails to different contacts, and checking their privacy status. The test also involves comparing the message body with pre-defined values from a JSON file.
#
#The test begins by selecting an account and clicking on the compose message button. The privacy status is then checked to ensure that it is undefined. The user then sends a message to bot with a specific subject and body, and the privacy status is checked again to ensure that it is Encrypted. The user then sends another message to bot1 with a different subject and body, and the privacy status is checked again to ensure that it is still secure.
#
#The test then involves clicking on the last message received, and checking the privacy status again, followed by replying to the message and checking the privacy status once more. The user then enters some additional text in the message subject and body fields, and clicks the send message button. Finally, the user goes back to the inbox and waits for a new message.
  Scenario: Cucumber Mail from new contact encrypted

    And I click compose message
    And I send 1 message to bot1 with subject mailFromNewContactEncryptedBody and body MailFromNewContactEncryptedBody
    And I click the last message received
    Then I compare messageBody from json file with MailFromNewContactEncryptedBody
    And I check the privacy status is Encrypted
    When I click reply message
    Then I check the privacy status is Encrypted
    And I click the send message button
    And I go back to the Inbox
    And I wait for the new message
 

#Summary: This Cucumber test involves testing whether an email message is correctly encrypted using the Planck encryption protocol. The test involves sending a message to a bot with a specific subject and body, verifying that the message is encrypted using Planck, replying to the message and checking if the privacy status is still secure, and finally comparing the planck rating with a value from a JSON file.
#Description: This Cucumber test is testing the email encryption functionality of an email client. The test includes several steps such as selecting the account, sending an encrypted message to a bot with a specific subject and body, clicking on the last received message, clicking on reply message, checking the privacy status, and comparing a rating string from a JSON file. The test appears to be checking whether the email encryption and decryption process is functioning correctly and whether the privacy status is Encrypted.
  Scenario: Cucumber: Ensure Mails are encrypted when Planck says so

    And I send 1 message to bot1 with subject mailsEncryptedWhenPlanckSaysSo and body MailsAreEncryptedWhen_Planck_saysSo
    And I click the last message received
    And I click reply message
    Then I check the privacy status is Encrypted
    When I click the send message button
    And I press back
    And I wait for the message and click it
    And I compare rating_string from json file with PEP_rating_reliable


#Summary: This Cucumber test describes a series of actions to test the privacy status and message body in a messaging application. The actions include selecting an account, composing and sending messages to various bots, checking the privacy status of each message, and comparing the message body to expected values.
#Description:This Cucumber test involves testing the privacy status of messages sent in a messaging application. The test involves selecting an account and composing messages to various bots and other recipients, checking the privacy status of each message, and comparing the message body to expected values.
#
#In more detail, the test begins by selecting an account and clicking the "compose message" button. The privacy status is checked and found to be undefined. A message is then sent to a bot, and the privacy status is checked again and found to be secure. Another message is sent to the same bot and the privacy status is checked and found to be undefined.
#
#Another message is sent to the same bot, and the privacy status is checked and found to be secure. The test then goes to the sent folder, clicks the last message received, and compares the message body to an expected value.
#
#Overall, this test verifies that messages are being sent securely to the intended recipients and that the privacy status is correctly updated based on the message's recipients and content.
  Scenario: Cucumber Mail to existing contact is encrypted

    And I click compose message
    Then I check the privacy status is Undefined
    When I send 1 message to bot1 with subject TM-10 and body TM-10
    And I click compose message
    And I enter bot1 in the messageTo field
    Then I check the privacy status is Encrypted
    When I enter empty in the messageTo field
    Then I check the privacy status is Undefined
    When I send 1 message to bot1 with subject TM-10A and body TM-10Abody
    And I go to the sent folder
    And I click the last message received
    Then I check the privacy status is Encrypted
    And I compare messageBody with TM-10Abody


#Summary: This Cucumber test is testing the privacy status of messages sent between different accounts and to different recipients, and whether the privacy status changes based on the recipients and the contents of the message.
#Description: This Cucumber test involves testing various aspects of composing and sending messages in an email application. The test checks the privacy status of messages being composed and sent to different recipients, as well as the privacy status of received messages. Additionally, the test checks the correctness of the message body and subject, and verifies that messages can be sent and received successfully. The test also involves navigating between different folders, such as the inbox and sent folder. Overall, the test aims to ensure that the email application functions correctly and maintains the privacy of user data.
  Scenario: Cucumber Mail to multiple contacts encrypted

    And I click compose message
    Then I check the privacy status is Undefined
    When I send 1 message to bot1 with subject TM-11 and body TM-11
    And I click the last message received
    Then I check the privacy status is Encrypted
    When I press back
    And I click compose message
    And I send 1 message to bot2 with subject TM-11A and body TM-11A
    And I click the last message received
    Then I check the privacy status is Encrypted
    When I press back
    And I click compose message
    And I enter bot1 in the messageTo field
    And I enter bot2 in the messageTo field
    Then I check the privacy status is Encrypted
    When I enter TM-11B in the messageSubject field
    And I enter TM-11B in the messageBody field
    And I enter empty in the messageTo field
    Then I check the privacy status is Undefined
    When I enter bot2 in the messageTo field
    And I enter empty in the messageSubject field
    And I enter empty in the messageBody field
    Then I check the privacy status is Encrypted
    And I enter TM-11C in the messageSubject field
    And I enter TM-11CBody in the messageBody field
    When I click the send message button
    And I wait for the new message
    And I go to the sent folder
    And I click the last message received
    Then I check the privacy status is Encrypted
    And I compare messageBody with TM-11CBody
    And I go back to the Inbox


#Summary: This Cucumber test checks the privacy status of messages being sent to different recipients in different scenarios.
#Description: This Cucumber test involves sending messages to different bots and checking the privacy status of the message. The test starts with selecting an account and composing and sending messages to bot1 and bot2. Then, the privacy status is checked and the message is sent to bot5, and the privacy status is checked again.
#
#Next, a message is composed with an empty "to" field and sent to bot1 and bot2, with privacy status checked before and after adding bot5 to the recipient list. Finally, the last message received is opened from the sent folder, and its privacy status and body content are checked.
  Scenario: Cucumber Mail to multiple contacts (mixed)

    And I click compose message
    And I send 1 message to bot1 with subject TM-12 and body TM-12
    And I click compose message
    And I send 1 message to bot2 with subject TM-12A and body TM-12A
    And I click compose message
    And I enter bot1 in the messageTo field
    And I enter bot2 in the messageTo field
    Then I check the privacy status is Encrypted
    When I enter bot5 in the messageTo field
    Then I check the privacy status is NotEncrypted
    When I enter TM-12B in the messageSubject field
    And I enter TM-12B in the messageBody field
    And I enter empty in the messageTo field
    And I enter bot1 in the messageTo field
    And I enter bot2 in the messageTo field
    Then I check the privacy status is Encrypted
    When I enter bot5 in the messageTo field
    Then I check the privacy status is NotEncrypted
     When I click the send message button
    And I wait for the new message
    And I go to the sent folder
    And I click the last message received
    Then I check the privacy status is NotEncrypted
    And I compare messageBody with TM-12B


#Summary: This Cucumber test involves checking the privacy status of an email, composing and sending an email with special characters to bot, comparing the message body with the special characters, going to the sent folder, and comparing the message body again. The test also involves composing and sending an email with special characters to oneself and comparing the message body.
#Description: This Cucumber test checks the functionality of composing and sending messages with special characters in the message body. It also tests the ability to view sent messages and compare the message body with the original message.
#
#The test begins with selecting an account and clicking on the compose message button. It then checks the initial privacy status of the message. Next, the test enters the recipient and message details, attaches the message body with special characters, and sends the message.
#
#The test then waits for the message to arrive, clicks on it, and compares the message body with the original message. It also checks the ability to view sent messages and compares the message body with the original message.
#
#Finally, the test goes to the inbox, composes a new message with the same special characters, sends the message, waits for it to arrive, clicks on it, and compares the message body with the original message.
  Scenario: Cucumber Special Characters

    And I click compose message
    Then I check the privacy status is Undefined
    When I enter bot2 in the messageTo field
    And I enter Special1 in the messageSubject field
    And I enter specialCharacters in the messageBody field
    And I click the send message button
    And I wait for the message and click it
    Then I compare messageBody with specialCharacters
    When I compare messageBody with specialCharacters
    And I press back
    And I go to the sent folder
    And I click the first message
    Then I compare messageBody with specialCharacters
    When I press back
    And I select the inbox from the menu
    And I click compose message
    And I enter myself in the messageTo field
    And I enter Special2 in the messageSubject field
    And I enter specialCharacters in the messageBody field
    And I click the send message button
    And I wait for the message and click it
    Then I compare messageBody with specialCharacters



#Summary: This Cucumber test involves removing all messages, sending a message with attachments containing special characters to bot5, and waiting for a new message.
#Description: This Cucumber test is focused on sending a message with special characters and checking that it is successfully received. The test begins by selecting an account and removing all existing messages. Then, a new message is composed and sent to a bot with a specified subject and body, which contains special characters. The message is sent with an attachment containing special characters as well. Finally, the test checks that the message was successfully received by waiting for a new message to appear.
  Scenario: Cucumber Attachment Send 1 File To 1 Contact (1/2)

    And I remove all messages
    And I click compose message
    And I send 1 message to bot5 with subject TM-126 and body attach1
    And I click compose message
    And I enter bot5 in the messageTo field
    And I enter TM-126A in the messageSubject field
    And I enter attachSpecialCharacters in the messageBody field
    When I attach specialCharacters
    And I click the send message button
    And I wait for the new message



#Summary: This Cucumber test is the second part of the test and involves selecting an account and opening the last received message and opening one of the attached files in it.
#Description: This Cucumber test scenario involves accessing a previously received message and opening an attached file. The test assumes that an email has been received and the user is currently viewing the inbox or message list. The test involves selecting the last message received and opening an attached file. The attached file may be of any type, such as a PDF or an image file. The purpose of this test is to ensure that the user can access and view attachments that are sent with emails.
  Scenario: Cucumber Attachment Send 1 File To 1 Contact (2/2)

    And I click the last message received
    Then I open 1 attached files


#Summary: This Cucumber test involves sending multiple messages to different bots with various subjects and bodies, and attaching several files to the last message sent.
#Description: This Cucumber test is a functional test that tests the ability of a messaging system to send multiple messages to different recipients with different subject and message body. It also checks if different types of files can be attached to the message, and the system's ability to send the message successfully. The test involves sending three messages to different recipients with different subjects and message bodies, and then sending another message to the same recipients with a different subject and message body. The test attaches different types of files to the message and then checks if the message has been sent successfully.
  Scenario: Cucumber Attachment Send 4 Files To 3 Contacts (1/2)

    When I click compose message
    And I send 1 message to bot6 with subject TM-128 and body attach4
    When I click compose message
    And I send 1 message to bot2 with subject TM-128A and body attach4A
    When I click compose message
    And I send 1 message to bot5 with subject TM-128B and body attach4B
    When I click compose message
    And I enter bot6 in the messageTo field
    And I enter bot2 in the messageTo field
    And I enter bot5 in the messageTo field
    And I enter TM-128C in the messageSubject field
    And I enter attach4C in the messageBody field
    When I attach PDF
    And I attach MSoffice
    And I attach settings
    And I attach picture
    And I click the send message button
    And I wait for the new message


#Summary: This Cucumber test is the second part of the test and involves selecting an account, navigating to the "sent" folder, clicking on the last message received, and then opening four attached files.
#Description: This Cucumber test is focused on verifying the ability to access and open attached files in a received email. The test starts by selecting the specified account and navigating to the sent folder. The last received message is then clicked, and the test verifies that 4 attached files can be opened. The purpose of this test is to ensure that the email service is functioning correctly and that users can access and view attachments sent to them.
  Scenario: Cucumber Attachment Send 4 Files To 3 Contacts (2/2)

    And I go to the sent folder
    And I click the last message received
    Then I open 4 attached files


#Summary: This Cucumber test involves sending an email with an attachment, verifying the privacy status, and checking the contents of the email and attachment.
#Description: This Cucumber test involves sending an email with an attachment and verifying that the privacy status of the message is Encrypted. The test begins with selecting an account and composing a message. The user sends a message to a bot, attaches a PDF file, and verifies the privacy status of the message. The user then sends a second message, attaches another file, and verifies the privacy status of the message. Finally, the user compares the message body of the second message with a pre-defined JSON file and opens the attached file.
  Scenario: Cucumber Attachment Receive Attachments One File

    When I click compose message
    And I send 1 message to bot5 with subject EFA-130 and body beforeAttachment
    And I click compose message
    And I enter bot5 in the messageTo field
    And I enter TM-130 in the messageSubject field
    And I enter attach1File in the messageBody field
    And I attach PDF
    Then I check the privacy status is Encrypted
    When I click the send message button
    And I wait for the message and click it
    Then I check the privacy status is Encrypted
    And I compare messageBody from json file with attach1File
    And I open 1 attached files


#Summary: This Cucumber test involves sending a message to a bot with multiple attachments, verifying the privacy status, waiting for the message to arrive, and opening the attachments.
#Description: This Cucumber test involves sending a message with multiple attachments and checking the privacy status of the message before and after sending it. The test begins with selecting an account and clicking the compose message button. Then, a message is sent to a bot with a subject and body, and another message is composed with attachments such as PDF, MSoffice, and a picture. The privacy status is checked before sending the message, and again after waiting for the message and clicking on it. Finally, the test opens the three attached files.
  Scenario: Cucumber Attachment Receive Attachments Three Files

    When I click compose message
    And I send 1 message to bot5 with subject TM-131 and body attach3
    And I click compose message
    And I enter bot5 in the messageTo field
    And I enter TM-131B in the messageSubject field
    And I enter attach3B in the messageBody field
    And I attach PDF
    And I attach MSoffice
    And I attach picture
    Then I check the privacy status is Encrypted
    When I click the send message button
    And I wait for the message and click it
    Then I check the privacy status is Encrypted
    And I open 3 attached files


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
    And I go to the drafts folder
    And I click message at position 1
    Then I compare messageBody with saveDraft2
    And I check picture is attached in draft
    And I check the privacy status is Encrypted
    When I go to the drafts folder
    And I click message at position 1
    Then I compare messageBody with saveDraft1
    And I check MSoffice is attached in draft
    And I check the privacy status is Encrypted
    And I go to the drafts folder


    #Summary: This Cucumber test describes a set of actions that a user takes on a messaging platform to send and search for messages with specific text. The test involves sending messages to different bots with specific subject and body text, composing and sending a message to oneself, and verifying that specific messages are found when searched for.
#Description: The test involves selecting an account, navigating to the inbox, sending messages to different recipients with specific subject and body text, composing messages to oneself with specific message subject and body text, and performing searches for specific messages with certain text. The test is considered successful if all searches return the expected number of messages.
  Scenario: Cucumber Search for email/s in the Inbox

    And I go to inbox folder from navigation menu
    And I send 2 messages to bot2 with subject 3messages and body textA
    And I send 1 messages to bot5 with subject 3messages and body textC
    And I click compose message
    And I enter myself in the messageTo field
    And I enter 1messages in the messageSubject field
    And I enter textB in the messageBody field
    And I click the send message button
    And I wait for the new message
    And I send 1 messages to bot2 with subject test and body textA
    And I send 1 messages to bot5 with subject subject and body textD
    And I click compose message
    And I enter myself in the messageTo field
    And I enter Subject in the messageSubject field
    And I enter textB in the messageBody field
    And I click the send message button
    And I wait for the new message
    Then I search for 3 messages with text 3messages
    Then I search for 1 message with text 1messages
    Then I search for 0 messages with text 0messages
    Then I search for 2 messages with text textB



    #Summary: This Cucumber test scenario involves a user selecting an account and verifying that the calendar date and body text of the last received message are correct.
#Description: The test starts by selecting an account and clicking the compose message button. Then, the user enters their own email address in the messageTo field and a calendar event is attached to the message. After that, the user clicks the send message button and waits for the new message to appear. The test is successful if the message is sent successfully and the new message is visible.
  Scenario: Cucumber Calendar Event

    And I click compose message
    And I enter myself in the messageTo field
    And I enter TM-130 in the messageSubject field
    And I enter ThisIsTheBody in the messageBody field
    And I attach calendarEvent
    When I click the send message button
    And I wait for the new message



    #Summary: This Cucumber test scenario involves a user selecting an account and verifying the correctness of the calendar and body text of the last received message.
#Description: The test verifies that the body text of the message is correct. This test ensures that the messaging system is functioning correctly and that messages are displayed as expected.
  Scenario: Cucumber Calendar Event2

    And I click the last message received
    And I check that the Calendar is correct and body text is ThisIsTheBody



    #Summary: This Cucumber test scenario involves a user composing a message and adding unreliable recipients. The test verifies that insecurity warnings appear when adding these recipients, and the message is discarded as a result.
#Description: This Cucumber test is designed to ensure that a messaging application warns users when sending messages to unreliable recipients. In this test, the user selects a specific account and clicks on the "compose message" button to create a new message. Then they enter three unreliable recipients in the messageTo field. The test checks if an unsecure warning is displayed, indicating that the recipients may not be trustworthy. This test is crucial to ensure the security of the messaging application and protect users from potential security threats.
  Scenario: Cucumber Unsecure Warning Test

    And I click compose message
    And I enter 3 unreliable recipients in the messageTo field
    Then I check unsecure warnings are there
    And I discard the message



    #Summary: This Cucumber test scenario involves a user composing a message, adding and removing unreliable recipients, and verifying that there are no insecurity warnings displayed in the application.
#Description: The test scenario starts with the user selecting an account represented by the <account> placeholder. Then, the user clicks the compose message button and enters their own address in the messageTo field.
#
#In the following steps, the user removes their own address by clicking the X button, and adds three unreliable recipients to the messageTo field. The user then removes the unreliable recipients by clicking the X button in reverse order.
#
#After removing all the recipients, the user checks for insecurity warnings. The test scenario is successful if the application does not display any warnings.
#
#The primary objective of this Cucumber test is to ensure that the application does not display insecurity warnings when a user removes all recipients from a message after adding unreliable recipients.
  Scenario: Cucumber Remove address clicking X button


    And I click compose message
    And I enter myself in the messageTo field
    Then I remove the 1 address clicking X button
    And I enter 3 unreliable recipients in the messageTo field
    Then I remove the 3 address clicking X button
    Then I remove the 2 address clicking X button
    Then I remove the 1 address clicking X button
    Then I check insecurity warnings are not there


#Summary: This Cucumber test describes a scenario where tests the widgets in the application.
#Description: This Cucumber test scenario is designed to test the functionality of messaging and widgets in an application.
#The test is useful in verifying that the application's messaging and widget functionality work as expected. The scenario is flexible as it allows for different accounts to be used by merely changing the placeholder.
  Scenario: Cucumber Widget

    And I send 1 messages to bot1 with subject WidgetTest and body TestingWid_gets
    Then I test widgets


