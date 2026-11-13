package com.proyecto.servicios.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UrlBaseDatos - URLs de Render, Railway y JDBC")
class UrlBaseDatosTest {

    @Test
    void render_postgresConCredenciales() {
        UrlBaseDatos url = UrlBaseDatos.desde("postgresql://usr:p%40ss@dpg-abc-a:5432/banco", null, null);

        assertThat(url.jdbcUrl()).isEqualTo("jdbc:postgresql://dpg-abc-a:5432/banco");
        assertThat(url.usuario()).isEqualTo("usr");
        assertThat(url.clave()).isEqualTo("p@ss");
        assertThat(url.toString()).doesNotContain("p@ss").doesNotContain("usr");
    }

    @Test
    void heroku_postgresSinPuertoConParametros() {
        UrlBaseDatos url = UrlBaseDatos.desde("postgres://u:c@host/db?sslmode=require", null, null);

        assertThat(url.jdbcUrl()).isEqualTo("jdbc:postgresql://host/db?sslmode=require");
    }

    @Test
    void railway_mysql() {
        UrlBaseDatos url = UrlBaseDatos.desde("mysql://root:secreto@containers.railway.app:6543/railway", "x", "y");

        assertThat(url.jdbcUrl()).isEqualTo("jdbc:mysql://containers.railway.app:6543/railway");
        assertThat(url.usuario()).isEqualTo("root");
        assertThat(url.clave()).isEqualTo("secreto");
    }

    @Test
    void jdbc_seUsaTalCualConCredencialesPorSeparado() {
        UrlBaseDatos url = UrlBaseDatos.desde("jdbc:mysql://127.0.0.1:3306/db", "root", "");

        assertThat(url.jdbcUrl()).isEqualTo("jdbc:mysql://127.0.0.1:3306/db");
        assertThat(url.usuario()).isEqualTo("root");
    }

    @Test
    void esquemaDesconocido_falla() {
        assertThatThrownBy(() -> UrlBaseDatos.desde("oracle://h/db", null, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
