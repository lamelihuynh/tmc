# Common tasks. Run `make help`.
SHELL := /bin/bash
export JAVA_HOME ?= $(shell /usr/libexec/java_home -v 21 2>/dev/null)
GRADLE := ./gradlew

.DEFAULT_GOAL := help

help: ## show this help
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  %-18s %s\n", $$1, $$2}'

build: ## compile everything and run unit + architecture tests
	$(GRADLE) build

test: ## unit tests only (fast, no Docker)
	$(GRADLE) test

arch-test: ## only the architecture (dependency rule) tests of every service
	$(GRADLE) test --tests '*ArchitectureTest'

integration: ## tests tagged integration (needs Docker for Testcontainers)
	$(GRADLE) integrationTest

format: ## apply google-java-format to all Java sources
	$(GRADLE) spotlessApply

lint: ## check formatting without changing files
	$(GRADLE) spotlessCheck

up: ## start PostgreSQL, Redis, RabbitMQ (needs Docker)
	docker compose -f docker/docker-compose.yml up -d

down: ## stop infrastructure, keep data
	docker compose -f docker/docker-compose.yml down

nuke: ## stop infrastructure and DELETE its data
	docker compose -f docker/docker-compose.yml down -v

sims: ## build the simulator command line tools into build/install
	$(GRADLE) :simulators:device-simulator:installDist :simulators:kiosk-simulator:installDist

# ---- zero-Docker local stack (see docs/runbook/local-setup.md) ----
local-up: ## build if needed and start turnstile, user-qr, attendance, gate-station (no Docker)
	scripts/dev/local-stack.sh start

local-down: ## stop the local stack
	scripts/dev/local-stack.sh stop

local-restart: ## stop and start again: clean in-memory state
	scripts/dev/local-stack.sh restart

local-status: ## which local services are listening
	scripts/dev/local-stack.sh status

# or run each service yourself in its own terminal:
run-turnstile: ## turnstile simulator on :8090
	$(GRADLE) :simulators:turnstile-simulator:bootRun

run-user-qr: ## user-qr on :8083 (no database)
	SPRING_PROFILES_ACTIVE=local $(GRADLE) :services:user-qr:bootRun

run-attendance: ## attendance on :8082 (in memory, asks gate-station during reconcile)
	SPRING_PROFILES_ACTIVE=local $(GRADLE) :services:attendance:bootRun

run-gate-station: ## gate-station on :8081 (one-time-use state in memory, real HTTP peers)
	SPRING_PROFILES_ACTIVE=local TURNSTILE_MODE=http $(GRADLE) :services:gate-station:bootRun

# ---- scripted demos against the running local stack ----
demo-online: ## happy path: enroll a phone, scan, gate opens, replay is refused
	scripts/demo/online.sh

demo-offline: ## edge down: kiosk verifies alone, then hands the journal over
	scripts/demo/offline.sh

demo-timeout: ## turnstile answers too late: UNKNOWN, no second open, reconcile commits
	scripts/demo/timeout.sh

.PHONY: local-up local-down local-restart local-status help build test arch-test integration format lint up down nuke sims run-turnstile run-user-qr run-attendance run-gate-station demo-online demo-offline demo-timeout
