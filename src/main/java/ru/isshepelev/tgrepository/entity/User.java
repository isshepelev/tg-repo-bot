package ru.isshepelev.tgrepository.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Data
@Table(name = "users")
@ToString
public class User {
    @Id
    private Long id;

    private String firstname;
    private String lastname;
    private String username;

    private LocalDate date;
}
