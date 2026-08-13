package com.eventHora.backend.Enum;

/**
 * Distinguishes between event gallery items:
 *  - PHOTO → uploaded directly to S3; served via presigned URL
 *  - VIDEO → external embed link (YouTube, Vimeo, etc.); stored and returned as-is
 */
public enum MediaType {
    PHOTO,
    VIDEO
}
