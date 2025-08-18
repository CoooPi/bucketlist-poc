package com.bucketlist.api;

import com.bucketlist.domain.Gender;
import com.bucketlist.domain.Mode;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateProfileRequest(
    @NotNull Gender gender,
    
    @Min(value = 18, message = "Age must be at least 18")
    @Max(value = 100, message = "Age must be at most 100")
    int age,
    
    @NotNull 
    @DecimalMin(value = "0.0", message = "Capital must be positive")
    BigDecimal capital,
    
    @NotNull Mode mode
) {}