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

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final RazorpayService razorpayService;
    private final RegistrationService registrationService;

    @PostMapping(value = "/razorpay", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        log.info("[WEBHOOK] Incoming Razorpay webhook, signature present: {}", signature != null);

        if (signature == null || signature.isBlank()) {
            log.warn("[WEBHOOK] Request missing X-Razorpay-Signature header — ignoring.");
            return ResponseEntity.ok("ok");
        }

        if (!razorpayService.verifyWebhookSignature(rawBody, signature)) {
            log.warn("[WEBHOOK] Invalid signature — request rejected silently.");
            return ResponseEntity.ok("ok");
        }

        registrationService.handleRazorpayWebhook(rawBody);

        return ResponseEntity.ok("ok");
    }
}
