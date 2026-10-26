package com.fintaxpro;
import jakarta.persistence.*;
@Entity @Table(name="users") public class AppUser{@Id String id;@Column(nullable=false,unique=true,length=254) String email;@Column(nullable=false) String name;@Column(nullable=false) String passwordHash;protected AppUser(){}AppUser(String id,String email,String name,String hash){this.id=id;this.email=email;this.name=name;this.passwordHash=hash;}}
