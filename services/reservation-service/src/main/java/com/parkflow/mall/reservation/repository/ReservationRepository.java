package com.parkflow.mall.reservation.repository;

import com.parkflow.mall.reservation.model.Reservation;
import com.parkflow.mall.reservation.model.ReservationStatus;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation save(Reservation reservation);
    Optional<Reservation> findByCode(String reservationCode);
    List<Reservation> findAll();
    boolean hasActivePlate(String normalizedPlate);
    long activeCountByVehicleType(com.parkflow.mall.reservation.model.VehicleType vehicleType);
}
