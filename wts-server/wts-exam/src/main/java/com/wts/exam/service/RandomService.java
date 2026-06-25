package com.wts.exam.service;

import com.wts.exam.dto.RandomItemDTO;
import com.wts.exam.entity.ExamRandomItem;
import com.wts.exam.entity.ExamRandomStep;

import java.util.List;

public interface RandomService {
    List<ExamRandomItem> listItems();
    ExamRandomItem createItem(RandomItemDTO dto, String operatorId);
    ExamRandomItem updateItem(String id, RandomItemDTO dto);
    void deleteItem(String id);
    void deleteItemsBatch(List<String> ids);
    List<ExamRandomStep> getSteps(String itemId);
    ExamRandomStep addStep(String itemId, RandomItemDTO.RandomStepDTO dto);
    ExamRandomStep updateStep(String id, RandomItemDTO.RandomStepDTO dto);
    void deleteStep(String id);
    void deleteStepsBatch(List<String> ids);
    /** Generate N random papers based on the item's steps. Returns list of created paper IDs. */
    List<String> generatePapers(String itemId, int count, String operatorId);
}
