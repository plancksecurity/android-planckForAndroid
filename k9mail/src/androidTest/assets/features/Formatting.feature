Feature: Formatting

  Keys for these test users will be obtained from the test bot

  Background:
    Given I create an account
    Given I start test


  @login-scenarios
  Scenario: Misc FormattingHTML
  Description: Send message with HTML formatting and special characters to myself
  and see if the formatting changed.
  Assumptions: None
  Expectation: HTML formatting is converted to RTF. However, formatting should be mostly preserved.
  Steps for Testing
  •	Create a new email and add your own email address as recipient and make sure the Privacy Status changes to “Secure & Trusted”
  •	Enter a subject
  •	In the ribbon of the message, go to “Format Text” and select “HTML”
  •	Enter a body, with at least the following formatting:
  some text underlined
  some text italic
  some text red
  highlight some text
  Add some special characters like äüöÄÜéàè
  a bullet list
  •	A
  •	B
  •	Send the message and wait that it arrives in your inbox
  •	VERIFY (1.2.19_01) if all the formatting is kept

  @login-scenarios
  Scenario:  Misc FormattingPlain
  Description: Send message with Plain Text formatting and special characters to myself and see if the formatting changed.
  Assumptions: None
  Expectation: Formatting is kept and special characters are displayed correctly.
  Steps for Testing
  •	Create a new email and add your own email address as recipient and make sure the Privacy Status changes to “Secure & Trusted”
  •	Enter a subject
  •	In the ribbon of the message, go to “Format Text” and select “Plain Text”
  •	Enter a body with some text and special characters (e.g. äüöÄÜéàè)
  •	Send the message and wait that it arrives in your inbox
  •	VERIFY (1.2.20_01) if all the formatting is kept

  @login-scenarios
  Scenario: Misc FormattingRichText
  Description: Send message with Rich Text formatting (RTF) and special characters to myself and see if the formatting changed.
  Assumptions: None
  Expectation: Formatting is kept and special characters are displayed correctly.
  Steps for Testing
  •	Create a new email and add your own email address as recipient and make sure the Privacy Status changes to “Secure & Trusted”
  •	Enter a subject
  •	In the ribbon of the message, go to “Format Text” and select “Rich Text”
  •	Enter a body, with at least the following formatting:
  some text underlined
  some text italic
  some text red
  Add some special characters like äüöÄÜéàè
  a bullet list
  •	A
  •	B
  •	Send the message and wait that it arrives in your inbox
  •	VERIFY (1.2.21_01) if all the formatting is kept