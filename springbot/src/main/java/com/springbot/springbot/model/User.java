package com.springbot.springbot.model;

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;

@Data
@Entity(name = "usersDataTable")
public class User {
    @Id
    private Long chatId;

    private String firstName;
    private String lastName;
    private String userName;
    private Timestamp registerAt;

    @Override
    public String toString() {
        return "User{" + 
                "chatId=" + chatId + 
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", userName='" + userName + '\'' +
                ", registerAt=" + registerAt +
                '}';
    }
}
