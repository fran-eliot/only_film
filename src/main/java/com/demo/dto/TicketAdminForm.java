package com.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketAdminForm {
    @NotNull
    private Long id;

    @NotNull
    @DecimalMin(value = "0.0", message = "El descuento no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "Usa como máximo dos decimales")
    private Double discount;
}
