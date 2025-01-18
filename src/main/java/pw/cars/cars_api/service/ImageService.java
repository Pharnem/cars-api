package pw.cars.cars_api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.sql.Types;

@Repository
public class ImageService {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    public ResponseEntity<byte[]> getImageById(String imageId) {
        var data = jdbcTemplate.query(
                """
                        SELECT data FROM Image
                        WHERE image_id=:imageId""",
                new MapSqlParameterSource()
                        .addValue("imageId", imageId),
                imageDataRowMapper());
        if (data.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(data.getFirst());
    }

    public ResponseEntity<Void> addImage(byte[] data) {
        String imageId = jdbcTemplate.queryForObject(
                "SELECT MAKE_ID() ",
                new MapSqlParameterSource(),String.class);
        int count = jdbcTemplate.update(
                """
                        INSERT INTO Image (image_id, data) 
                        VALUES (:id, :data);""",
                new MapSqlParameterSource()
                        .addValue("id", imageId)
                        .addValue("data",data, Types.BLOB)
        );
        if (count==0) {
            return ResponseEntity.badRequest().build();
        }
        URI uri = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .pathSegment("cars/{carId}")
                .build(imageId);
        return ResponseEntity.created(uri).build();
    }

    private RowMapper<byte[]> imageDataRowMapper() {
        return (rs, rowNum) -> {
            try {
                return rs.getBlob("data").getBinaryStream().readAllBytes();
            } catch (IOException e) {
                return null;
            }
        };
    }
}