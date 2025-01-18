package pw.cars.cars_api.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pw.cars.cars_api.model.Car;
import pw.cars.cars_api.model.PostCarData;
import pw.cars.cars_api.service.CarService;

import java.util.List;

@RestController
@RequestMapping("/cars")
class CarController {

    @Autowired
    private CarService carService;

    @GetMapping
    public ResponseEntity<List<Car>> getAllCars(
            @RequestParam(name="p",defaultValue="0",required = false) String page,
            @RequestParam(name="psize",defaultValue="10",required = false) String pageSize,
            @RequestParam(name="available",defaultValue="true",required = false) Boolean availableOnly
            ) {
        return carService.listCars(Long.parseLong(page),Long.parseLong(pageSize),availableOnly);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Car> getCarById(@PathVariable String id) {
        return carService.getCarById(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> addCar(@ModelAttribute PostCarData car) {
        return carService.addCar(car);
    }

    @PutMapping(path = "/{carId}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> updateCar(@PathVariable String carId, @RequestBody PostCarData car) {
        return carService.replaceCar(carId, car);
    }

    @DeleteMapping("/{carId}")
    public ResponseEntity<String> deleteCar(@PathVariable String carId) {
        return ResponseEntity.status(405).build();
    }
}