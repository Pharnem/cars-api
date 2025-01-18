package pw.cars.cars_api.model;

public class Rental {
    public String rentalId;
    public String customerId;
    public String carId;
    public java.sql.Timestamp startAt, endAt;
    public Boolean cancelled;
}
