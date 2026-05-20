#!/bin/bash

# Whisper Service Setup Script
# Sets up the Python environment and starts the Whisper transcription microservice

set -e

echo "=========================================="
echo "Whisper Transcription Service Setup"
echo "=========================================="

# Check Python installation
if ! command -v python3 &> /dev/null; then
    echo "❌ Python 3 is not installed. Please install Python 3.10 or later."
    exit 1
fi

PYTHON_VERSION=$(python3 --version | cut -d' ' -f2)
echo "✅ Python version: $PYTHON_VERSION"

# Check FFmpeg installation (required by Whisper)
if ! command -v ffmpeg &> /dev/null; then
    echo "❌ FFmpeg is not installed. Installing FFmpeg..."
    
    # Detect OS and install accordingly
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        if command -v brew &> /dev/null; then
            brew install ffmpeg
        else
            echo "❌ Homebrew not found. Please install FFmpeg manually."
            exit 1
        fi
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        # Linux
        sudo apt-get update
        sudo apt-get install -y ffmpeg
    else
        echo "❌ Unsupported OS. Please install FFmpeg manually."
        exit 1
    fi
fi

echo "✅ FFmpeg is installed"

# Create virtual environment if it doesn't exist
if [ ! -d "venv" ]; then
    echo "📦 Creating Python virtual environment..."
    python3 -m venv venv
fi

# Activate virtual environment
echo "🔧 Activating virtual environment..."
source venv/bin/activate

# Install dependencies
echo "📚 Installing Python dependencies..."
pip install --upgrade pip setuptools wheel
pip install -r requirements.txt

echo ""
echo "=========================================="
echo "✅ Setup Complete!"
echo "=========================================="
echo ""
echo "To start the service, run:"
echo "  source venv/bin/activate"
echo "  python app.py"
echo ""
echo "Or use:"
echo "  ./run.sh"
echo ""
echo "Service will run on http://localhost:5000"
echo "=========================================="
