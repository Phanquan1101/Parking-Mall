export type PublicTicket = {
  sessionId: string;
  sessionCode: string;
  vehiclePlate: string;
  vehicleType: string;
  status: string;
  paymentStatus: string;
  entryTime: string;
  durationMinutes: number;
  estimatedFee: number;
  discountAmount: number;
  finalFee: number;
  canGenerateExitPass: boolean;
  exitPassAvailable: boolean;
  exitPassMessage: string;
  message: string;
};
