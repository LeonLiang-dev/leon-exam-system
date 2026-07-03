package com.wts.exam.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;
import java.io.Serializable;

@Data
@TableName("wts_card_point")
public class ExamCardPoint implements Serializable {

    @TableId(type = IdType.INPUT)
    private String id;

    private String cardid;
    private String versionid;
    private Integer point;
    private Integer mpoint;
    private String complete;
    private String reviewRequired;
    private String reviewReason;
    private String reviewComment;
}
