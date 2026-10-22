package ru.library.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegisterForm {

    @NotBlank(message = "Укажите имя и фамилию")
    private String fullName;

    @NotBlank(message = "Укажите почту")
    @Email(message = "Почта выглядит неверно")
    private String email;

    @Size(min = 6, message = "Пароль — не короче 6 символов")
    private String password;

    private String phone;
}
