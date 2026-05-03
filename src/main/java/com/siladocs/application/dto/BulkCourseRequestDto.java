package com.siladocs.application.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO for bulk course upload.
 * Frontend sends full entity data so backend can find-or-create
 * Careers, Curriculums, and Courses automatically.
 */
@Getter
@Setter
public class BulkCourseRequestDto {

    private CarreraData carrera;
    private MallaData malla;
    private CursoData curso;

    @Getter
    @Setter
    public static class CarreraData {
        private String nombre;
        private String facultad;
        private Integer ciclos;
        private String estado;
    }

    @Getter
    @Setter
    public static class MallaData {
        private String nombre;
        private Integer año;
        private Integer numCursos;
        private Integer creditos;
        private String descripcion;
        private String estado;
    }

    @Getter
    @Setter
    public static class CursoData {
        private String codigo;
        private String nombre;
        private Integer ciclo;
        private Integer año;
        private String estado;
        private String fechaPublicacion;
    }
}
