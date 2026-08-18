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

@Slf4j
@Service
public class RazorpayService {

    @Value("${razorpay.api.key}")
    private String keyId;

    @Value("${razorpay.api.secret}")
    private String keySecret;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    public String createOrder(BigDecimal totalAmountInRupees, String receiptId) throws RazorpayException {

        RazorpayClient client = new RazorpayClient(keyId, keySecret);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", totalAmountInRupees
                .multiply(BigDecimal.valueOf(100))
                .intValue());
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", receiptId);

        orderRequest.put("payment_capture", 1);

        Order order = client.orders.create(orderRequest);

        String razorpayOrderId = order.get("id");
        log.info("[RAZORPAY] Order created → id={}, amount={} paise, receipt={}",
                razorpayOrderId,
                order.get("amount"),
                receiptId);

        return razorpayOrderId;
    }

    public boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String signature) {
        try {
            String data = razorpayOrderId + "|" + razorpayPaymentId;

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

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

            log.error("[RAZORPAY] Signature verification failed due to crypto error: {}", e.getMessage());
            return false;
        }
    }

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

    public void initiateRefund(String razorpayPaymentId) throws RazorpayException {
        RazorpayClient client = new RazorpayClient(keyId, keySecret);

        JSONObject refundRequest = new JSONObject();
        refundRequest.put("speed", "normal");

        com.razorpay.Refund refund = client.payments.refund(razorpayPaymentId, refundRequest);

        log.info("[RAZORPAY] Refund initiated ✅ — paymentId={}, refundId={}, status={}",
                razorpayPaymentId,
                refund.get("id"),
                refund.get("status"));
    }
}
