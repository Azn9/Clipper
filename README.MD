# Clipper

This small java application runs a twitch bot that listens to `!clip` commands in chat. When the command is sent, the bot creates a clip and sends it to a discord channel.

### Setup:
1. Create a twitch token with the `clips:edit` and `chat:read` scopes
2. Create and fill a `config.json` file:
```Json
{
  "accessToken": "",
  "refreshToken": "",
  "clientId": "",
  "clientSecret": "",
  "streamerId": "",
  "streamerName": "",
  "webhookUrl": ""
}
```
3. Runs the application
