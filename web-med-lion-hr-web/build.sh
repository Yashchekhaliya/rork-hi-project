#!/bin/bash
PATH="$(dirname "$0")/node_modules/.bin:$PATH" bun x vite build
