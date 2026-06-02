# Med Lion HR — Supabase + Cloudflare Backend

## What Changed

The local Bun/Express server and Windows desktop app have been removed. The backend runs entirely in the cloud via Supabase + Cloudflare Worker. Only the **Android app** and **web dashboard** remain.

## Features — Complete

- [x] All data stored in Supabase Postgres
- [x] Cloudflare Worker API at `https://li980wrgnunptwig2nzqh-backend.rork.app`
- [x] Admin login requires **userId + password** (not just password)
- [x] Android app connects to cloud Worker
- [x] Web dashboard with premium blue/white/green/red UI
- [ ] Deploy website to Cloudflare Pages with your own account

## How It Works

1. **Supabase Postgres**: employees, attendance_logs, leave_requests, admin_settings, work_site
2. **Cloudflare Worker**: handles auth (userId+password), geofence checks, salary, CSV export
3. **Android app**: connects directly to Worker URL
4. **Web dashboard**: connects directly to Worker URL

## Admin Credentials

- **User ID**: `admin`
- **Password**: `Yashwant@2000`

## API Endpoints

- `GET /api/health`
- `POST /api/admin/login` — `{ userId, password }`
- `GET /api/employees` / `POST /api/employees`
- `GET /api/attendance` / `POST /api/attendance/checkin` / `POST /api/attendance/checkout`
- `GET /api/leaves` / `POST /api/leaves`
- `GET /api/salaries/:month/:year`
- `GET /api/worksite` / `PUT /api/worksite`
- `GET /api/export/payroll/:month/:year`
- `POST /api/setup` — auto-creates tables and seeds credentials

## What's Removed

- Windows Electron desktop app
- Local Express server (`server/`)
- Server IP configuration from Android app
