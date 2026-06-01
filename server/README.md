# Med Lion HR — Local Server

A lightweight server that your Android app and the web dashboard connect to over your local WiFi network. All data is saved to `data.json`, so nothing is lost when you restart.

## Run it on your PC

1. Install [Bun](https://bun.sh) (one-time): `curl -fsSL https://bun.sh/install | bash`
2. In this `server` folder, install dependencies (one-time): `bun install`
3. Start the server: `bun start`

You'll see something like:

```
🦁 Med Lion HR Server running on http://0.0.0.0:8080
   Local:   http://localhost:8080
   Network: http://<your-ip>:8080
```

## Find your PC's local IP

- **Windows:** open Command Prompt → `ipconfig` → look for "IPv4 Address" (e.g. `192.168.1.20`)
- **macOS:** System Settings → Network, or run `ipconfig getifaddr en0`
- **Linux:** `hostname -I`

## Connect the Android app

1. Make sure the phone and PC are on the **same WiFi**.
2. Open the app → **Admin → Settings → Local Server**.
3. Enter `your-ip:8080` (e.g. `192.168.1.20:8080`) and tap **Connect**.
4. The dot turns green when connected. If the PC is off, the app keeps working offline and re-syncs automatically when it's back.

## Open the web dashboard

On any device on the same network, open a browser to:

```
http://<your-ip>:8080
```

Log in with the admin password (default `Yashwant@2000`). The dashboard serves from the same server, so it's always in sync with the phone.

> The built web dashboard files go in a `web-dist` folder next to this server. Build the web app and copy its `dist` output here as `web-dist` to serve it from the server, or host the web app separately and point it at this server.

## Change the port

Set the `PORT` environment variable, e.g. `PORT=9000 bun start`.
