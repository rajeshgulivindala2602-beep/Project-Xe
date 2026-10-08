Notes

Time spent: 4 hrs

Track: Full Stack, so I did both the backend and the frontend. AI tools: Claude and GitHub Copilot in VS Code.

What I did

I added rate alerts. A user can create an alert for a currency pair and a direction (above or below a number), see whether it is waiting or triggered, and delete it. Once an alert has triggered it stays triggered, and the page shows a red banner when any alert has fired. The page refreshes rates and alerts every 15 seconds.

I started with the backend because the UI depends on it. I first refactored the rate fetching so it can ask for one pair at a time, then added the alert API, and then built the frontend.

Backend

Rate fetching. RatesController repeated the same request setup for every pair. I moved the Xe request into XeRateClient, put a RateProvider interface in front of it, and made RateService fetch the supported pairs in order. The /api/rates response keeps the same shape as before: pair, rate and asOf.

Timeouts, errors and caching. A single shared RestTemplate uses Basic authentication with a 3 second connect timeout and a 5 second read timeout. If Xe fails, the API returns a 502. RateService keeps each rate for 30 seconds by default, and setting rates.cache-ttl-seconds to 0 turns the cache off. If two requests arrive at the same moment on an empty cache, both may call Xe. I accepted that to avoid adding locking to a small app. One side effect is that a manual refresh can show a cached rate. I also added a Clock bean so the cache can be tested without sleeping.

Alert model and storage. An alert has an id, a pair, a threshold stored as a BigDecimal, a direction, a creation time and an optional trigger time. The direction is sent as lowercase above or below. Alerts are stored in memory. That is simple, but alerts are lost when the app restarts, and the data would not be shared if several instances were running. The storage sits behind an AlertRepository interface, so a database version can be added later.

Evaluation and API. AlertEvaluator holds the comparison rules. The comparison is strict, so a rate exactly equal to the threshold does not trigger. Once an alert has triggered, it keeps its original trigger time even if the rate moves back. AlertService checks alerts when they are created or listed. There is no background job. It asks for each pair at most once per check. If rates are unavailable, listing still works and the alerts come back with a current rate of null, and their existing triggered state is kept. Bad input is rejected with a hand-written check, so I did not need a validation library. Creating an alert returns 201 with a Location header, deleting returns 204, and deleting an unknown id returns 404. Errors are returned as a JSON object with a single error field.

Frontend

Rate board and state. The three repeated rate functions were replaced by one loop over the rates returned by the API. One Pinia store holds the rates and alerts and provides the load, add and remove actions. The old state.ts file was removed.

Alert screen. AlertForm takes its list of pairs from the loaded rates, checks the threshold before sending it, and clears it after a successful add. AlertList shows the pair, the condition, the status, the trigger time or the current rate, and a delete button. A red banner appears when any alert is triggered. The page polls every 15 seconds and stops the timer when the page is closed.

On the backend I was running, alert responses did not include currentRate. The list therefore uses the matching value from the rate board only when that property is missing. If the property is present and null, it shows as unavailable.

Errors. If a refresh fails, the page keeps the last good data and shows an error message. If the server cannot be reached, the message is: Cannot reach the server. Is the backend running? If a delete returns 404, the alert is removed from the list anyway, because it is already gone on the server.

Tests and checks

The backend tests passed: 37 tests, 0 failures. They cover parsing and failures in the rate client, the rate cache, the repository, the evaluation rules, the service and the controller.

The frontend build passed, and npm test passed: 13 tests in 2 files. They cover threshold validation and the store behavior for load, add and remove, including a failed refresh and a 404 on delete. I did not write tests for the API layer or the components. I kept the test setup small and focused on validation and store logic.

In the browser I checked these cases. An alert for USD/CAD above 1.00 triggers immediately and shows the banner. An alert above 9.99 stays waiting and shows the current rate. An invalid threshold shows a message and nothing is sent. Deleting an alert removes it from the list. I also simulated failed requests in the browser and confirmed the last data stayed on screen with the backend error message. Port 5180 was already in use by another server, so I did not stop it to test a real outage.

Things I left for later

The Xe credentials are in application.properties. They were provided for this exercise, but a real deployment should use environment variables or a secrets manager, and the exposed key should be replaced.

Rates are fetched one pair after another. Xe may allow several target currencies in one request, which would be faster.

Alerts are only checked when someone creates or lists them, so they do not fire while nobody is using the app.

Deleting with an invalid id, such as /api/alerts/not-a-uuid, may return Spring's default 400 response instead of the common error format.

The styling is basic on purpose.

What I would do next
Add a scheduled job that checks alerts, and a way to notify users, so alerts can fire when nobody is looking.
Store alerts in a database, and link them to users if the app becomes multi-user.
Support more currency pairs, and fetch rates in one call or in parallel.
Move the credentials out of the repository.
Add tests for the API layer and the components, and stop polling when the browser tab is hidden.
Add a CI workflow that runs both test suites.
How I used AI

Claude: I used it to help plan the work and to draft implementation ideas and tests.

GitHub Copilot in VS Code: I used it to look through the project, make the changes, and fix problems found while testing.

What I kept: the separation between rate fetching, alert evaluation, storage and the UI; the in-memory repository; and focused tests for the core behavior.

What I changed or rejected: I did not keep an early, compact version of the alert code once the requirements called for separate model, repository, evaluator and API layers. I corrected a test that depended on insertion order when two alerts had the same creation time. I also fixed the UI so it handles alert responses that leave out currentRate.

How I checked it: I ran the full backend tests, built the frontend, ran the frontend tests, and tried creating, listing, invalid input, triggered and waiting alerts, deleting, and failed requests in the browser. I read through the changes before committing them