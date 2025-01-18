package pw.cars.cars_api.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import pw.cars.cars_api.service.ImageService;

import java.io.IOException;

@RestController
@RequestMapping("/images")
class ImageController {

    @Autowired
    private ImageService imageService;

    @GetMapping(path="/{id}",produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getImageById(@PathVariable String id) {
        return imageService.getImageById(id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> addImage(@RequestPart MultipartFile file) {
        try {
            return imageService.addImage(file.getBytes());
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}