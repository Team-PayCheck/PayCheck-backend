package com.example.paycheck.domain.workplace.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NtsBusinessStatusRequest {

    @JsonProperty("b_no")
    private List<String> businessNumbers;
}
