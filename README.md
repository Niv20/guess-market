# Guess Market Exercise 3

Chat bonus implemented. The floating chat button lets logged-in users exchange messages through the central server. The newest 500 messages are shared automatically.

Guess Market now runs as a Tomcat application with separate JavaFX clients. One server owns every event, account and trade. This guide explains deployment, the grading workflow, the main new classes and the choices made for Exercise 3.

| Submitter | Details |
| --- | --- |
| Name | Niv Libovitch |
| ID number | 207298514 |
| Email | anivbniv@gmail.com |
| GitHub repository | https://github.com/Niv20/guess-market |

### Run the submitted application

1. Install Java 25 or newer and Apache Tomcat 10.1. Start Tomcat with that Java runtime. The application uses Jakarta Servlet 6; Tomcat 9 is incompatible.
2. Extract the submission ZIP. Copy its single guess-market.war into Tomcat's webapps directory, retaining that filename. Start Tomcat and allow automatic deployment to finish.
3. Open the client directory and run run.bat. JavaFX and the client dependencies are included; Java itself and Tomcat must be installed separately.
4. Leave the server address at its default, enter an unused username and select the login button. No password or separate registration is required.
5. Launch run.bat again for another independent user. Load funds, then upload an Exercise 3 XML file from one client; its events appear in the other client automatically.

### Server address

The default API address is http://localhost:8080/guess-market/api/. The login screen has an editable Server field. A launch argument can also override it:

```text
run.bat --server=http://localhost:8080/guess-market/api/
```

The health endpoint is /api/health under the deployed context.

A web client hosted at `https://niv20.github.io` can use this loopback API as a local companion. The server remains bound to `127.0.0.1`; only that exact web origin receives CORS access. Set `GUESS_MARKET_ALLOWED_ORIGINS` (comma-separated origins, with no paths) before starting the server to use a different trusted website. Browser requests must also use a Host in `GUESS_MARKET_ALLOWED_HOSTS`, whose default is `localhost,127.0.0.1,::1`. A deliberate LAN or remote deployment can set it to the complete exact comma-separated list of permitted plain hostnames or IP literals; schemes, paths, ports and wildcards are rejected. This setting grants no network exposure and does not change the launcher's loopback binding. Do not expose the Tomcat port to the public internet without configuring and securing that deployment separately.

### Run from the source repository

On macOS or Linux, install JDK 25 or newer and run these commands from the repository root. If the client has not been built, run ./build.sh before ./run.sh:

```text
./start-guess-market.sh     # start the server
./run.sh                   # launch a client separately
./start-guess-market.sh --stop
```

The launcher builds and starts only the server. On first use it downloads Tomcat 10.1.59, unless CATALINA_HOME points to an existing Tomcat 10.1 installation. Downloads and local server files stay in .runtime. Its server-only build preserves existing client outputs.

Use --port 8081 to choose another port and update the client Server field to match. Use --no-build to start the existing WAR. Stop the local server before rebuilding or changing its port.

## Using the application

### Events and trading

The Events tab lists every event, including unopened, active and closed markets. Search by event, description or market maker, and filter by trading method, status or commission method. Select a tile to see its description, options, terms, account, participants, transactions, order books and price history.

Only your own identity is available for actions. An event's market maker can fund and open it, then choose the winning option and close it. An active LMSR event accepts share purchases; an order book event accepts buy and sell orders with a quantity and limit price. Matching, partial fills, minting, commissions and settlement use the existing engine rules.

Get quote asks the server for the current LMSR share cost before commission. The quote is indicative: a completed purchase recalculates its price against the latest market. Another user may trade between the quote and the purchase. Closing an event requires confirmation in the client.

### My account and the shared header

My account follows the supplied sketch: the left side shows your balance and funding controls, the account ledger and balance chart. The right side shows events you run or participate in, your holdings and orders, full event details and the same trading controls used on Events.

Private account rows and personal involvement records always belong to the logged-in user. The header keeps your current balance visible and provides Upload events and a profile icon. The profile dropdown shows your username, Settings and Logout. Settings offers Midnight, Daylight and Neon themes and an animations switch; animations start enabled. Funding controls live only in My account. Leaving an account or closing the client asks you to choose Leave or Stay.

### Upload event files

Use Upload events to choose an XML file of at most 2 MB. The server accepts the supplied Exercise 3 format in data/ex3/GM-EX3-Schema.xsd, illustrated by data/ex3/small.xml and data/ex3/multiple.xml. Files contain events without event IDs or a users section. Exercise 1 and 2 system files are not accepted by the online upload endpoint.

Every accepted upload adds to the existing market. The server generates unique event IDs and assigns the uploading user as market maker of every event in that file. It checks the supplied schema, the event-definition rules, duplicate names within the file and names already present on the server. A rejected file reports its reasons and adds nothing. A successful upload does not open the events or debit a subsidy.

### Live updates and chat

The client pulls a complete snapshot about once per second, with no overlapping requests. Views preserve selections and refresh when their data changes. The floating chat button at the bottom right opens a chat drawer over a dark backdrop, with an unread-message badge for new incoming messages. Close it with its close button, Escape or the backdrop. Own messages appear on the right in the current theme color, and other messages appear on the left in gray. Chat shares messages with every client; messages contain server-supplied names and timestamps. It accepts up to 1,000 characters and retains the newest 500 entries. Username and message fields display live character counters, including for pasted and replaced text. Message sends do not produce a success notification. Other action results and failures use dialogs instead of header notices.

The event filter pane starts wide enough to show all three filters on one row, and its divider remains draggable. Layouts wrap controls, provide scrolling and rearrange their split panes vertically in narrower windows. Window resizing leaves both the event and private account details accessible.

## Server and engine architecture

The client communicates only through HTTP. Tomcat creates one application service and one online engine for the deployment. Existing DTO-based display components are repackaged for the client, while the full trading engine stays inside the WAR.

| Module or artifact | Responsibility |
| --- | --- |
| server / guess-market.war | Servlet endpoints, sessions, shared application lifecycle and native multipart handling. The WAR embeds engine.jar, dto.jar, Gson and the JAXB runtime dependencies. |
| engine / engine.jar | Market model, trading, accounts, XML validation and the online engine extension. The supplied Exercise 3 schema is included as a resource. |
| dto / dto.jar | Immutable request, result and network records shared by the server and client. |
| client / client.jar | The independent JavaFX application, HTTP transport and polling lifecycle. |
| ui / ui-common.jar | A selected subset of existing presentation helpers, FXML, styles and display controllers. Legacy ui.jar remains available for Exercise 2 regression work. |

### Main server classes

| Class | Role |
| --- | --- |
| ApplicationListener | Creates fresh ApplicationService state on deployment and removes the context reference when the application stops. |
| ApplicationService | Owns the online engine, cookie and bearer authentication, and shared chat. It holds the engine monitor while applying actions or collecting a complete snapshot. |
| AccessTokenStore | Issues 256-bit browser tokens, refreshes their 30-minute inactivity window and revokes them at logout. |
| BrowserHostAllowlist | Validates and matches the exact local, LAN or remote server names approved for browser access. |
| MarketServlet | Routes /api/* requests, enforces the exact web-origin allowlist, binds authenticated identity, maps failures to JSON and handles uploads entirely in memory. |
| JsonInput | Reads a bounded strict JSON object, requires the correct primitive types and required fields, and rejects identity overrides in action payloads. |
| ApiException | Carries an expected HTTP status and a user-facing error message inside the server. |

### Main online engine additions

| Class or interface | Role |
| --- | --- |
| OnlineMarketEngine | Extends GuessMarketEngine with registration, deposits, stream uploads and account history. |
| OnlineMarketEngineImpl | Starts with an empty market, adds users and files cumulatively, exposes ledger rows, and disables offline replacement and persistence methods. |
| OnlineEventFileLoader | Validates an XML stream against the supplied schema, checks event rules and produces all candidate events before any are added. |
| UserAlreadyExistsException | Reports an attempted registration with an existing username. The factory now exposes createOnlineEngine(). |

## Client classes and shared data

### Main client classes

| Class | Role |
| --- | --- |
| Main | A small launcher that starts JavaFX with the application JAR on the classpath and JavaFX on the module path. |
| GuessMarketClient | Builds the login screen, Events and My account; applies snapshots; preserves selections; connects the profile settings, floating chat, upload and account funding controls. |
| ClientSession | Runs login, actions and polling on one worker. It delivers callbacks on the JavaFX thread and discards callbacks from older sessions after logout or expiry. |
| ApiClient | Uses JDK HttpClient, a cookie store and Gson for HTTP/JSON. It resolves the server URL, posts multipart bytes, enforces timeouts and reads structured errors. |
| ApiClient.ApiException | Preserves an HTTP error status so ClientSession can distinguish an expired session from a recoverable connection failure. |
| TradeActions | One reusable trading panel for both main screens. It binds quotes, purchases, orders, opening and closing to the selected market and the current session. |

### Reused components

EventDetailsController, OptionBookController, EventTile and UserInvolvementController retain the earlier event, book and personal-involvement displays. The common formatting, tables, dialogs, icons, tiles, skins, animation helper and styles are packaged with their FXML resources. These components receive DTOs; the client distribution contains no engine classes or engine.jar.

### New records

| Record | Contents |
| --- | --- |
| SnapshotDto | All event details and price histories; public user summaries; the current user, involvements, balance history and account entries; the shared chat. |
| PublicUserDto | Username, balance and whether that user runs at least one event. |
| AccountEntryDto | Operation sequence, description, signed amount, balance before and balance after. Deposits, trades, commissions and settlement changes remain separate rows. |
| ChatMessageDto | Monotonic message ID, authenticated username, message text and a server timestamp in epoch milliseconds. |
| LoginResponseDto | The canonical username and an opaque browser bearer token; JavaFX continues to use its session cookie. |
| QuoteDto | The current LMSR share cost, before commission. |
| MessageDto | One message field for structured errors and simple acknowledgements. |

Records copy their collections defensively, including lists nested in snapshot price histories. Existing event, order, trade, holding, user and action DTOs continue to carry the domain data; no internal market objects cross the HTTP boundary.

## HTTP protocol and concurrency

All paths below are relative to http://localhost:8080/guess-market/api/. JSON requests use Content-Type: application/json; the upload uses multipart/form-data. Event option indexes start at zero. JavaFX uses its HttpOnly, SameSite=Lax session cookie. Hosted browsers send the login result's token as `Authorization: Bearer <token>` and never place it in a URL.

| Method and path | Request and result |
| --- | --- |
| GET /health | No authentication. Returns the readiness message plus `service: "guess-market"` and `apiVersion: 2`. |
| POST /login | {username} creates a session and returns LoginResponseDto with a bearer token. An existing name returns HTTP 409. |
| POST /logout | Invalidates the cookie session and/or revokes its bearer token; returns an acknowledgement. |
| GET /snapshot | Returns SnapshotDto with all public market information and this session's private account records. |
| POST /funds | {amount} deposits funds in this session's account and returns UserDto. |
| POST /upload | One multipart field named file, containing one XML file. Returns LoadResultDto. |
| GET /quote | Query parameters eventId, optionIndex and shares. Returns QuoteDto. |
| POST /events/open | {eventId}; returns OpenEventResultDto. |
| POST /events/close | {eventId, winningOptionIndex}; returns CloseEventResultDto. |
| POST /events/buy | {eventId, optionIndex, shares}; returns PurchaseResultDto. |
| POST /events/order | {eventId, optionIndex, side, quantity, pricePerShare}; returns SubmitOrderResultDto. side is BUY or SELL. |
| POST /events/create | CreateEventRequestDto fields except userName; the session is the creator. Returns EventDto. Available through HTTP; the client adds events through XML uploads. |
| POST /chat | {message}; returns the accepted ChatMessageDto. |

### Authorization and failures

Every API route except login and health requires a valid cookie session or bearer token. Browser tokens expire after about 30 minutes of inactivity. A client cannot choose another buyer, seller or market maker: action identity is taken only from its credential. The server checks market-maker permissions and all engine rules even if the client has already enabled or disabled the corresponding button. Approved CORS preflights receive 204 before authentication; a present, unapproved Origin is rejected.

Errors use the JSON shape {"message":"..."}. Expected failures include invalid input (400), expired sessions (401), forbidden actions (403), missing events or endpoints (404), wrong methods (405), conflicting state (409), oversized uploads (413) and unsupported content types (415). XML failures include their individual validation reasons.

### A coherent shared market

All engine calls are synchronized. The service also holds that same monitor while collecting an entire snapshot, so another request cannot expose a half-completed trade, commission payment or settlement. Upload validation and insertion are atomic. A slow request delays the next poll instead of creating overlapping pulls.

## Build verification and operating choices

### Build from the repository

The build scripts use javac and jar directly. JavaFX runtimes are included for Windows, Linux and Apple Silicon macOS. The full build compiles the legacy UI for reuse, the online server and the standalone client. Run the command for your operating system from the repository root:

```text
./build.sh                 # macOS or Linux
build.bat                  # Windows
```

Build outputs are out/artifacts/guess-market.war and out/artifacts/client/. The developer run.sh or run.bat starts that client. To assemble the required Windows submission:

```text
./package.sh
```

The result is out/GuessMarket-Ex3.zip. It contains exactly one WAR, readme.docx and a client directory with its application JARs, Gson, Windows JavaFX and run.bat. The Servlet API is provided by Tomcat; the WAR includes the application libraries it needs. No external Gson or JAXB installation is required.

### Validation workflow

Run ./test.sh for the project checks. Set CATALINA_HOME to the installed Tomcat directory to include the isolated HTTP checks; those checks also require Python 3. Engine checks are in tests/engine/OnlineMarketEngineTest.java, HTTP scenarios in tests/http_integration.py and client transport checks in tests/client/ApiClientIntegrationTest.java. For a manual review, use two client windows with different unused names.

- Load funds in both accounts, upload small.xml, open a market as its owner and trade from the second client. Observe balances, ledger rows and event history update in both windows.
- Upload a second valid file to check accumulation. Try a duplicate name and an invalid file; check the error reasons and confirm that the previous market is unchanged.
- Check that only the owner can open or close an event, that a session cannot trade as someone else, and that every commission and payout reaches the appropriate account.
- Send chat messages in both directions and resize the Events and My account screens. Sign out and confirm the ended session can no longer call the API.

### State and session choices

New accounts start at zero. Load funds before paying an opening subsidy or trading. A positive finite deposit is required. The inherited Exercise 2 blocking rule remains permanent: an account that was blocked stays blocked even if a later deposit or payout makes its balance positive.

Usernames are case-insensitive and remain reserved for the server lifetime, including after logout, client closure or credential expiry. This prevents a fresh session from claiming an existing account without a password. Keep the same client window open to retain access. Cookie sessions and browser bearer tokens expire after 30 minutes without requests; active polling keeps them alive.

Events, accounts, history, chat and sessions exist only in memory and disappear when the application or server stops. Tomcat session persistence is explicitly disabled. XML upload bytes remain in memory through native Servlet multipart handling; no uploaded content is saved to a server file.

After a connection interruption, polling retries automatically. Mutating actions are not automatically repeated: inspect the refreshed market and account before retrying a deposit, order or purchase whose response was lost. If login cannot reach the server, verify Tomcat, the deployed WAR filename, the Java version and the Server field.

Source code: [Niv20/guess-market](https://github.com/Niv20/guess-market)

### Desktop interaction regression checks

After `./build.sh`, run `./tests/run_ui_tests.sh` in a desktop session with a display. These isolated JavaFX checks exercise character editing and paste limits, responsive layouts and themes, profile settings, modal chat and unread counts, funds errors and Leave/Stay choices. They do not write to a market server. Rendered UI checks are saved under out/ui-checks.
