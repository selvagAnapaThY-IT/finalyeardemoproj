package com.fintaxpro;
import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface UserRepository extends JpaRepository<AppUser,String>{Optional<AppUser> findByEmail(String email);}
