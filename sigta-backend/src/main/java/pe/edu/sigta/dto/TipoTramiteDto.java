package pe.edu.sigta.dto;

public record TipoTramiteDto(
        Integer id,
        String codigo,
        String nombre,
        boolean requiereEstablecimiento) {
}