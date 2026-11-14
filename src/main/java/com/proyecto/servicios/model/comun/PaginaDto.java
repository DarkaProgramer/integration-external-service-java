package com.proyecto.servicios.model.comun;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Página de resultados independiente de Spring Data (estructura estable para los frontends). */
@Getter
public class PaginaDto<T> {

    private final List<T> contenido;
    private final int pagina;
    private final int tamano;
    private final long totalElementos;
    private final int totalPaginas;

    public PaginaDto(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {
        this.contenido = contenido;
        this.pagina = pagina;
        this.tamano = tamano;
        this.totalElementos = totalElementos;
        this.totalPaginas = totalPaginas;
    }

    public static <E, T> PaginaDto<T> de(Page<E> page, Function<E, T> mapper) {
        return new PaginaDto<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
