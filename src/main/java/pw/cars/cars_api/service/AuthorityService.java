package pw.cars.cars_api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;
import pw.cars.cars_api.model.PostCustomerData;

import java.sql.SQLIntegrityConstraintViolationException;

@Repository
public class AuthorityService {
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    public ResponseEntity<Void> register(PostCustomerData customerData) {
        try {
            jdbcTemplate.update(
                    """
                            CALL REGISTER_CUSTOMER(:email, :username, :password, @cid);""",
                    new MapSqlParameterSource()
                            .addValue("email",customerData.email())
                            .addValue("username", customerData.username())
                            .addValue("password", customerData.password())
            );
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        var id = jdbcTemplate.queryForObject("SELECT @cid;",
                new MapSqlParameterSource(),
                String.class);
        if (id == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.noContent()
                .header("Set-Cookie",
                        String.format("customer-token=%s; Max-Age=36000",id))
                .build();
    }

    public ResponseEntity<Void> login(String username, String password) {
        var id = jdbcTemplate.queryForObject(
                """
                        SELECT GET_CUSTOMER_BY_CREDENTIAlS(:username, :password);""",
                new MapSqlParameterSource()
                        .addValue("username", username)
                        .addValue("password", password),
                String.class
        );
        if (id == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.noContent()
                .header("Set-Cookie",
                        String.format("customer-token=%s; Max-Age=36000",id))
                .build();
    }
}