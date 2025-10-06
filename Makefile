## Variables
MVN := $(if $(wildcard ./mvnw),./mvnw,mvn)
SHELL := /bin/bash

# Project configuration
PROJECT_VERSION ?= $(shell $(MVN) help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null || echo "0.1.0-SNAPSHOT")
PROJECT_ARTIFACT_ID ?= tamarind
MAIN_CLASS ?= io.github.cogitatortech.tamarind.TamarindServer

# Server configuration
SERVER_HOST ?= localhost
SERVER_PORT ?= 8081
DEV_PORT ?= ${SERVER_PORT}

# Build configuration
MAVEN_OPTS ?= -B
SKIP_TESTS ?= false
JAVA_OPTS ?=

# Directories
TARGET_DIR := target
QUARKUS_APP_DIR := $(TARGET_DIR)/quarkus-app
RELEASE_DIR := release-dist
TMP_DIRS := $(TARGET_DIR) qodana-results $(RELEASE_DIR)

# Artifacts
RUNNER_JAR = $(TARGET_DIR)/$(PROJECT_ARTIFACT_ID)-$(PROJECT_VERSION)-runner.jar
QUARKUS_JAR = $(QUARKUS_APP_DIR)/quarkus-run.jar
SOURCES_JAR = $(TARGET_DIR)/$(PROJECT_ARTIFACT_ID)-$(PROJECT_VERSION)-sources.jar
JAVADOC_JAR = $(TARGET_DIR)/$(PROJECT_ARTIFACT_ID)-$(PROJECT_VERSION)-javadoc.jar

# Docker configuration
DOCKER_IMAGE ?= $(PROJECT_ARTIFACT_ID)
DOCKER_TAG ?= latest
DOCKER_REGISTRY ?=

# Qodana configuration
QODANA_IMAGE ?= jetbrains/qodana-jvm-community:latest
QODANA_RESULTS_DIR := qodana-results

# Default target executed when 'make' is run without arguments
.DEFAULT_GOAL := help

# Phony targets don't represent files
.PHONY: help build package release test server dev format lint clean \
	setup-hooks test-hooks qodana qodana-docker docker-build docker-run \
	release-package info

help: ## Show this help message
	@echo "Usage: make <target>"
	@echo ""
	@echo "Targets:"
	@grep -E '^[a-zA-Z_-]+:.*## .*$$' $(MAKEFILE_LIST) | \
	awk 'BEGIN {FS = ":.*## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'
	@echo ""
	@echo "Configuration variables (can be overridden):"
	@echo "  SERVER_HOST=$(SERVER_HOST)"
	@echo "  SERVER_PORT=$(SERVER_PORT)"
	@echo "  SKIP_TESTS=$(SKIP_TESTS)"
	@echo "  DOCKER_IMAGE=$(DOCKER_IMAGE)"
	@echo "  DOCKER_TAG=$(DOCKER_TAG)"

info: ## Show project information
	@echo "Project: $(PROJECT_ARTIFACT_ID)"
	@echo "Version: $(PROJECT_VERSION)"
	@echo "Main Class: $(MAIN_CLASS)"
	@echo "Maven: $(MVN)"
	@echo "Target Directory: $(TARGET_DIR)"
	@echo "Quarkus JAR: $(QUARKUS_JAR)"

build: ## Run the full Maven build lifecycle (compile, check, test, and package)
	@echo "Building project and running all checks..."
	@$(MVN) $(MAVEN_OPTS) verify

package: ## Compile and package the application into a JAR file
	@echo "Packaging application..."
	@$(MVN) $(MAVEN_OPTS) package $(if $(filter true,$(SKIP_TESTS)),-DskipTests,)

release: ## Create a release build
	@echo "Creating release build..."
	@$(MVN) $(MAVEN_OPTS) clean package -DskipTests
	@echo ""
	@echo "================================================================"
	@echo "Release build complete!"
	@echo "================================================================"
	@echo ""
	@echo "Artifacts created:"
	@ls -lh $(TARGET_DIR)/*.jar 2>/dev/null || true
	@echo ""
	@echo "To run:"
	@echo "  java -jar $(QUARKUS_JAR)"
	@echo ""
	@echo "To build Docker image:"
	@echo "  make docker-build"
	@echo ""

release-package: release ## Create release distribution packages (tar.gz and zip bundles)
	@echo "Creating release packages..."
	@mkdir -p $(RELEASE_DIR)
	@cd $(QUARKUS_APP_DIR) && tar -czf ../../$(RELEASE_DIR)/$(PROJECT_ARTIFACT_ID)-$(PROJECT_VERSION).tar.gz *
	@cd $(QUARKUS_APP_DIR) && zip -qr ../../$(RELEASE_DIR)/$(PROJECT_ARTIFACT_ID)-$(PROJECT_VERSION).zip *
	@cp $(SOURCES_JAR) $(RELEASE_DIR)/ 2>/dev/null || true
	@cp $(JAVADOC_JAR) $(RELEASE_DIR)/ 2>/dev/null || true
	@echo "Release packages created in $(RELEASE_DIR):"
	@ls -lh $(RELEASE_DIR)/

test: ## Run all the tests
	@echo "Running tests..."
	@$(MVN) $(MAVEN_OPTS) test

server: clean ## Start the Tamarind server
	@echo "Starting Tamarind server on $(SERVER_HOST):$(SERVER_PORT)..."
	# Fail fast if the requested port is already in use to avoid confusion
	@if ss -ltn 2>/dev/null | awk '{print $4}' | grep -qE ":$(SERVER_PORT)(\$|:)"; then \
		echo "Error: port $(SERVER_PORT) appears to be in use. If you want to run on a different port, use: make server SERVER_PORT=<port> or export SERVER_PORT=<port>"; \
		exit 1; \
	fi; \
	# Always (re)build to ensure latest dependencies and resources are packaged
	@$(MAKE) package SKIP_TESTS=true
	# Determine engine type (env var TAMARIND_ENGINE_TYPE takes precedence; default to duckdb)
	@ENGINE_TYPE="$(TAMARIND_ENGINE_TYPE)"; \
	if [ -z "$$ENGINE_TYPE" ]; then ENGINE_TYPE="duckdb"; fi; \
	if [ "$$ENGINE_TYPE" = "postgresql" ]; then \
	  if [ -z "$(TAMARIND_ENGINE_POSTGRESQL_PASSWORD)" ] && [ -z "$(POSTGRES_PASSWORD)" ]; then \
	    echo "Error: tamarind.engine.postgresql.password is required when using PostgreSQL (engine.type=postgresql)."; \
	    echo "Set it by exporting an environment variable or passing it to make:"; \
	    echo "  export TAMARIND_ENGINE_POSTGRESQL_PASSWORD=yourpassword && make server"; \
	    echo "  OR"; \
	    echo "  make server TAMARIND_ENGINE_POSTGRESQL_PASSWORD=yourpassword"; \
	    echo "  OR (less recommended for shared environments):"; \
	    echo "  make server JAVA_OPTS='-Dtamarind.engine.postgresql.password=yourpassword'"; \
	    exit 1; \
	  fi; \
	else \
	  unset TAMARIND_ENGINE_POSTGRESQL_PASSWORD POSTGRES_PASSWORD; \
	fi; \
	PASSWORD_FLAG=""; \
	if [ -n "$(TAMARIND_ENGINE_POSTGRESQL_PASSWORD)" ]; then \
	  PASSWORD_FLAG="-Dtamarind.engine.postgresql.password=$(TAMARIND_ENGINE_POSTGRESQL_PASSWORD)"; \
	elif [ -n "$(POSTGRES_PASSWORD)" ]; then \
	  PASSWORD_FLAG="-Dtamarind.engine.postgresql.password=$(POSTGRES_PASSWORD)"; \
	fi; \
	echo "Using engine type: $$ENGINE_TYPE"; \
	java $(JAVA_OPTS) $$PASSWORD_FLAG -Dtamarind.engine.type=$$ENGINE_TYPE \
		-Dquarkus.oidc.enabled=false \
		-Dquarkus.http.host=$(SERVER_HOST) \
		-Dquarkus.http.port=$(SERVER_PORT) \
		-jar $(QUARKUS_JAR)

dev: ## Start the server in development mode
	@echo "Starting server in dev mode on port $(DEV_PORT)..."
	# Check if dev port is available
	@if ss -ltn 2>/dev/null | awk '{print $4}' | grep -qE ":$(DEV_PORT)(\$|:)"; then \
		echo "Error: dev port $(DEV_PORT) appears to be in use. Run with: make dev DEV_PORT=<port>"; \
		exit 1; \
	fi; \
	@$(MVN) $(MAVEN_OPTS) quarkus:dev -Dquarkus.http.port=$(DEV_PORT)

format: ## Format Java source files
	@echo "Formatting source code..."
	@$(MVN) $(MAVEN_OPTS) spotless:apply

lint: ## Check code style
	@echo "Checking code style..."
	@$(MVN) $(MAVEN_OPTS) checkstyle:check

clean: ## Remove all build artifacts
	@echo "Cleaning project..."
	@$(MVN) $(MAVEN_OPTS) clean
	@rm -rf $(TMP_DIRS)
	@echo "Cleaned: $(TMP_DIRS)"

docker-build: ## Build Docker image
	@echo "Building Docker image: $(DOCKER_IMAGE):$(DOCKER_TAG)..."
	@docker build -t $(DOCKER_IMAGE):$(DOCKER_TAG) .
	@if [ -n "$(DOCKER_REGISTRY)" ]; then \
		docker tag $(DOCKER_IMAGE):$(DOCKER_TAG) $(DOCKER_REGISTRY)/$(DOCKER_IMAGE):$(DOCKER_TAG); \
		echo "Tagged: $(DOCKER_REGISTRY)/$(DOCKER_IMAGE):$(DOCKER_TAG)"; \
	fi

docker-run: ## Run Docker container
	@echo "Running Docker container..."
	@docker run --rm -it \
		-p $(SERVER_PORT):8080 \
		-p 8815:8815 \
		-e JAVA_OPTS="$(JAVA_OPTS)" \
		$(DOCKER_IMAGE):$(DOCKER_TAG)

setup-hooks: ## Install Git hooks (pre-commit and pre-push)
	@echo "Setting up Git hooks..."
	@if ! command -v pre-commit &> /dev/null; then \
	   echo "Error: pre-commit not found. Install it using: pip install pre-commit"; \
	   exit 1; \
	fi
	@pre-commit install --hook-type pre-commit
	@pre-commit install --hook-type pre-push
	@pre-commit install-hooks
	@echo "Git hooks installed successfully"

test-hooks: ## Test Git hooks on all files
	@echo "Testing Git hooks..."
	@pre-commit run --all-files --show-diff-on-failure

# Ensure JAR exists before running server
$(QUARKUS_JAR):
	@echo "Quarkus JAR not found. Building..."
	@$(MAKE) package
