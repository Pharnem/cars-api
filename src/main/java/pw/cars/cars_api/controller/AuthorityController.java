package pw.cars.cars_api.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pw.cars.cars_api.model.PostCustomerData;
import pw.cars.cars_api.service.AuthorityService;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import java.io.IOException;

@RestController
@RequestMapping()
class AuthorityController {

    @Autowired
    private AuthorityService authorityService;

    @PutMapping(path="/session")
    public ResponseEntity<Void> login(@RequestHeader(name = "Authorization") String basicAuth) {
        ResponseEntity<Void> bad = ResponseEntity.badRequest()
                .header("WWW-Authenticate",
                        "Basic realm=\"Customer\"").build();
        if (!basicAuth.toLowerCase().startsWith("basic ")) {
            return bad;
        }
        String decoded = new String(
                Base64.getUrlDecoder().decode(basicAuth.split("\\s+", 2)[1]),
                StandardCharsets.UTF_8);
        String[] user_pass = decoded.split(":");
        System.out.println(user_pass[0]);
        System.out.println(user_pass[1]);
        if (user_pass.length != 2) {
            return bad;
        }
        return authorityService.login(user_pass[0], user_pass[1]);
    }

    @PostMapping(path="/customer")
    public ResponseEntity<Void> register(@ModelAttribute PostCustomerData customerData) {
        return authorityService.register(customerData);
    }
}