# Migrate Med Lion HR from local Express server to Supabase + Cloudflare

## What Will Change

The local Bun/Express server that runs on your PC will be replaced with a cloud backend powered by Supabase (database) and a Cloudflare Worker (API). No more local server — everything runs in the cloud.

## Features

- All employee, attendance, leave, payroll, and worksite data stored in a live Supabase Postgres database
- Same API endpoints as before, but served from a Cloudflare Worker at a permanent cloud URL
- Both the Windows app and Android app connect directly to the cloud — no local server needed
- Admin password and employee credentials stored securely in the database
- Data persists even if your PC is off

## How It Works

1. Supabase Postgres stores all your HR data (employees, attendance logs, leave requests, payroll)
2. A Cloudflare Worker acts as the API — it handles login, geofence checks, salary calculations, CSV exports, and talks to Supabase
3. The Windows desktop app and Android app both connect to the Worker's cloud URL instead of `localhost:8080`

## What Stays the Same

- All existing screens and features work exactly as before
- The premium blue/white/green/red design is unchanged
- Admin login flow is identical
- The Electron Windows EXE build still works

## What Gets Removed

- The local `server/` folder (Express + JSON file storage) — no longer needed
- The localStorage fallback in the web app — replaced by real cloud database
- The Android app's server IP configuration — replaced by cloud URL

