package pw.cars.cars_api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pw.cars.cars_api.model.Car;
import pw.cars.cars_api.model.Location;
import pw.cars.cars_api.model.Model;
import pw.cars.cars_api.model.PostCarData;

import java.net.URI;
import java.util.List;

@Repository
public class CarService {
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    public ResponseEntity<List<Car>> listCars(Long page, Long page_size, Boolean availableOnly) {
        return ResponseEntity.ofNullable(jdbcTemplate.query(
                String.format("""
                        SELECT C.car_id, C.image_id,
                            M.model_id, M.name,
                            M.daily_rate, M.door_count, M.fuel_capacity,
                            M.production_year, M.seat_count, FT.name AS fuel_type,
                            L.location_id, L.country, L.city, L.postal_code, L.street, L.street_number, L.full_address,
                            ST_X(L.coordinates) AS latitude, ST_Y(L.coordinates) AS longitude
                        FROM %s AS C
                        INNER JOIN Model AS M USING (model_id)
                        INNER JOIN FuelType AS FT USING (fuel_type_id)
                        INNER JOIN Location AS L USING (location_id)
                        LIMIT :count OFFSET :offset;""", availableOnly ? "AvailableCar" : "Car"),
                new MapSqlParameterSource()
                        .addValue("count", page_size)
                        .addValue("offset", page*page_size),
                carRowMapper()));
    }

    public ResponseEntity<Car> getCarById(String carId) {
        List<Car> ret = jdbcTemplate.query(
                """
                        SELECT C.car_id, C.image_id,
                            M.model_id, M.name,
                            M.daily_rate, M.door_count, M.fuel_capacity,
                            M.production_year, M.seat_count, FT.name AS fuel_type,
                            L.location_id, L.country, L.city, L.postal_code, L.street, L.street_number, L.full_address, 
                            ST_X(L.coordinates) AS latitude, ST_Y(L.coordinates) AS longitude
                        FROM Car AS C
                        INNER JOIN Model AS M USING (model_id)
                        INNER JOIN FuelType AS FT USING (fuel_type_id)
                        INNER JOIN Location AS L USING (location_id)
                        WHERE C.car_id = :cid
                        LIMIT 1;""",
                new MapSqlParameterSource()
                        .addValue("cid", carId),
                carRowMapper());
        if (ret.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ofNullable(ret.getFirst());
    }

    public ResponseEntity<String> addCar0(PostCarData data) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                """
                        INSERT INTO Car (car_id, model_id, location_id, image_id) 
                        VALUES (MAKE_ID(),:model,:location,:image);""",
                new MapSqlParameterSource()
                        .addValue("model",data.modelId())
                        .addValue("location",data.locationId())
                        .addValue("image",data.imageId()),
                keyHolder,
                new String[] {"car_id"}
        );
        System.out.println(keyHolder.getKeyAs(String.class));
        return ResponseEntity.ofNullable(keyHolder.getKeyAs(String.class));
    }

    public ResponseEntity<Void> addCar(PostCarData data) {
        String carId = jdbcTemplate.queryForObject("SELECT MAKE_ID();",new MapSqlParameterSource(),String.class);
        int count = jdbcTemplate.update(
                """
                        INSERT INTO Car (car_id, model_id, location_id, image_id) 
                        VALUES (:id,:model,:location,:image);""",
                new MapSqlParameterSource()
                        .addValue("id", carId)
                        .addValue("model",data.modelId())
                        .addValue("location",data.locationId())
                        .addValue("image",data.imageId())
        );
        if (count==0) {
            return ResponseEntity.badRequest().build();
        }
        URI uri = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .pathSegment("cars/{carId}")
                .build(carId);
        return ResponseEntity.created(uri).build();
    }

    public ResponseEntity<Void> replaceCar(String carId, PostCarData data) {
        int changed = 0;
        try {
            changed = jdbcTemplate.update(
                    """
                            UPDATE Car
                            SET model_id=:model, location_id=:location, image_id=:image
                            WHERE car_id=:car;""",
                    new MapSqlParameterSource()
                            .addValue("car", carId)
                            .addValue("model", data.modelId())
                            .addValue("location", data.locationId())
                            .addValue("image", data.imageId())
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
        URI uri = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .pathSegment("cars/{carId}")
                .build(carId);
        return changed==0 ? ResponseEntity.notFound().build()
                : ResponseEntity.noContent().header("Content-Location",uri.toString()).build();
    }

    private RowMapper<Car> carRowMapper() {
        return (rs, rowNum) -> {
            Car car = new Car();
            car.carId = rs.getString("car_id");

            car.model = new Model();
            car.model.model_id = rs.getString("model_id");
            car.model.producer = null; //rs.getLong("producer_id");
            car.model.name = rs.getString("name");
            car.model.production_year = rs.getLong("production_year");
            car.model.fuel_type = rs.getString("fuel_type");
            car.model.fuel_capacity = rs.getLong("fuel_capacity");
            car.model.seat_count = rs.getLong("seat_count");
            car.model.door_count = rs.getLong("door_count");
            car.model.daily_rate = rs.getBigDecimal("daily_rate");

            car.image = rs.getString("image_id");

            car.location = new Location();
            car.location.location_id = rs.getString("location_id");
            car.location.country = rs.getString("country");
            car.location.city = rs.getString("city");
            car.location.postalCode = rs.getString("postal_code");
            car.location.street = rs.getString("street");
            car.location.streetNumber = rs.getString("street_number");
            car.location.latitude = rs.getFloat("latitude");
            car.location.longitude = rs.getFloat("longitude");
            car.location.address = rs.getString("full_address");
            return car;
        };
    }
}