# HeartBeatLive Server
Server of HeartBeatLive APP.

This server use **Gralde** as build tool and **Java 17** as runtime engine.
Also, this server use MongoDB as main database and Redis as cache and Pub/Sub service (you may use `docker-compose.yml` for launching them locally).
Use `gradlew.sh` or `gradlew.bat` files for launching Gradle commands.

## Tests
Use `./gradlew test` command to launch test.
You need to have Docker Engine running while you running test.
We use Docker to launch applications, that tests depend on.

## API
API documentation collected in `docs/` folder.

### Authentication
All authentication is implemented using **Firebase Authentication** service.
User should be able to authenticate using email+password, Google OAuth and Apple OAuth.

Client should use Firebase Authentication for user registration and authentication.
Then, client should use Firebase ID token for authenticating on server.
All user profile updating operations must be implemented using server GraphQL API, you may want to refresh user token after that.

For correct working, you need to deploy to the Firebase Functions code from [HeartBeatLiveFirebaseFunctions repository](https://github.com/HeartBeatLive/HeartBeatLiveFirebaseFunctions).
This code send to this server information about user creation and deletion.

### Push Notifications
Push notifications is sending using [OneSignal](https://onesignal.com) service.
Clients must request notification permissions on application launched, set external user id when user is logged in and delete external user id when user is logged out.

Most of all notifications contains `heart_beat_live:notification_id` metadata.
You may use this metadata to map received push notification with notifications from server.

To set user's external id, client should provide OneSignal identifier authentication token.
Along with OneSignal app id, you can receive an identifier authentication token using GraphQL API.