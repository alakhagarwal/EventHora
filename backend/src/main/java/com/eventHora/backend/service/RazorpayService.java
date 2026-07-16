package com.eventHora.backend.service;

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
import java.util.HexFormat;

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
