package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.client.Cliente;
import com.proyecto.servicios.entity.client.Cuenta;
import com.proyecto.servicios.entity.client.Domicilio;
import com.proyecto.servicios.model.onboarding.ClienteConsultaRequestDto;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Construye la consulta dinámica de clientes a partir de los filtros recibidos.
 * Sólo se agregan a la consulta los filtros que vienen informados.
 */
public final class ClienteSpecifications {

    private ClienteSpecifications() {
    }

    public static Specification<Cliente> conFiltros(ClienteConsultaRequestDto filtros) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (filtros == null) {
                return cb.conjunction();
            }

            agregarParcial(predicados, cb, root.get("nombre"), filtros.getNombre());
            agregarParcial(predicados, cb, root.get("apellidoPaterno"), filtros.getApellidoPaterno());
            agregarParcial(predicados, cb, root.get("apellidoMaterno"), filtros.getApellidoMaterno());

            if (tieneTexto(filtros.getCurp())) {
                predicados.add(cb.equal(root.get("curp"), filtros.getCurp()));
            }
            if (tieneTexto(filtros.getRfc())) {
                predicados.add(cb.equal(root.get("rfc"), filtros.getRfc()));
            }
            if (tieneTexto(filtros.getCorreo())) {
                predicados.add(cb.equal(cb.lower(root.get("correo")), filtros.getCorreo().trim().toLowerCase()));
            }
            if (tieneTexto(filtros.getTelefonoMovil())) {
                predicados.add(cb.equal(root.get("telefonoMovil"), filtros.getTelefonoMovil()));
            }
            if (tieneTexto(filtros.getSexo())) {
                predicados.add(cb.equal(root.get("sexo"), filtros.getSexo()));
            }
            if (filtros.getActivo() != null) {
                predicados.add(cb.equal(root.get("activo"), filtros.getActivo()));
            }
            if (filtros.getIngresoMinimo() != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("ingresoMensual"), filtros.getIngresoMinimo()));
            }
            if (filtros.getIngresoMaximo() != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("ingresoMensual"), filtros.getIngresoMaximo()));
            }
            if (filtros.getFechaRegistroDesde() != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filtros.getFechaRegistroDesde().atStartOfDay()));
            }
            if (filtros.getFechaRegistroHasta() != null) {
                predicados.add(cb.lessThan(root.get("createdAt"), filtros.getFechaRegistroHasta().plusDays(1).atStartOfDay()));
            }

            if (tieneTexto(filtros.getMunicipio()) || tieneTexto(filtros.getEstado()) || tieneTexto(filtros.getCodigoPostal())) {
                Join<Cliente, Domicilio> domicilio = root.join("domicilio", JoinType.INNER);
                agregarParcial(predicados, cb, domicilio.get("municipio"), filtros.getMunicipio());
                agregarParcial(predicados, cb, domicilio.get("estado"), filtros.getEstado());
                if (tieneTexto(filtros.getCodigoPostal())) {
                    predicados.add(cb.equal(domicilio.get("codigoPostal"), filtros.getCodigoPostal()));
                }
            }

            if (tieneTexto(filtros.getEstatusCuenta())) {
                Join<Cliente, Cuenta> cuenta = root.join("cuenta", JoinType.INNER);
                predicados.add(cb.equal(cuenta.get("estatus"), filtros.getEstatusCuenta()));
            }

            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    private static void agregarParcial(List<Predicate> predicados,
                                       jakarta.persistence.criteria.CriteriaBuilder cb,
                                       jakarta.persistence.criteria.Expression<String> campo,
                                       String valor) {
        if (tieneTexto(valor)) {
            predicados.add(cb.like(cb.lower(campo), "%" + valor.trim().toLowerCase() + "%"));
        }
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
