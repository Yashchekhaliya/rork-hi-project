#!/usr/bin/env node
// Wrapper that invokes vite from node_modules
const { spawn } = require('child_process');
const path = require('path');
const vitePath = path.join(__dirname, 'node_modules', 'vite', 'bin', 'vite.js');
const child = spawn(process.execPath, [vitePath, ...process.argv.slice(2)], { stdio: 'inherit' });
child.on('exit', code => process.exit(code));
