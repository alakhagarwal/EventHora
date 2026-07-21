package com.eventHora.backend.controller;

import com.eventHora.backend.service.RazorpayService;
import com.eventHora.backend.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives server-to-server webhook events from Razorpay.
 *
 * WHY A SEPARATE CONTROLLER?
 *   The webhook endpoint has a fundamentally different contract from the other endpoints:
 *   1. It MUST accept the body as a raw String (not parsed JSON).
 *      Spring's JSON parsing re-serializes the body, which can change whitespace and
 *      key ordering — this would break the HMAC-SHA256 signature check.
 *   2. It MUST always return 200 OK, even if our processing logic fails internally.
 *      Razorpay treats any non-2xx response as a failure and retries for up to 24 hours.
 *   3. It is completely unauthenticated at the HTTP level — security is enforced
 *      by verifying the Razorpay cryptographic signature on every single request.
 *
 * URL: POST /api/webhooks/razorpay
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final RazorpayService razorpayService;
    private final RegistrationService registrationService;

    /**
     * POST /api/webhooks/razorpay
     *
     * Entry point for all Razorpay webhook events.
     *
     * CRITICAL — Raw Body:
     *   We accept the body as a plain String (TEXT_PLAIN or application/json as String).
     *   Spring will still read the body, but won't parse it as a Java object.
     *   This preserves the exact byte sequence needed for signature verification.
     *
     * CRITICAL — Always 200:
     *   This endpoint ALWAYS returns 200 OK.
     *   If we return 4xx/5xx, Razorpay will retry the webhook repeatedly.
     *   Even if the signature is invalid (suspected forgery), we return 200 so
     *   the attacker gets no feedback about why it was rejected.
     *
     * Access: PUBLIC (secured by Razorpay HMAC-SHA256 signature verification)
     */
    @PostMapping(value = "/razorpay", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        log.info("[WEBHOOK] Incoming Razorpay webhook, signature present: {}", signature != null);

        // Step 1: Reject requests with no signature header immediately
        if (signature == null || signature.isBlank()) {
            log.warn("[WEBHOOK] Request missing X-Razorpay-Signature header — ignoring.");
            return ResponseEntity.ok("ok"); // still 200 — gives no useful info to attacker
        }

        // Step 2: Verify the cryptographic signature
        // If this fails, someone is trying to spoof a Razorpay event.
        if (!razorpayService.verifyWebhookSignature(rawBody, signature)) {
            log.warn("[WEBHOOK] Invalid signature — request rejected silently.");
            return ResponseEntity.ok("ok"); // still 200 — Razorpay won't retry; attacker gets nothing
        }

        // Step 3: Signature is valid — process the event
        // handleRazorpayWebhook() catches all exceptions internally and never throws,
        // so this call will always complete without crashing this endpoint.
        registrationService.handleRazorpayWebhook(rawBody);

        return ResponseEntity.ok("ok");
    }
}
