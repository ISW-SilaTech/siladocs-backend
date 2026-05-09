package com.siladocs.infrastructure.web.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InstitutionConfigDto {

    private String name;
    private String domain;
    private String email;
    private String phone;
    private String address;
}
