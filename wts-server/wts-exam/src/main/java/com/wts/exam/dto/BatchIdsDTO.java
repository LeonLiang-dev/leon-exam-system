package com.wts.exam.dto;

import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.List;

@Data
public class BatchIdsDTO {
    private List<String> ids;

    public List<String> normalizedIds() {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }
}
