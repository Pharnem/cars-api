package pw.cars.cars_api.model;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public class Car {
    public String carId;
    public Model model;
    public Location location;
    public String image;

    public String getUrl() {
        return ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .pathSegment("cars","{id}").build(carId).toString();
    }
}
