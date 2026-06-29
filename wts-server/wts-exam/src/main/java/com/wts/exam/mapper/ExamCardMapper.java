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
}
