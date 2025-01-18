package pw.cars.cars_api.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pw.cars.cars_api.model.PostRentalData;
import pw.cars.cars_api.model.Rental;
import pw.cars.cars_api.service.RentalService;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/rentals")
class RentalController {

    @Autowired
    private RentalService rentalService;

    @GetMapping()
    public ResponseEntity<List<Rental>> listRentals
            (@CookieValue("customer-token") String customerId,
            @RequestParam(name="p",defaultValue="1",required = false) Long page,
            @RequestParam(name="psize",defaultValue="5",required = false) Long pageSize,
            @RequestParam(name = "cancelled", defaultValue = "",required = false) String cancelled
    )
    {
        return rentalService.listRentals(
                page,
                pageSize,
                customerId,
                Objects.equals(cancelled, "only")      ? Boolean.TRUE
                : Objects.equals(cancelled, "exclude") ? Boolean.FALSE
                :                                           null
        );
    }

    @GetMapping(path="/{id}")
    public ResponseEntity<Rental> getRentalById(@PathVariable String id)
    {
        return rentalService.getRentalById(id);
    }

    @PostMapping()
    public ResponseEntity<String> addRental(@CookieValue("customer-token") String customerId,
                                            @ModelAttribute PostRentalData rental) {
        return rentalService.addRental(customerId,rental);
    }
}