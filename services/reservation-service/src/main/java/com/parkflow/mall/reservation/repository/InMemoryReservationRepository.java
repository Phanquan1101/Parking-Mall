package com.parkflow.mall.reservation.repository;

import com.parkflow.mall.reservation.model.Reservation;
import com.parkflow.mall.reservation.model.ReservationStatus;
import com.parkflow.mall.reservation.model.VehicleType;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryReservationRepository implements ReservationRepository {

    private final Map<String, Reservation> byCode = new ConcurrentHashMap<>();

    @Override
    public Reservation save(Reservation reservation) {
        byCode.put(reservation.reservationCode(), reservation);
        return reservation;
    }

    @Override
    public Optional<Reservation> findByCode(String code) {
        return Optional.ofNullable(byCode.get(code));
    }

    @Override
    public List<Reservation> findAll() {
        return byCode.values()
                .stream()
                .sorted(Comparator.comparing(Reservation::createdAt).reversed())
                .toList();
    }

    @Override
    public boolean hasActivePlate(String plate) {
        return byCode.values()
                .stream()
                .anyMatch(r ->
                        r.normalizedVehiclePlate().equals(plate)
                                && r.status() == ReservationStatus.RESERVED
                );
    }

    @Override
    public long activeCountByVehicleType(VehicleType type) {
        return byCode.values()
                .stream()
                .filter(r ->
                        r.vehicleType() == type
                                && r.status() == ReservationStatus.RESERVED
                )
                .count();
    }
}