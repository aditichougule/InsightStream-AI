#!/bin/bash

# Whisper Service Run Script
# Starts the Whisper transcription microservice

set -e

echo "Starting Whisper Transcription Service..."

# Check if virtual environment exists
if [ ! -d "venv" ]; then
    echo "Virtual environment not found. Running setup first..."
    bash setup.sh
fi

# Activate virtual environment
source venv/bin/activate

# Load environment variables
if [ -f ".env" ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

echo "=========================================="
echo "Whisper Transcription Service"
echo "=========================================="
echo "Model: ${WHISPER_MODEL:-base}"
echo "Port: ${PORT:-5000}"
echo "Debug: ${DEBUG:-False}"
echo "=========================================="
echo ""
echo "Service running at http://localhost:${PORT:-5000}"
echo "Health check: http://localhost:${PORT:-5000}/health"
echo ""
echo "Press Ctrl+C to stop"
echo ""

# Run the Flask application
python app.py
