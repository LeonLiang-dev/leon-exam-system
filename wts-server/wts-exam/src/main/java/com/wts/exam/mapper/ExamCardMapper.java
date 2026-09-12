package com.wts.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wts.exam.entity.ExamCard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExamCardMapper extends BaseMapper<ExamCard> {
    @Select("""
            SELECT COUNT(1)
            FROM wts_card c
            INNER JOIN wts_room r ON r.ID = c.ROOMID
            WHERE c.PSTATE IN ('16', '21')
            """)
    Long countFinishedCardsWithExistingRoom();

    @Select("""
            SELECT u.CLASS_NAME AS className, COUNT(1) AS cardCount, ROUND(AVG(c.POINT), 1) AS avgPoint
            FROM wts_card c
            INNER JOIN alone_auth_user u ON u.ID = c.USERID
            WHERE c.PSTATE IN ('16', '21')
              AND u.CLASS_NAME IS NOT NULL AND u.CLASS_NAME <> ''
            GROUP BY u.CLASS_NAME
            ORDER BY cardCount DESC
            LIMIT 20
            """)
    java.util.List<java.util.Map<String, Object>> countCardsByClass();
}
