### History feature
I want to implement a feature where users can see a list of past tts queries.


#### Entry point:
Entry point should be a button in the top left corner of the app, with an icon only (history icon). This entry point should always be available.

#### UI of the history page
History Page is a standard Fragment. It has a toolbar, with a title "History" and a back button.
UI:
----------------
<    History   |
Title 1      > |
Title 2      > |
Title 3      > |
...            |
----------------

UX: Users can see a list of past queries and tap on them to use them in the app text editor (replace current text with the past query)
Queries should be sorted oldest to most recent.

#### Eligibility
Any past query that was successfully processed by backend can be persisted as a past query

#### Persistence
For now, persist past query as a string in Datastore


