package pw.cars.cars_api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pw.cars.cars_api.model.*;

import java.net.URI;
import java.util.List;

@Repository
public class RentalService {
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    public ResponseEntity<List<Rental>> listRentals(Long page, Long page_size, String customerId, Boolean cancelled) {
        return ResponseEntity.ofNullable(jdbcTemplate.query(
                """
                        SELECT R.rental_id, R.customer_id, R.car_id, R.start_at, R.end_at, R.duration_days, R.cancelled
                        FROM Rental AS R
                        WHERE (:cancelled IS NULL OR R.cancelled = :cancelled)
                            AND (:customerId IS NULL OR R.customer_id = :customerId)
                        LIMIT :count OFFSET :offset;""",
                new MapSqlParameterSource()
                        .addValue("cancelled", cancelled)
                        .addValue("customerId", customerId)
                        .addValue("count", page_size)
                        .addValue("offset", page*page_size),
                rentalRowMapper()
        ));
    }

    public ResponseEntity<Rental> getRentalById(String rentalId) {
        var ret = jdbcTemplate.query(
                """
                        SELECT R.rental_id, R.customer_id, R.car_id, R.start_at, R.end_at, R.duration_days, R.cancelled
                        FROM Rental AS R
                        WHERE R.rental_id = :rentalId;""",
                new MapSqlParameterSource()
                        .addValue("rentalId", rentalId),
                rentalRowMapper()
        );
        if (ret.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ofNullable(ret.getFirst());
    }

    public ResponseEntity<String> addRental(String customerId, PostRentalData rental) {
        if (Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT (NOT EXISTS(SELECT * FROM AvailableCar WHERE car_id=:car));",
                new MapSqlParameterSource()
                        .addValue("car",rental.carId()),
                Boolean.class))) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        String rentalId = jdbcTemplate.queryForObject("SELECT MAKE_ID();",new MapSqlParameterSource(),String.class);
        int count = jdbcTemplate.update(
                """
                        INSERT INTO Rental (rental_id, customer_id, car_id, start_at, end_at)
                        VALUES (:id,:customer,:car,:start,:end);""",
                new MapSqlParameterSource()
                        .addValue("id", rentalId)
                        .addValue("customer",customerId)
                        .addValue("car",rental.carId())
                        .addValue("start",rental.startAt())
                        .addValue("end",rental.endAt())

        );
        if (count==0) {
            return ResponseEntity.badRequest().build();
        }
        URI uri = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/rentals/{rental_id}")
                .build(rentalId);
        return ResponseEntity.created(uri).build();
    }

    private RowMapper<Rental> rentalRowMapper() {
        return (rs, rowNum) -> {
            Rental rental = new Rental();
            rental.rentalId = rs.getString("rental_id");
            rental.carId = rs.getString("car_id");
            rental.customerId = rs.getString("customer_id");
            rental.startAt = rs.getTimestamp("start_at");
            rental.endAt = rs.getTimestamp("end_at");
            rental.cancelled = rs.getBoolean("cancelled");
            return rental;
        };
    }
}