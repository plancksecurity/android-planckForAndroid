Feature: Test
  Background:
    Given I created an account

  @QTR-412
  Scenario Outline: Cucumber Search for email/s in the Inbox


    When I select account <account>
    And I select Inbox from Hamburger menu
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
    Examples:
      |account|
      |  0    |
      |  1    |
      |  2    |

  @QTR-154 @QTR-1
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
    And I click message at position 1
    Then I compare messageBody with longText
    And I check if the privacy status is pep_yellow
    And I discard the message
    And I go back to the Inbox
    Examples:
      |account|
      |  0    |
      |  1    |
      |  2    |


  @QTR-1978 @QTR-1
  Scenario Outline: Cucumber Save Draft email with long word

    When I select account <account>
    When I send 1 message to bot2 with subject saveDraft and body saveDraftBody
    When I click compose message
    And I check if the privacy status is pEpRatingUndefined
    And I enter bot2 in the messageTo field
    And I enter TM154A in the messageSubject field
    And I enter longWord in the messageBody field
    And I save as draft
    And I go to the drafts folder
    And I click message at position 1
    Then I compare messageBody with longWord
    And I check if the privacy status is pep_yellow
    And I discard the message
    And I go back to the Inbox
    Examples:
      |account|
      |  0    |
      |  1    |
      |  2    |



  @QTR-
  Scenario Outline: Cucumber Calendar Event

    When I select account <account>


    And I click compose message
    And I enter myself in the messageTo field
    And I enter TM-130 in the messageSubject field
    And I enter ThisIsTheBody in the messageBody field
    And I attach calendarEvent
    When I click the send message button

    Examples:
      | account |
      | 0       |
      | 1       |
      | 2       |

  @QTR-
  Scenario Outline: Cucumber Calendar Event2


    When I select account <account>
    #And I wait for the message and click it
    #And I press back
    And I click the last message received
    And I check that the Calendar is correct and body text is ThisIsTheBody
    Examples:
      | account |
      | 0       |
      | 1       |
      | 2       |




