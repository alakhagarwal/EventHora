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
