package com.parkflow.mall.reservation.repository;

import com.parkflow.mall.reservation.model.Reservation;
import com.parkflow.mall.reservation.model.ReservationStatus;
import com.parkflow.mall.reservation.model.VehicleType;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryReservationRepository implements ReservationRepository {
    private final Map<String, Reservation> byCode = new ConcurrentHashMap<>();
    public Reservation save(Reservation reservation) { byCode.put(reservation.reservationCode(), reservation); return reservation; }
    public Optional<Reservation> findByCode(String code) { return Optional.ofNullable(byCode.get(code)); }
    public List<Reservation> findAll() { return byCode.values().stream().sorted(Comparator.comparing(Reservation::createdAt).reversed()).toList(); }
    public boolean hasActivePlate(String plate) { return byCode.values().stream().anyMatch(r -> r.normalizedVehiclePlate().equals(plate) && r.status() == ReservationStatus.RESERVED); }
    public long activeCountByVehicleType(VehicleType type) { return byCode.values().stream().filter(r -> r.vehicleType() == type && r.status() == ReservationStatus.RESERVED).count(); }
}
