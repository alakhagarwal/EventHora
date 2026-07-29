export interface CheckInResponse {
  ticketReference: string;
  memberId: string;
  eventTitle: string;
  quantity: number;
  totalAmount: number;
  paymentStatus: string;
  alreadyCheckedIn: boolean;
  checkedInAt: string;
  message: string;
}

export interface LookupResponse {
  registrationId: string;
  ticketReference: string;
  memberId: string;
  memberType: string;
  quantity: number;
  totalAmount: number;
  paymentStatus: string;
  paymentPreference: string;
  isCheckedIn: boolean;
  checkedInAt: string | null;
  bookedAt: string;
}
