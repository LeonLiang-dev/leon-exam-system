package com.wts.exam.service;

import com.wts.common.result.PageResult;
import com.wts.exam.dto.RoomDTO;
import com.wts.exam.entity.ExamRoom;
import com.wts.exam.entity.ExamRoomPaper;
import com.wts.exam.entity.ExamRoomUser;

import java.util.List;

public interface RoomService {
    PageResult<ExamRoom> list(int page, int size, String keyword, String pstate);
    PageResult<ExamRoom> listMyRooms(int page, int size, String userId, String keyword, String pstate);
    ExamRoom getDetail(String id);
    ExamRoom create(RoomDTO dto, String operatorId, String operatorName);
    ExamRoom update(String id, RoomDTO dto, String operatorId);
    void delete(String id, String operatorId);
    void deleteBatch(List<String> ids, String operatorId);
    void publish(String id, String operatorId);
    void publishBatch(List<String> ids, String operatorId);
    void close(String id, String operatorId);
    void closeBatch(List<String> ids, String operatorId);
    void addPaper(String roomId, String paperId, String name, Float passPoint);
    void removePaper(String roomId, String paperId);
    List<ExamRoomPaper> getRoomPapers(String roomId);
    List<ExamRoomUser> getAssignedUsers(String roomId);
    void assignUsers(String roomId, List<String> userIds);
}
