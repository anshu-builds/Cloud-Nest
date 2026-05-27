package com.cloudnest.controller;

import com.cloudnest.dto.UserRegistrationDto;
import com.cloudnest.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * AuthController — Handles login, registration, and logout pages.
 *
 * @Controller (not @RestController) because we return Thymeleaf template names,
 * not JSON. Spring resolves template names to HTML files in /templates/.
 */
@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /login — Show the login page.
     * Spring Security handles the actual login POST processing.
     */
    @GetMapping("/login")
    public String showLoginPage() {
        return "login"; // Resolves to templates/login.html
    }

    /**
     * GET /register — Show the registration form.
     */
    @GetMapping("/register")
    public String showRegistrationPage(Model model) {
        // Add an empty DTO for the form to bind to
        model.addAttribute("user", new UserRegistrationDto());
        return "register"; // Resolves to templates/register.html
    }

    /**
     * POST /register — Process the registration form.
     *
     * @param dto           The form data bound to the DTO
     * @param bindingResult Contains validation errors (if any)
     */
    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") UserRegistrationDto dto,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        // If validation annotations (@NotBlank, @Email, etc.) found errors, re-show the form
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            // Attempt registration
            userService.registerUser(dto);
            // Success! Redirect to login with a success message
            redirectAttributes.addFlashAttribute("success",
                    "Registration successful! Please log in.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            // Registration failed (duplicate username/email, password mismatch)
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    /**
     * GET / — Redirect root URL to dashboard (if logged in) or login page.
     */
    @GetMapping("/")
    public String redirectToDashboard() {
        return "redirect:/dashboard";
    }
}
