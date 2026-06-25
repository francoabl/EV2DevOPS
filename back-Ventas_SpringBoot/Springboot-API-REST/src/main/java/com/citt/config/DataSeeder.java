package com.citt.config;

import com.citt.persistence.entity.Venta;
import com.citt.persistence.repository.VentaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

/**
 * Carga ordenes de compra de ejemplo si la tabla esta vacia.
 * Replica los datos historicos de db.json para que la aplicacion
 * sea funcional sobre una base de datos nueva (local o en AWS).
 */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedVentas(VentaRepository ventaRepository) {
        return args -> {
            if (ventaRepository.count() > 0) {
                return;
            }
            ventaRepository.save(Venta.builder()
                    .direccionCompra("P Sherman Calle Wallabi 42 Sydney")
                    .valorCompra(22990)
                    .fechaCompra(LocalDate.of(2024, 2, 2))
                    .despachoGenerado(false)
                    .build());
            ventaRepository.save(Venta.builder()
                    .direccionCompra("Avenida siempre viva 69")
                    .valorCompra(12590)
                    .fechaCompra(LocalDate.of(2024, 3, 5))
                    .despachoGenerado(false)
                    .build());
            ventaRepository.save(Venta.builder()
                    .direccionCompra("Avenida Por atras 1313")
                    .valorCompra(13990)
                    .fechaCompra(LocalDate.of(2024, 4, 20))
                    .despachoGenerado(false)
                    .build());
            ventaRepository.save(Venta.builder()
                    .direccionCompra("Calle presidente kirby 8528")
                    .valorCompra(9990)
                    .fechaCompra(LocalDate.of(2024, 4, 15))
                    .despachoGenerado(false)
                    .build());
        };
    }
}
