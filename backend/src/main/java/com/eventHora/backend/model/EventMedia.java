package com.eventHora.backend.model;

import com.eventHora.backend.Enum.MediaType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single photo or video associated with an event's media gallery.
 *
 * Photos  → uploaded to S3; `url` stores the S3 object key.
 *            Served to clients via a presigned URL generated at read time.
 * Videos  → external embed link (YouTube / Vimeo).
 *            `url` stores the embed URL as-is; no S3 involved.
 *
 * Ordering is controlled by `sortOrder` (ascending). Admins can reorder gallery
 * items via PATCH /api/events/{id}/media/reorder.
 *
 * Mapped table: event_media
 */
@Entity
@Table(name = "event_media")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ─── Parent event ─────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // ─── Media details ────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaType mediaType;              // PHOTO or VIDEO

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;                       // S3 key (PHOTO) or embed URL (VIDEO)

    @Column(columnDefinition = "TEXT")
    private String caption;                   // Optional label shown below the media

    @Column(nullable = false)
    private int sortOrder;                    // 0-based; lower = shown first

    // ─── Audit ────────────────────────────────────────────────────────────────

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}
