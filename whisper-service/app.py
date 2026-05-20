"""
AI Video Intelligence Platform - Whisper Transcription Microservice

A Python Flask microservice for transcribing audio files using OpenAI's Whisper model.
Provides REST API endpoints for audio-to-text conversion with timestamp segmentation.

Author: AI Video Intelligence Platform
Version: 1.0.0
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
from werkzeug.utils import secure_filename
import whisper
import os
import logging
from datetime import datetime
import traceback
from typing import Dict, List, Any
import json

# Configuration
app = Flask(__name__)
CORS(app)

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Configuration from environment or defaults
UPLOAD_FOLDER = os.getenv('UPLOAD_FOLDER', 'uploads/audio')
ALLOWED_EXTENSIONS = {'mp3', 'wav', 'ogg', 'flac', 'm4a', 'aac'}
WHISPER_MODEL = os.getenv('WHISPER_MODEL', 'base')
MAX_FILE_SIZE = int(os.getenv('MAX_FILE_SIZE', 5 * 1024 * 1024 * 1024))  # 5GB

app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['MAX_CONTENT_LENGTH'] = MAX_FILE_SIZE

# Initialize Whisper model on startup
logger.info(f"Loading Whisper model: {WHISPER_MODEL}")
whisper_model = whisper.load_model(WHISPER_MODEL)
logger.info(f"Whisper model loaded successfully")


def allowed_file(filename: str) -> bool:
    """Check if file extension is allowed."""
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS


def create_response(success: bool, message: str, data: Any = None, status_code: int = 200) -> tuple:
    """Create standardized JSON response."""
    response = {
        "success": success,
        "message": message,
        "data": data,
        "statusCode": status_code,
        "timestamp": datetime.utcnow().isoformat()
    }
    return jsonify(response), status_code


@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint to verify service is running."""
    return create_response(
        success=True,
        message="Whisper transcription service is running",
        data={
            "model": WHISPER_MODEL,
            "timestamp": datetime.utcnow().isoformat()
        }
    )


@app.route('/transcribe', methods=['POST'])
def transcribe_audio():
    """
    Transcribe audio file using Whisper model.
    
    Expected multipart form data:
        - file: Audio file (required)
        - language: Language code (optional, e.g., 'en', 'es')
    
    Response format:
    {
        "segments": [
            {"start": 0, "end": 5, "text": "Hello world"},
            {"start": 5, "end": 10, "text": "Welcome"}
        ],
        "full_text": "Hello world Welcome",
        "language": "en"
    }
    """
    try:
        # Validate file was provided
        if 'file' not in request.files:
            logger.warning("Transcribe request received without file")
            return create_response(
                success=False,
                message="No audio file provided",
                status_code=400
            )
        
        file = request.files['file']
        
        # Validate file is not empty
        if file.filename == '':
            logger.warning("Empty filename provided")
            return create_response(
                success=False,
                message="No file selected",
                status_code=400
            )
        
        # Validate file extension
        if not allowed_file(file.filename):
            logger.warning(f"Unsupported file type: {file.filename}")
            return create_response(
                success=False,
                message=f"Unsupported audio format. Allowed formats: {', '.join(ALLOWED_EXTENSIONS)}",
                status_code=415
            )
        
        # Get optional language parameter
        language = request.form.get('language', None)
        
        # Save uploaded file temporarily
        filename = secure_filename(file.filename)
        filepath = os.path.join(app.config['UPLOAD_FOLDER'], filename)
        
        # Ensure directory exists
        os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)
        
        logger.info(f"Saving uploaded file: {filename}")
        file.save(filepath)
        
        try:
            # Transcribe audio using Whisper
            logger.info(f"Starting transcription: {filename}")
            
            # Build transcribe parameters
            transcribe_params = {
                'model': whisper_model,
                'audio': filepath,
                'verbose': False
            }
            
            # Add language if specified
            if language:
                logger.info(f"Transcribing with language: {language}")
                transcribe_params['language'] = language
            
            # Perform transcription
            result = whisper.transcribe(**transcribe_params)
            
            logger.info(f"Transcription completed: {filename}")
            
            # Format response with segments
            segments = []
            for segment in result.get('segments', []):
                segments.append({
                    "start": int(segment['start']),
                    "end": int(segment['end']),
                    "text": segment['text'].strip()
                })
            
            # Get full text
            full_text = result.get('text', '').strip()
            detected_language = result.get('language', 'unknown')
            
            transcription_data = {
                "segments": segments,
                "full_text": full_text,
                "language": detected_language,
                "duration": result.get('duration', 0)
            }
            
            return create_response(
                success=True,
                message="Audio transcribed successfully",
                data=transcription_data,
                status_code=200
            )
        
        finally:
            # Clean up temporary file
            if os.path.exists(filepath):
                logger.info(f"Cleaning up temporary file: {filepath}")
                os.remove(filepath)
    
    except Exception as e:
        logger.error(f"Transcription failed: {str(e)}")
        logger.error(traceback.format_exc())
        return create_response(
            success=False,
            message=f"Transcription failed: {str(e)}",
            status_code=500
        )


@app.route('/transcribe/file', methods=['POST'])
def transcribe_file():
    """
    Transcribe audio file from disk path.
    
    Expected JSON body:
    {
        "file_path": "/path/to/audio.mp3",
        "language": "en" (optional)
    }
    """
    try:
        data = request.get_json()
        
        if not data or 'file_path' not in data:
            logger.warning("Transcribe request received without file_path")
            return create_response(
                success=False,
                message="No file path provided",
                status_code=400
            )
        
        filepath = data['file_path']
        language = data.get('language', None)
        
        # Validate file exists
        if not os.path.exists(filepath):
            logger.warning(f"File not found: {filepath}")
            return create_response(
                success=False,
                message=f"Audio file not found: {filepath}",
                status_code=404
            )
        
        logger.info(f"Starting transcription from disk: {filepath}")
        
        # Build transcribe parameters
        transcribe_params = {
            'model': whisper_model,
            'audio': filepath,
            'verbose': False
        }
        
        if language:
            logger.info(f"Transcribing with language: {language}")
            transcribe_params['language'] = language
        
        # Perform transcription
        result = whisper.transcribe(**transcribe_params)
        
        logger.info(f"Transcription completed: {filepath}")
        
        # Format response
        segments = []
        for segment in result.get('segments', []):
            segments.append({
                "start": int(segment['start']),
                "end": int(segment['end']),
                "text": segment['text'].strip()
            })
        
        full_text = result.get('text', '').strip()
        detected_language = result.get('language', 'unknown')
        
        transcription_data = {
            "segments": segments,
            "full_text": full_text,
            "language": detected_language,
            "duration": result.get('duration', 0)
        }
        
        return create_response(
            success=True,
            message="Audio transcribed successfully",
            data=transcription_data,
            status_code=200
        )
    
    except Exception as e:
        logger.error(f"Transcription failed: {str(e)}")
        logger.error(traceback.format_exc())
        return create_response(
            success=False,
            message=f"Transcription failed: {str(e)}",
            status_code=500
        )


@app.route('/models', methods=['GET'])
def get_available_models():
    """Get list of available Whisper models."""
    models = {
        "tiny": "Smallest model (39M parameters) - Fastest",
        "base": "Small model (74M parameters) - Good balance (current)",
        "small": "Medium model (244M parameters) - Better accuracy",
        "medium": "Large model (769M parameters) - High accuracy",
        "large": "Largest model (1.5B parameters) - Best accuracy"
    }
    
    return create_response(
        success=True,
        message="Available Whisper models",
        data={
            "models": models,
            "current_model": WHISPER_MODEL
        }
    )


@app.route('/info', methods=['GET'])
def get_service_info():
    """Get service information and statistics."""
    return create_response(
        success=True,
        message="Whisper transcription service info",
        data={
            "service": "Whisper Transcription Microservice",
            "version": "1.0.0",
            "model": WHISPER_MODEL,
            "upload_folder": UPLOAD_FOLDER,
            "allowed_formats": list(ALLOWED_EXTENSIONS),
            "max_file_size_gb": MAX_FILE_SIZE / (1024 * 1024 * 1024),
            "timestamp": datetime.utcnow().isoformat()
        }
    )


@app.errorhandler(413)
def request_entity_too_large(error):
    """Handle file too large error."""
    return create_response(
        success=False,
        message=f"File size exceeds maximum limit of {MAX_FILE_SIZE / (1024 * 1024 * 1024):.1f}GB",
        status_code=413
    )


@app.errorhandler(404)
def not_found(error):
    """Handle 404 errors."""
    return create_response(
        success=False,
        message="Endpoint not found",
        status_code=404
    )


@app.errorhandler(500)
def internal_error(error):
    """Handle 500 errors."""
    logger.error(f"Internal server error: {str(error)}")
    return create_response(
        success=False,
        message="Internal server error",
        status_code=500
    )


if __name__ == '__main__':
    port = int(os.getenv('PORT', 5000))
    debug = os.getenv('DEBUG', 'False').lower() == 'true'
    
    logger.info(f"Starting Whisper transcription service on port {port}")
    logger.info(f"Debug mode: {debug}")
    
    app.run(
        host='0.0.0.0',
        port=port,
        debug=debug,
        threaded=True
    )
