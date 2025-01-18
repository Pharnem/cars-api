package pw.cars.cars_api.model;

public record PostRentalData(
        String carId,
        java.sql.Timestamp startAt,
        java.sql.Timestamp endAt)
{}