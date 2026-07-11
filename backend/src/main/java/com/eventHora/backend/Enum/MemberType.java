package com.eventHora.backend.Enum;

/**
 * Determines how a member is identified and which OTP channel to use.
 *
 * INDIAN   → identified by Member ID + Mobile Number → OTP sent via WhatsApp
 * OVERSEAS → identified by Member ID + Email Address → OTP sent via Email
 *
 * This is set by the frontend based on which form the member fills in
 * and is sent along with the VerifyMemberRequest.
 */
public enum MemberType {
    INDIAN,     // Registered mobile number is the identifier
    OVERSEAS    // Registered email address is the identifier
}
