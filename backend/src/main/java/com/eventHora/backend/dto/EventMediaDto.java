package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.MediaType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for a single item in an event's media gallery.
 *
 * Included inside EventResponse and PublicEventResponse.
 *
 * For PHOTO: `url` is a presigned S3 URL (valid for 7 days).
 * For VIDEO: `url` is the external embed link (YouTube/Vimeo) — returned as-is, safe to
 *            drop into an <iframe> or <video> tag on the frontend.
 */
@Data
@Builder
public class EventMediaDto {

    private UUID id;
    private MediaType mediaType;   // PHOTO or VIDEO
    private String url;            // Presigned S3 URL (PHOTO) or embed URL (VIDEO)
    private String caption;        // Optional label
    private int sortOrder;         // 0-based display order (ascending)
    private LocalDateTime uploadedAt;
}
