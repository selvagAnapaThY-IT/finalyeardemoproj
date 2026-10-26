package com.fintaxpro;
import java.util.Map;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;import org.springframework.web.bind.MethodArgumentNotValidException;import org.springframework.dao.DataIntegrityViolationException;import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice public class ApiErrors{
 @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<?> invalid(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(){return ResponseEntity.badRequest().body(Map.of("error","Check your name, email and password (10–72 characters)."));}
 @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> conflict(){return ResponseEntity.status(409).body(Map.of("error","A conflicting record already exists."));}
 @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("error",e.getReason()==null?"Request failed.":e.getReason()));}
 @ExceptionHandler(Exception.class) ResponseEntity<?> general(){return ResponseEntity.status(500).body(Map.of("error","Unable to complete the request. Please retry."));}
}
