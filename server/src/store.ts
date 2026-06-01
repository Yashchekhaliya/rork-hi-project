import * as fs from "node:fs";
import * as path from "node:path";
import type { AppData } from "./types.js";

const DATA_FILE = path.join(process.cwd(), "data.json");

const DEFAULT_DATA: AppData = {
  adminPassword: "Yashwant@2000",
  workSite: {
    name: "Sonorous — Vapi",
    center: { latitude: 20.3734, longitude: 72.9141 },
    radiusMeters: 1000,
  },
  employees: [
    { id: "e1", employeeId: "EMP001", password: "pass1234", name: "Aria Nakamura", role: "Lead Product Designer", avatarColor: 0xFF26E8FF, baseSalary: 85000, status: "CHECKED_OUT" },
    { id: "e2", employeeId: "EMP002", password: "pass1234", name: "Marcus Vela", role: "Backend Engineer", avatarColor: 0xFF9D5BFF, baseSalary: 75000, status: "CHECKED_IN" },
    { id: "e3", employeeId: "EMP003", password: "pass1234", name: "Lena Frost", role: "QA Analyst", avatarColor: 0xFFFF4ECB, baseSalary: 55000, status: "CHECKED_IN" },
    { id: "e4", employeeId: "EMP004", password: "pass1234", name: "Dev Okafor", role: "DevOps", avatarColor: 0xFF5BFFB0, baseSalary: 80000, status: "ON_LEAVE" },
    { id: "e5", employeeId: "EMP005", password: "pass1234", name: "Sora Pierce", role: "Data Scientist", avatarColor: 0xFFFFC24B, baseSalary: 72000, status: "CHECKED_OUT" },
  ],
  logs: [],
  leaves: [],
};

let data: AppData;

function generateId(prefix: string): string {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

export function loadData(): void {
  try {
    if (fs.existsSync(DATA_FILE)) {
      const raw = fs.readFileSync(DATA_FILE, "utf-8");
      data = JSON.parse(raw);
    } else {
      data = JSON.parse(JSON.stringify(DEFAULT_DATA));
      saveData();
    }
  } catch {
    data = JSON.parse(JSON.stringify(DEFAULT_DATA));
    saveData();
  }
}

export function saveData(): void {
  fs.writeFileSync(DATA_FILE, JSON.stringify(data, null, 2), "utf-8");
}

export function getData(): AppData {
  return data;
}

export function setData(newData: Partial<AppData>): void {
  Object.assign(data, newData);
  saveData();
}

export { generateId, DATA_FILE };
