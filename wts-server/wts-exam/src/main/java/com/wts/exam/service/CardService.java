package com.wts.exam.service;

import com.wts.common.result.PageResult;
import com.wts.exam.dto.CardSubmitDTO;
import com.wts.exam.dto.ExamPaperVO;
import com.wts.exam.dto.JudgeDTO;
import com.wts.exam.entity.ExamCard;
import com.wts.exam.entity.ExamCardAnswer;
import com.wts.exam.entity.ExamCardPoint;

import java.util.List;

public interface CardService {
    ExamCard enterRoom(String roomId, String userId, String userName, boolean roomAdmin);
    void saveAnswers(String cardId, CardSubmitDTO dto, String userId);
    ExamCard submit(String cardId, CardSubmitDTO dto, String userId);
    ExamCard getResult(String cardId);
    ExamCard getResult(String cardId, String userId, boolean roomAdmin);
    List<ExamCardAnswer> getCardAnswers(String cardId);
    List<ExamCardPoint> getCardPoints(String cardId);
    void judge(String cardId, JudgeDTO dto, String judgeUserId, String judgeUserName);
    void judgeBatch(List<String> cardIds, String judgeUserId, String judgeUserName);
    ExamPaperVO getExamPaper(String cardId, String userId);
    ExamPaperVO getExamPaperForReview(String cardId);
    PageResult<ExamCard> getRoomCards(String roomId, int page, int size);
}
