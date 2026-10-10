package com.proyecto.servicios.model.onboarding;

public class ClienteResponseDto {
    private String nombre;
    private String correo;
    private String telefono;
    private String curp;
    private String rfc;

    public ClienteResponseDto(String nombre, String correo, String telefono, String curp, String rfc) {
        this.nombre = nombre;
        this.correo = correo;
        this.telefono = telefono;
        this.curp = curp;
        this.rfc = rfc;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getCurp() { return curp; }
    public void setCurp(String curp) { this.curp = curp; }
    public String getRfc() { return rfc; }
    public void setRfc(String rfc) { this.rfc = rfc; }
}