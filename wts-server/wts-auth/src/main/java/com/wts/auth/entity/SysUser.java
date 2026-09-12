package com.wts.auth.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;
import java.io.Serializable;

@Data
@TableName("alone_auth_user")
public class SysUser implements Serializable {

    @TableId(type = IdType.INPUT)
    private String id;

    private String name;
    private String loginname;
    private String password;
    private String comments;
    /** 用户类型: 1系统用户, 2其他, 3超级管理员 */
    private String type;
    /** 职位: student/teacher/director/deputy/platform_admin */
    private String post;
    /** 权限点（逗号分隔），platform_admin 为空 = 全部权限 */
    private String perms;
    /** 班级标签（学生导入时记录，仅显示/筛选用） */
    private String className;
    private String ctime;
    private String utime;
    private String cuser;
    private String muser;
    /** 状态: 1正常, 0禁用 */
    private String state;
    private String logintime;
    private String imgid;
    private String uuid;

    @TableField(exist = false)
    private String ip;
}
