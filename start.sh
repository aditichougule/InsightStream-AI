#!/bin/bash

# AI Video Intelligence Platform - Quick Start Script
# This script sets up and runs the Spring Boot application

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo "🚀 AI Video Intelligence Platform - Quick Start"
echo "================================================"
echo ""

# Check Java
echo "✓ Checking Java..."
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Please install Java 17+"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | grep -oP 'version "\K.*(?=")')
echo "  Using Java $JAVA_VERSION"
echo ""

# Check Maven
echo "✓ Checking Maven..."
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven not found. Please install Maven 3.8+"
    exit 1
fi
MVN_VERSION=$(mvn -v 2>&1 | head -n1)
echo "  $MVN_VERSION"
echo ""

# Check PostgreSQL
echo "✓ Checking PostgreSQL..."
if ! command -v psql &> /dev/null; then
    echo "⚠️  PostgreSQL not found. Please install it:"
    echo "   brew install postgresql"
    echo "   brew services start postgresql"
    exit 1
fi

echo "  PostgreSQL is installed"
echo ""

# Create database if not exists
echo "✓ Setting up database..."
if ! psql -U postgres -d postgres -c "SELECT 1 FROM pg_database WHERE datname = 'ai_video_ip'" | grep -q 1; then
    echo "  Creating database 'ai_video_ip'..."
    createdb ai_video_ip 2>/dev/null || true
    echo "  ✓ Database created"
else
    echo "  ✓ Database already exists"
fi
echo ""

# Build project
echo "✓ Building project..."
mvn clean package -q -DskipTests
echo "  ✓ Build successful"
echo ""

# Start application
echo "🎉 Starting AI Video Intelligence Platform..."
echo ""
echo "📱 Application running at: http://localhost:8080"
echo "📚 API Documentation: http://localhost:8080/swagger-ui/index.html"
echo ""
echo "Press Ctrl+C to stop the application"
echo ""

mvn spring-boot:run
