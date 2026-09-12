package com.wts.exam.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;
import java.io.Serializable;

@Data
@TableName("wts_room")
public class ExamRoom implements Serializable {

    @TableId(type = IdType.INPUT)
    private String id;

    private String dusername;
    private String duser;
    private String dtime;
    private String pcontent;
    private String pstate;
    private String euser;
    private String eusername;
    private String cuser;
    private String cusername;
    private String etime;
    private String ctime;
    private String examtypeid;
    private String timetype;
    private String starttime;
    private String endtime;
    private String writetype;
    private String roomnote;
    private Integer timelen;
    private String counttype;
    private String name;
    private String restarttype;
    private String imgid;
    private String ssorttype;
    private String osorttype;
    private String pshowtype;
    private String uuid;
    private String resultstype;
    private String publictype;
    private String adjudgetype;
    private String picktype;
    private String type;
    private String statistical;
    private String typemodel;
    private String papervmodel;

    @TableField(exist = false)
    private String myCardId;

    @TableField(exist = false)
    private String myCardPstate;

    @TableField(exist = false)
    private Boolean resultAvailable;

    @TableField(exist = false)
    private String resultUnavailableReason;

    /** 该答题室已产生的答卷总数（非持久化，列表查询时汇总填充） */
    @TableField(exist = false)
    private Integer cardCount;
}
