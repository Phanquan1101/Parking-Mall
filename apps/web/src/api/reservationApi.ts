const base=(import.meta.env.VITE_API_BASE_URL??"http://localhost:8080").replace(/\/$/,"");
export type Reservation={reservationId:string;reservationCode:string;vehiclePlate:string;vehicleType:"MOTORBIKE"|"CAR";reservedFrom:string;reservedUntil:string;expiresAt:string;status:"RESERVED"|"CANCELLED"|"EXPIRED"|"CONSUMED";message?:string};
async function read(r:Response){if(!r.ok)throw new Error((await r.text())||"Reservation request failed");return r.json() as Promise<Reservation>}
export function createReservation(body:{vehiclePlate:string;vehicleType:string;reservedFrom:string;reservedUntil:string;customerName?:string;customerPhone?:string}){return fetch(base+"/api/reservations",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(body)}).then(read)}
export function getReservation(code:string){return fetch(base+"/api/reservations/"+encodeURIComponent(code)).then(read)}
export function cancelReservation(code:string,reason:string){return fetch(base+"/api/reservations/"+encodeURIComponent(code)+"/cancel",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({reason})}).then(read)}
