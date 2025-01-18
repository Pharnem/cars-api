package pw.cars.cars_api.model;

import java.math.BigDecimal;

public class Model {
    public String model_id;
    public Producer producer;
    public String name;
    public Long production_year;
    public String fuel_type;
    public Long fuel_capacity;
    public Long seat_count;
    public Long door_count;
    public BigDecimal daily_rate;
    public String slug;
}
