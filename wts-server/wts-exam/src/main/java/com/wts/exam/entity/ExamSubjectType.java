package com.wts.exam.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
@TableName("wts_subject_type")
public class ExamSubjectType implements Serializable {

    @TableId(type = IdType.INPUT)
    private String id;

    private String treecode;
    private String comments;
    private String name;
    private String ctime;
    private String utime;
    private String state;
    private String cuser;
    private String muser;
    private String parentid;
    private Integer sort;
    private String readpop;
    private String writepop;

    @TableField(exist = false)
    private List<ExamSubjectType> children;

    /** 创建人所属教研室名称（非表字段，树查询时填充） */
    @TableField(exist = false)
    private String orgName;
}
