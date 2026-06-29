package com.wts.exam.dto;

import lombok.Data;

@Data
public class RoomDTO {
    private String name;
    private String examtypeid;
    private String pstate;
    private String pshowtype;
    private String timetype;
    private String starttime;
    private String endtime;
    private Integer timelen;
    private String writetype;
    private String roomnote;
    private String counttype;
    private String restarttype;
    private String resultstype;
    private String adjudgetype;
    private String picktype;
    private String publictype;
    private String ssorttype;
    private String osorttype;
}
