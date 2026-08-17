package com.eventHora.backend.service;

import com.eventHora.backend.Enum.MemberType;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Wraps all direct interactions with the Razorpay Java SDK.
 *
 * Responsibilities:
 *  1. createOrder()        — Tells Razorpay "Hey, I want to collect ₹X from someone".
 *                            Razorpay returns a secure Order ID that the frontend uses
 *                            to open the Razorpay payment popup.
 *
 *  2. verifySignature()    — After the user pays, Razorpay sends back a cryptographic
 *                            HMAC-SHA256 signature. We verify it to prove the payment
 *                            data was NOT tampered with by the frontend.
 *
 * This service is intentionally kept free of any business logic. It only knows
 * how to talk to Razorpay. RegistrationService orchestrates when to call it.
 */
@Slf4j
@Service
public class RazorpayService {

    // ─── Configuration ─────────────────────────────────────────────────────────

    @Value("${razorpay.api.key}")
    private String keyId;

    @Value("${razorpay.api.secret}")
    private String keySecret;

    // Webhook secret is DIFFERENT from the API secret.
    // It is set when you create the webhook in the Razorpay dashboard.
    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    // ─── Order Creation ────────────────────────────────────────────────────────

    /**
     * Creates a Razorpay Order for the given amount.
     *
     * Razorpay always works in the SMALLEST currency unit:
     *   ₹1000 → 100000 paise
     *   ₹1.50 → 150 paise
     *
     * @param  totalAmountInRupees  The total amount to collect (e.g. BigDecimal("1000.00"))
     * @param  receiptId            A unique internal reference you choose (e.g. our ticketReference).
     *                              Razorpay stores this alongside the order for reconciliation.
     * @return                      The Razorpay Order ID string (e.g. "order_PwZa8xyz...")
     * @throws RazorpayException    If the Razorpay API call fails (network error, bad keys, etc.)
     */
    public String createOrder(BigDecimal totalAmountInRupees, String receiptId) throws RazorpayException {

        // Razorpay SDK requires a fresh client per call (it's a thin stateless wrapper)
        RazorpayClient client = new RazorpayClient(keyId, keySecret);

        // Build the payload Razorpay expects
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", totalAmountInRupees
                .multiply(BigDecimal.valueOf(100))   // ₹1000 → 100000 paise
                .intValue());
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", receiptId);      // Your own internal reference

        // Tell Razorpay to auto-capture payment as soon as the user pays.
        // Without this, the money sits in an "authorized but not captured" limbo.
        orderRequest.put("payment_capture", 1);

        Order order = client.orders.create(orderRequest);

        String razorpayOrderId = order.get("id");
        log.info("[RAZORPAY] Order created → id={}, amount={} paise, receipt={}",
                razorpayOrderId,
                order.get("amount"),
                receiptId);

        return razorpayOrderId;
    }

    // ─── Payment Link Creation ─────────────────────────────────────────────────

    /**
     * Result holder for a Razorpay Payment Link creation.
     */
    public record PaymentLinkResult(String id, String shortUrl) {}

    /**
     * Creates a Razorpay Payment Link and (when SMS/email is connected) sends
     * it directly to the member's phone or email via Razorpay's built-in notification.
     *
     * CURRENT IMPLEMENTATION — Mock/Log Mode:
     *   Since SMS and email delivery are not yet integrated, this method:
     *   1. Attempts to create a real Razorpay Payment Link via the API.
     *   2. Logs the payment link URL prominently so it can be copied manually.
     *   3. Falls back to a mock URL if the API call fails (e.g. test-key restrictions).
     *
     * FUTURE:
     *   Replace the log statement with actual SMS (Twilio/MSG91) or email (SendGrid)
     *   delivery. The rest of the flow stays the same.
     *
     * @param amount       Total amount in rupees
     * @param contact      Phone number (INDIAN) or email address (OVERSEAS)
     * @param memberType   Determines the notify channel (sms vs email)
     * @param description  Human-readable description shown on the payment page
     * @param expiresAt    When the link should expire (use registration deadline)
     * @param receiptId    Our internal ticket reference for reconciliation
     * @return             PaymentLinkResult containing the link ID and short URL
     */
    public PaymentLinkResult createPaymentLink(
            BigDecimal amount,
            String contact,
            MemberType memberType,
            String description,
            Instant expiresAt,
            String receiptId) {

        String shortUrl;
        String linkId;

        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            JSONObject customerInfo = new JSONObject();
            if (memberType == MemberType.INDIAN) {
                customerInfo.put("contact", "+91" + contact.replaceAll("\\D", ""));
            } else {
                customerInfo.put("email", contact);
            }

            JSONObject notify = new JSONObject();
            notify.put("sms",   memberType == MemberType.INDIAN);
            notify.put("email", memberType == MemberType.OVERSEAS);

            JSONObject payload = new JSONObject();
            payload.put("amount",          amount.multiply(BigDecimal.valueOf(100)).intValue()); // paise
            payload.put("currency",        "INR");
            payload.put("description",     description);
            payload.put("customer",        customerInfo);
            payload.put("notify",          notify);
            payload.put("reminder_enable", false);
            payload.put("expire_by",       expiresAt.getEpochSecond());
            payload.put("reference_id",    receiptId);

            com.razorpay.PaymentLink link = client.paymentLink.create(payload);
            linkId   = link.get("id");
            shortUrl = link.get("short_url");

            log.info("[PAYMENT-LINK] Created Razorpay Payment Link ✅ " +
                     "id={}, amount=₹{}, contact={}, receipt={}",
                     linkId, amount, contact, receiptId);

        } catch (RazorpayException e) {
            // API call failed (likely test-key restriction or network issue).
            // Generate a mock link so the flow continues during development.
            linkId   = "plink_MOCK_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            shortUrl = "https://rzp.io/l/MOCK-" + receiptId;
            log.warn("[PAYMENT-LINK] Razorpay API failed ({}), using MOCK link. " +
                     "Replace with real integration when SMS/email service is ready.", e.getMessage());
        }

        // ─── MOCK DELIVERY: Log the URL until SMS/email service is connected ───
        // TODO: Replace this block with actual SMS (MSG91/Twilio) or email (SendGrid) call.
        log.info("[PAYMENT-LINK] 📲 SEND TO MEMBER {} ({}): Payment Link = {}",
                 contact, memberType, shortUrl);
        log.info("[PAYMENT-LINK] Amount: ₹{} | Receipt: {} | Expires: {}",
                 amount, receiptId, expiresAt);

        return new PaymentLinkResult(linkId, shortUrl);
    }

    // ─── Signature Verification ────────────────────────────────────────────────

    /**
     * Verifies the HMAC-SHA256 signature that Razorpay attaches to every
     * successful payment callback.
     *
     * WHY THIS MATTERS:
     * After a user pays, the Razorpay popup closes and the frontend sends us:
     *   { razorpayOrderId, razorpayPaymentId, razorpaySignature }
     * A malicious user could fabricate these fields to claim they paid!
     * Razorpay prevents this by signing `orderId|paymentId` with YOUR secret key.
     * Only Razorpay (and you) know the secret, so if the signature matches,
     * the payment is 100% genuine.
     *
     * ALGORITHM:
     *   data      = razorpayOrderId + "|" + razorpayPaymentId
     *   expected  = HMAC_SHA256(data, keySecret)
     *   valid     = (expected == razorpaySignature)
     *
     * @param razorpayOrderId   The order ID we created earlier (e.g. "order_PwZa8xyz...")
     * @param razorpayPaymentId The payment ID Razorpay assigned (e.g. "pay_Qx3Rabc...")
     * @param signature         The signature Razorpay sent to the frontend after payment
     * @return                  true if the signature is valid, false if it was tampered with
     */
    public boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String signature) {
        try {
            String data = razorpayOrderId + "|" + razorpayPaymentId;

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Convert byte array to lowercase hex string
            String expectedSignature = HexFormat.of().formatHex(hash);

            boolean isValid = expectedSignature.equals(signature);

            if (isValid) {
                log.info("[RAZORPAY] Signature verified ✅ — orderId={}, paymentId={}",
                        razorpayOrderId, razorpayPaymentId);
            } else {
                log.warn("[RAZORPAY] Signature MISMATCH ❌ — possible tampering! orderId={}",
                        razorpayOrderId);
            }

            return isValid;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            // This should never happen in a correctly configured JVM
            log.error("[RAZORPAY] Signature verification failed due to crypto error: {}", e.getMessage());
            return false;
        }
    }

    // ─── Webhook Signature Verification ───────────────────────────────────────

    /**
     * Verifies the signature Razorpay attaches to every webhook POST request.
     *
     * HOW THIS DIFFERS FROM verifySignature():
     *   - verifySignature()        → used for frontend payment callbacks.
     *                                Signs: razorpayOrderId + "|" + razorpayPaymentId
     *                                Uses:  the API secret (keySecret)
     *
     *   - verifyWebhookSignature() → used for server-to-server Razorpay webhook calls.
     *                                Signs: the ENTIRE raw request body (JSON string)
     *                                Uses:  a SEPARATE webhook secret (webhookSecret)
     *
     * Razorpay sends the computed signature in the HTTP header: X-Razorpay-Signature
     *
     * @param rawBody   The raw JSON string of the entire webhook request body.
     *                  IMPORTANT: Must be the raw bytes as received, NOT re-serialized.
     * @param signature The value of the X-Razorpay-Signature header.
     * @return          true if the signature matches (webhook is genuine), false otherwise.
     */
    public boolean verifyWebhookSignature(String rawBody, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = HexFormat.of().formatHex(hash);

            boolean isValid = expectedSignature.equals(signature);

            if (isValid) {
                log.info("[RAZORPAY] Webhook signature verified ✅");
            } else {
                log.warn("[RAZORPAY] Webhook signature MISMATCH ❌ — possible spoofed request!");
            }

            return isValid;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("[RAZORPAY] Webhook signature verification failed due to crypto error: {}", e.getMessage());
            return false;
        }
    }

    // ─── Refunds ───────────────────────────────────────────────────────────────

    /**
     * Initiates a full refund for a previously captured Razorpay payment.
     *
     * Called when the sold-out race condition occurs in confirmPayment():
     *   — The member's card was charged by Razorpay, but the event just sold out
     *     before we could confirm their ticket. We owe them a full refund.
     *
     * Speed options:
     *   "normal"  — Standard refund, 5-7 business days. Reliable for all banks.
     *   "optimum" — Instant if the member's bank supports it, else falls back to normal.
     *
     * We deliberately use "normal" for maximum bank compatibility.
     *
     * @param razorpayPaymentId  The payment ID from Razorpay (e.g. "pay_Qx3Rabc...")
     * @throws RazorpayException If the Razorpay API call fails (caller should handle gracefully)
     */
    public void initiateRefund(String razorpayPaymentId) throws RazorpayException {
        RazorpayClient client = new RazorpayClient(keyId, keySecret);

        // No "amount" field = full refund of the entire captured amount
        JSONObject refundRequest = new JSONObject();
        refundRequest.put("speed", "normal");

        com.razorpay.Refund refund = client.payments.refund(razorpayPaymentId, refundRequest);

        log.info("[RAZORPAY] Refund initiated ✅ — paymentId={}, refundId={}, status={}",
                razorpayPaymentId,
                refund.get("id"),
                refund.get("status"));
    }
}
