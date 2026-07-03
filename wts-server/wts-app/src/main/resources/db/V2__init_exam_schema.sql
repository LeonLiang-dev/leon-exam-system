-- V2__init_exam_schema.sql
-- All 49 exam tables for WTS

-- ============================================================
-- Subject tables
-- ============================================================

CREATE TABLE IF NOT EXISTS wts_subject_type (
    ID          varchar(32) NOT NULL,
    TREECODE    varchar(256),
    COMMENTS    varchar(128),
    NAME        varchar(64),
    CTIME       varchar(14),
    UTIME       varchar(14),
    STATE       char(1),
    CUSER       varchar(32),
    MUSER       varchar(32),
    PARENTID    varchar(32),
    SORT        int,
    READPOP     char(1),
    WRITEPOP    char(1),
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_subject (
    ID            varchar(32) NOT NULL,
    TYPEID        varchar(32),
    VERSIONID     varchar(32),
    PSTATE        varchar(2),
    MATERIALID    varchar(32),
    PRAISENUM     int,
    COMMENTNUM    int,
    ANALYSISNUM   int,
    DONUM         int,
    RIGHTNUM      int,
    UUID          varchar(32),
    INTRODUCTION  varchar(512),
    LEVEL         int,
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_subject_version (
    ID          varchar(32) NOT NULL,
    CTIME       varchar(16),
    CUSERNAME   varchar(64),
    CUSER       varchar(32),
    PSTATE      varchar(2),
    PCONTENT    varchar(128),
    TIPSTR      varchar(256),
    TIPNOTE     text,
    TIPTYPE     varchar(2),
    SUBJECTID   varchar(32),
    ANSWERED    varchar(2),
    PRIMARY KEY (ID),
    KEY IDX_SUBJECT_VERSION_SUBJECTID (SUBJECTID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_subject_answer (
    ID          varchar(32) NOT NULL,
    VERSIONID   varchar(32),
    ANSWER      varchar(512),
    ANSWERNOTE  text,
    RIGHTANSWER varchar(2),
    SORT        int,
    POINTWEIGHT int,
    GROUPNO     int,
    UUID        varchar(32),
    PSTATE      varchar(2),
    PCONTENT    varchar(128),
    CUSER       varchar(32),
    CUSERNAME   varchar(64),
    CTIME       varchar(16),
    PRIMARY KEY (ID),
    KEY IDX_SUBJECT_ANSWER_VERSIONID (VERSIONID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_subject_analysis (
    ID          varchar(32) NOT NULL,
    CTIME       varchar(16),
    CUSERNAME   varchar(64),
    PSTATE      varchar(2),
    CUSER       varchar(32),
    PCONTENT    varchar(128),
    TEXT        text,
    SUBJECTID   varchar(32),
    PRIMARY KEY (ID),
    KEY IDX_SUBJECT_ANALYSIS_SUBJECTID (SUBJECTID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_subject_comment (
    ID          varchar(32) NOT NULL,
    CTIME       varchar(16),
    CUSERNAME   varchar(64),
    PSTATE      varchar(2),
    CUSER       varchar(32),
    PCONTENT    varchar(128),
    TEXT        text,
    SUBJECTID   varchar(32),
    PRIMARY KEY (ID),
    KEY IDX_SUBJECT_COMMENT_SUBJECTID (SUBJECTID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_subject_pop (
    ID          varchar(32) NOT NULL,
    TYPEID      varchar(32),
    USERNAME    varchar(64),
    USERID      varchar(32),
    FUNTYPE     varchar(2),
    PRIMARY KEY (ID),
    KEY IDX_SUBJECT_POP_TYPEID (TYPEID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_subject_userown (
    ID          varchar(32) NOT NULL,
    CTIME       varchar(16),
    CUSERNAME   varchar(64),
    PSTATE      varchar(2),
    CUSER       varchar(32),
    PCONTENT    varchar(128),
    SUBJECTID   varchar(32),
    CARDID      varchar(32),
    MODELTYPE   varchar(2),
    PRIMARY KEY (ID),
    KEY IDX_SUBJECT_USEROWN_CUSER_SUBJECTID (CUSER, SUBJECTID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================================
-- Material table
-- ============================================================

CREATE TABLE IF NOT EXISTS wts_material (
    ID       varchar(32) NOT NULL,
    UUID     varchar(32),
    TITLE    varchar(512),
    TEXT     text,
    PSTATE   varchar(2),
    CUSER    varchar(32),
    EUSER    varchar(32),
    CTIME    varchar(16),
    ETIME    varchar(16),
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================================
-- Exam type tables
-- ============================================================

CREATE TABLE IF NOT EXISTS wts_exam_type (
    ID          varchar(32) NOT NULL,
    TREECODE    varchar(256),
    COMMENTS    varchar(128),
    NAME        varchar(64),
    CTIME       varchar(14),
    UTIME       varchar(14),
    STATE       char(1),
    CUSER       varchar(32),
    MUSER       varchar(32),
    PARENTID    varchar(32),
    SORT        int,
    DOMAIN      varchar(16),
    MNGPOP      char(1),
    ADJUDGEPOP  char(1),
    QUERYPOP    char(1),
    SUPERPOP    char(1),
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_exam_pop (
    ID          varchar(32) NOT NULL,
    TYPEID      varchar(32),
    USERNAME    varchar(64),
    USERID      varchar(32),
    FUNTYPE     varchar(2),
    PRIMARY KEY (ID),
    KEY IDX_EXAM_POP_TYPEID (TYPEID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_exam_stat (
    ID           varchar(32) NOT NULL,
    CUSER        varchar(32),
    PAPERNUM     int,
    SUBJECTNUM   int,
    ERRORSUBNUM  int,
    TESTNUM      int,
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================================
-- Paper tables
-- ============================================================

CREATE TABLE IF NOT EXISTS wts_paper (
    ID           varchar(32) NOT NULL,
    EXAMTYPEID   varchar(32),
    CTIME        varchar(16),
    ETIME        varchar(16),
    CUSERNAME    varchar(64),
    CUSER        varchar(32),
    EUSERNAME    varchar(64),
    EUSER        varchar(32),
    PSTATE       varchar(2),
    PCONTENT     varchar(128),
    NAME         varchar(128),
    SUBJECTNUM   int,
    POINTNUM     int,
    COMPLETETNUM int,
    AVGPOINT     int,
    TOPPOINT     int,
    LOWPOINT     int,
    ADVICETIME   int,
    PAPERNOTE    text,
    BOOKNUM      int,
    UUID         varchar(32),
    KNOWID       varchar(32),
    PRIMARY KEY (ID),
    KEY IDX_PAPER_EXAMTYPEID (EXAMTYPEID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_paper_chapter (
    ID             varchar(32) NOT NULL,
    STYPE          varchar(2),
    PTYPE          varchar(2),
    INITPOINT      int,
    SUBJECTTYPEID  varchar(32),
    SUBJECTNUM     int,
    SUBJECTPOINT   int,
    NAME           varchar(64),
    TEXTNOTE        text,
    PARENTID       varchar(32),
    PAPERID        varchar(32),
    SORT           int,
    TREECODE       varchar(256),
    PRIMARY KEY (ID),
    KEY IDX_PAPER_CHAPTER_PAPERID (PAPERID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_paper_subject (
    ID          varchar(32) NOT NULL,
    VERSIONID   varchar(32),
    SUBJECTID   varchar(32),
    CHAPTERID   varchar(32),
    SORT        int,
    POINT       int,
    PAPERID     varchar(32),
    PRIMARY KEY (ID),
    KEY IDX_PAPER_SUBJECT_VERSIONID (VERSIONID),
    KEY IDX_PAPER_SUBJECT_PAPERID_SUBJECTID (PAPERID, SUBJECTID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_paper_userown (
    ID          varchar(32) NOT NULL,
    CTIME       varchar(16),
    CUSERNAME   varchar(64),
    PSTATE      varchar(2),
    CUSER       varchar(32),
    PCONTENT    varchar(128),
    PAPERID     varchar(32),
    PAPERNAME   varchar(128),
    ROOMID      varchar(32),
    ROOMNAME    varchar(64),
    CARDID      varchar(32),
    MODELTYPE   varchar(2),
    SCORE       float,
    RPCENT      float,
    PRIMARY KEY (ID),
    KEY IDX_PAPER_USEROWN_CUSER (CUSER)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================================
-- Room tables
-- ============================================================

CREATE TABLE IF NOT EXISTS wts_room (
    ID            varchar(32) NOT NULL,
    DUSERNAME     varchar(32),
    DUSER         varchar(32),
    DTIME         varchar(14),
    PCONTENT      varchar(128),
    PSTATE        varchar(2),
    EUSER         varchar(32),
    EUSERNAME     varchar(64),
    CUSER         varchar(32),
    CUSERNAME     varchar(64),
    ETIME         varchar(16),
    CTIME         varchar(16),
    EXAMTYPEID    varchar(32),
    TIMETYPE      varchar(2),
    STARTTIME     varchar(16),
    ENDTIME       varchar(16),
    WRITETYPE     varchar(2),
    ROOMNOTE      text,
    TIMELEN       int,
    COUNTTYPE     varchar(2),
    NAME          varchar(64),
    RESTARTTYPE   varchar(2),
    IMGID         varchar(32),
    SSORTTYPE     varchar(2),
    OSORTTYPE     varchar(2),
    PSHOWTYPE     varchar(2),
    UUID          varchar(32),
    RESULTSTYPE   varchar(2),
    PUBLICTYPE    varchar(2),
    ADJUDGETYPE   varchar(2),
    PICKTYPE      varchar(2),
    TYPE          varchar(2),
    STATISTICAL   varchar(2),
    TYPEMODEL     varchar(2),
    PAPERVMODEL   varchar(2),
    PRIMARY KEY (ID),
    KEY IDX_ROOM_EXAMTYPEID (EXAMTYPEID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_room_paper (
    ID          varchar(32) NOT NULL,
    ROOMID      varchar(32),
    PAPERID     varchar(32),
    NAME        varchar(128),
    PASSPOINT   float,
    PRIMARY KEY (ID),
    KEY IDX_ROOM_PAPER_ROOMID (ROOMID),
    KEY IDX_ROOM_PAPER_PAPERID (PAPERID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_room_user (
    ID          varchar(32) NOT NULL,
    USERID      varchar(32),
    ROOMID      varchar(32),
    GROUPID     varchar(32),
    PRIMARY KEY (ID),
    KEY IDX_ROOM_USER_ROOMID (ROOMID),
    KEY IDX_ROOM_USER_USERID (USERID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_room_usergroup (
    ID          varchar(32) NOT NULL,
    ROOMID      varchar(32),
    OBJID       varchar(32),
    OBJTYPE     varchar(2),
    OBJNAME     varchar(64),
    PRIMARY KEY (ID),
    KEY IDX_ROOM_USERGROUP_ROOMID (ROOMID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_room_scoreeval (
    ID          varchar(32) NOT NULL,
    ROOMID      varchar(32),
    PAPERID     varchar(32),
    NAME        varchar(64),
    NOTE        varchar(128),
    DESCRIBES   text,
    POINTS      int,
    POINTE      int,
    PRIMARY KEY (ID),
    KEY IDX_ROOM_SCOREEVAL_ROOMID (ROOMID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================================
-- Card tables
-- ============================================================

CREATE TABLE IF NOT EXISTS wts_card (
    ID                varchar(32) NOT NULL,
    PAPERID           varchar(32),
    ROOMID            varchar(32),
    USERID            varchar(32),
    POINT             float,
    ADJUDGEUSERNAME   varchar(64),
    ADJUDGEUSER       varchar(32),
    ADJUDGETIME       varchar(16),
    STARTTIME         varchar(16),
    ENDTIME           varchar(16),
    PSTATE            varchar(2),
    PCONTENT          varchar(256),
    COMPLETENUM       int,
    ALLNUM            int,
    OVERTIME          varchar(2),
    RESULTSTYPE       varchar(2),
    SUBMITTIME        varchar(16),
    ROOMUUID          varchar(32),
    PAPERUUID         varchar(32),
    USERUUID          varchar(32),
    ADJUDGEUSERUUID   varchar(32),
    STATISTICAL       varchar(2),
    PRIMARY KEY (ID),
    KEY IDX_CARD_PAPERID_USERID_ROOMID (PAPERID, USERID, ROOMID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_card_answer (
    ID          varchar(32) NOT NULL,
    CARDID      varchar(32),
    ANSWERID    varchar(32),
    VERSIONID   varchar(32),
    CUSER       varchar(32),
    VALSTR      mediumtext,
    CTIME       varchar(16),
    PCONTENT    varchar(256),
    PSTATE      varchar(2),
    POINT       int DEFAULT 0,
    MPOINT      int DEFAULT 0,
    REVIEW_REQUIRED varchar(1) DEFAULT '0',
    REVIEW_REASON   varchar(64) DEFAULT '',
    REVIEW_COMMENT  varchar(512) DEFAULT '',
    PRIMARY KEY (ID),
    KEY IDX_CARD_ANSWER_CARDID_VERSIONID_ANSWERID (CARDID, VERSIONID, ANSWERID),
    KEY IDX_CARD_ANSWER_CARDID_VERSIONID (CARDID, VERSIONID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_card_point (
    ID          varchar(32) NOT NULL,
    CARDID      varchar(32),
    VERSIONID   varchar(32),
    POINT       int,
    MPOINT      int,
    COMPLETE    varchar(1),
    REVIEW_REQUIRED varchar(1) DEFAULT '0',
    REVIEW_REASON   varchar(64) DEFAULT '',
    REVIEW_COMMENT  varchar(512) DEFAULT '',
    PRIMARY KEY (ID),
    KEY IDX_CARD_POINT_CARDID (CARDID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================================
-- History tables
-- ============================================================

CREATE TABLE IF NOT EXISTS wts_his_card (
    ID              varchar(32) NOT NULL,
    BACKVERSION     varchar(16),
    USERNAME        varchar(64),
    ROOMNAME        varchar(64),
    PAPERNAME       varchar(128),
    USERUUID        varchar(32),
    ROOMUUID        varchar(32),
    PAPERUUID       varchar(32),
    PSTATE          varchar(2),
    ALLPOINT        float,
    ALLTIME         varchar(32),
    RESULTSTYPE     varchar(2),
    CUSER           varchar(32),
    CTIME           varchar(16),
    COMPLETENUM     int,
    ALLNUM          int,
    STATISTICAL     varchar(2),
    PRIMARY KEY (ID),
    KEY IDX_HIS_CARD_BACKVERSION (BACKVERSION),
    KEY IDX_HIS_CARD_USERUUID (USERUUID),
    KEY IDX_HIS_CARD_ROOMUUID (ROOMUUID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_his_card_answer (
    ID              varchar(32) NOT NULL,
    BACKVERSION     varchar(16),
    CARDID          varchar(32),
    SUBJECTUUID     varchar(32),
    ANSWERUUID      varchar(32),
    VALSTR          text,
    CUSER           varchar(32),
    CTIME           varchar(16),
    PSTATE          varchar(2),
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_his_card_point (
    ID              varchar(32) NOT NULL,
    BACKVERSION     varchar(16),
    CARDID          varchar(32),
    SUBJECTUUID     varchar(32),
    VERSIONID       varchar(32),
    POINT           int,
    MPOINT          int,
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_his_subject (
    ID              varchar(32) NOT NULL,
    BACKVERSION     varchar(16),
    TITLE           varchar(512),
    TYPE            varchar(2),
    SUBJECTUUID     varchar(32),
    PAPERUUID       varchar(32),
    ROOMUUID        varchar(32),
    SORT            int,
    POINT           int,
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_his_answer (
    ID              varchar(32) NOT NULL,
    BACKVERSION     varchar(16),
    ANSWERUUID      varchar(32),
    SUBJECTUUID     varchar(32),
    PAPERUUID       varchar(32),
    ROOMUUID        varchar(32),
    TITLE           varchar(512),
    RIGHTANSWER     varchar(2),
    SORT            int,
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_his_user (
    ID              varchar(32) NOT NULL,
    BACKVERSION     varchar(16),
    USERUUID        varchar(32),
    NAME            varchar(64),
    LOGINNAME       varchar(64),
    ORGUUID         varchar(32),
    ORGNAME         varchar(128),
    POSTUUID        varchar(32),
    POSTNAMES       varchar(256),
    ROOMUUID        varchar(32),
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================================
-- Random exam tables
-- ============================================================

CREATE TABLE IF NOT EXISTS wts_random_item (
    ID       varchar(32) NOT NULL,
    NAME     varchar(128),
    CUSER    varchar(32),
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_random_step (
    ID          varchar(32) NOT NULL,
    ITEMID      varchar(32),
    NAME        varchar(128),
    SORT        int,
    SUBNUM      int,
    SUBPOINT    int,
    TIPTYPE     varchar(2),
    TYPEID      varchar(32),
    KNOWID      varchar(32),
    PRIMARY KEY (ID),
    KEY IDX_RANDOM_STEP_ITEMID (ITEMID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================================
-- Comment/Log tables
-- ============================================================

CREATE TABLE IF NOT EXISTS wts_comment (
    ID          varchar(32) NOT NULL,
    BANDID      varchar(32),
    TYPE        varchar(2),
    TEXT        text,
    CUSER       varchar(32),
    CUSERNAME   varchar(64),
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_log_point (
    ID          varchar(32) NOT NULL,
    USERID      varchar(32),
    PAPERID     varchar(32),
    PAPERNAME   varchar(128),
    ROOMID      varchar(32),
    ROOMNAME    varchar(64),
    POINT       float,
    CTIME       varchar(16),
    PRIMARY KEY (ID),
    KEY IDX_LOG_POINT_USERID (USERID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================================
-- Document tables
-- ============================================================

CREATE TABLE IF NOT EXISTS wts_docfile (
    ID          varchar(32) NOT NULL,
    UUID        varchar(32),
    NAME        varchar(256),
    SIZE        bigint,
    TYPE        varchar(64),
    EXT         varchar(16),
    STOREPATH   varchar(512),
    CUSER       varchar(32),
    CUSERNAME   varchar(64),
    CTIME       varchar(16),
    PSTATE      varchar(2),
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_docfile_text (
    ID       varchar(32) NOT NULL,
    DOCID    varchar(32),
    TEXT     longtext,
    CTIME    varchar(16),
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================================
-- Knowledge tables
-- ============================================================

CREATE TABLE IF NOT EXISTS wts_know (
    ID          varchar(32) NOT NULL,
    NAME        varchar(64),
    PARENTID    varchar(32),
    TREECODE    varchar(256),
    COMMENTS    varchar(128),
    SORT        int,
    STATE       varchar(1),
    CUSER       varchar(32),
    MUSER       varchar(32),
    CTIME       varchar(14),
    UTIME       varchar(14),
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_know_resource (
    ID            varchar(32) NOT NULL,
    KNOWID        varchar(32),
    RESOURCEID    varchar(32),
    RESOURCETYPE  varchar(2),
    CUSER         varchar(32),
    CTIME         varchar(16),
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_know_subject (
    ID          varchar(32) NOT NULL,
    KNOWID      varchar(32),
    SUBJECTID   varchar(32),
    CUSER       varchar(32),
    CTIME       varchar(16),
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_know_cardsta (
    ID          varchar(32) NOT NULL,
    KNOWID      varchar(32),
    CARDID      varchar(32),
    ALLNUM      int,
    RIGHTNUM    int,
    CUSER       varchar(32),
    CTIME       varchar(16),
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_know_monthsta (
    ID          varchar(32) NOT NULL,
    KNOWID      varchar(32),
    MONTHS      varchar(6),
    ALLNUM      int,
    RIGHTNUM    int,
    CUSER       varchar(32),
    CTIME       varchar(16),
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================================
-- Statistical tables
-- ============================================================

CREATE TABLE IF NOT EXISTS wts_statistical_backup (
    ID          varchar(32) NOT NULL,
    ROOMID      varchar(32),
    PAPERID     varchar(32),
    USERID      varchar(32),
    CARDID      varchar(32),
    POINT       float,
    COMPLETENUM int,
    ALLNUM      int,
    CTIME       varchar(16),
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_statistical_card (
    ID       varchar(32) NOT NULL,
    CARDID   varchar(32),
    KEY1     varchar(1000),
    VAL1     text,
    CTIME    varchar(16),
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_statistical_room (
    ID       varchar(32) NOT NULL,
    ROOMID   varchar(32),
    KEY1     varchar(1000),
    VAL1     text,
    CTIME    varchar(16),
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================================
-- Sharing tables
-- ============================================================

CREATE TABLE IF NOT EXISTS wts_share_node (
    ID       varchar(32) NOT NULL,
    NAME     varchar(64),
    URL      varchar(256),
    TOKEN    varchar(128),
    STATE    varchar(1),
    CUSER    varchar(32),
    CTIME    varchar(16),
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_share_room (
    ID       varchar(32) NOT NULL,
    NODEID   varchar(32),
    ROOMID   varchar(32),
    STATE    varchar(1),
    CUSER    varchar(32),
    CTIME    varchar(16),
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_share_user (
    ID       varchar(32) NOT NULL,
    NODEID   varchar(32),
    USERID   varchar(32),
    STATE    varchar(1),
    CUSER    varchar(32),
    CTIME    varchar(16),
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================================
-- Other tables
-- ============================================================

CREATE TABLE IF NOT EXISTS wts_node_card (
    ID       varchar(32) NOT NULL,
    NODEID   varchar(32),
    CARDID   varchar(32),
    STATE    varchar(1),
    CUSER    varchar(32),
    CTIME    varchar(16),
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS wts_task_mirror (
    ID        varchar(32) NOT NULL,
    CTIME     varchar(16),
    PSTATE    varchar(2),
    PCONTENT  varchar(128),
    CARDID    varchar(32),
    VERSIONID varchar(32),
    ANSWERID  varchar(32),
    VAL       text,
    TYPE      varchar(2),
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
