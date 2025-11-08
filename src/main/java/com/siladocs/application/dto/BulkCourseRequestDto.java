package com.siladocs.application.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BulkCourseRequestDto {
    // Coincide con el payload que tu frontend envía
    private String carrera;
    private String malla;
    private String ciclo;
    private String curso;
}