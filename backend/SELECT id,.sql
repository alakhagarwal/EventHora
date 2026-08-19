SELECT id,
       booked_at,
       member_id,
       member_type,
       payment_preference,
       payment_status,
       quantity,
       razorpay_order_id,
       ticket_reference,
       total_amount,
       event_id,
       is_checked_in,
       razorpay_payment_id,
       checked_in_at
FROM public.registrations
LIMIT 1000;