import type { PublicTicket } from "../types/ticket";
const base=(import.meta.env.VITE_API_BASE_URL??"http://localhost:8080").replace(/\/$/,"");
export type PaymentOrder={paymentOrderId:string;paymentCode:string;amount:number;status:string};
async function request<T>(path:string,body:unknown,key?:string):Promise<T>{const r=await fetch(base+path,{method:"POST",headers:{"Content-Type":"application/json",...(key?{"Idempotency-Key":key}:{})},body:JSON.stringify(body)});if(!r.ok)throw new Error("Payment request failed");return r.json() as Promise<T>}
export const createPaymentOrder=(ticket:PublicTicket,token:string)=>request<PaymentOrder>("/api/payments/orders",{targetType:"PARKING_SESSION",targetId:ticket.sessionId,lookupToken:token},crypto.randomUUID());
export const simulatePayment=(order:PaymentOrder)=>request<{status:string}>("/api/payments/simulations/success",{paymentOrderId:order.paymentOrderId,paymentCode:order.paymentCode,amount:order.amount},crypto.randomUUID());
