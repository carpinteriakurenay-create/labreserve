package com.labreserve.dto;

import lombok.Data;

@Data
public class StudentUpdateRequest {
    private Long labId;
    private String name;
    private String gender;
    private Integer age;
    private String address;
}
