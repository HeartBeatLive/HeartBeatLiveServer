# HeartBeatLive Server
Server of HeartBeatLive APP.

This server use **Gralde** as build tool and **Java 17** as runtime engine.
Also, this server use MongoDB as main database (you may use `docker-compose.yml` for launching it locally). Use `gradlew.sh` or `gradlew.bat` files for launching Gradle commands.

## API
All server communication made using GraphQL query language.

| Info     | Description    |
|----------|----------------|
| Endpoint | `/graphql`     |
| Method   | `POST`         |
| Body     | GraphQL schema |
| Headers  | `Content-Type: application/graphql+json`<br/>`Authorization: Bearer <jwt_token>` (Optional, see [Authentication](#authentication))<br/>`X-Application-Type: <application_type>` (Optional)<br/>`X-Application-Version: <application_version>` (Optional)<br/>`X-Platform: <platform_info>` (Optional) |

## Authentication
All authentication is implemented using **Firebase Authentication** service.
User should be able to authenticate using email+password, Google OAuth and Apple OAuth.

Client should use Firebase Authentication for user registration and authentication.
Then, client should use Firebase ID token for authenticating on server.
All user profile updating operations must be implemented using server GraphQL API (see [API](#api)), you may want to refresh user token after that.

For correct working, you need to deploy to the Firebase Functions code from [HeartBeatLiveFirebaseFunctions repository](https://github.com/HeartBeatLive/HeartBeatLiveFirebaseFunctions).
This code send to this server information about user creation and deletion.