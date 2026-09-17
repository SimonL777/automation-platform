package io.github.simonl777.common;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
@RestControllerAdvice
public class Errors {
 @ExceptionHandler(ApiFailure.class) public ResponseEntity<?> api(ApiFailure e){return ResponseEntity.status(e.status).body(Map.of("error",e.code));}
 @ExceptionHandler({IllegalArgumentException.class,MethodArgumentNotValidException.class,org.springframework.http.converter.HttpMessageNotReadableException.class})
 public ResponseEntity<?> invalid(Exception e){return ResponseEntity.badRequest().body(Map.of("error","INVALID_REQUEST"));}
}
