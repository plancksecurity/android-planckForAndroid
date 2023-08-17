Feature: Ensure Mails are encrypted when Planck says so
  Background:


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

