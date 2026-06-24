-- Empty schema for WTS / LeonExam.
-- Generated from wts.v1.4.1.sql with INSERT data removed.

-- --------------------------------------------------------
-- 主机:                           127.0.0.1
-- 服务器版本:                        5.7.25-log - MySQL Community Server (GPL)
-- 服务器操作系统:                      Win64
-- HeidiSQL 版本:                  9.4.0.5125
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

-- 导出  表 wts-trunk.alone_applog 结构
CREATE TABLE IF NOT EXISTS `alone_applog` (
  `ID` varchar(32) NOT NULL,
  `CTIME` varchar(16) NOT NULL,
  `DESCRIBES` varchar(1024) NOT NULL,
  `APPUSER` varchar(32) NOT NULL,
  `LEVELS` varchar(32) DEFAULT NULL,
  `METHOD` varchar(128) DEFAULT NULL,
  `CLASSNAME` varchar(128) DEFAULT NULL,
  `IP` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_Reference_9` (`APPUSER`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.alone_applog 的数据：0 rows
/*!40000 ALTER TABLE `alone_applog` DISABLE KEYS */;
/*!40000 ALTER TABLE `alone_applog` ENABLE KEYS */;

-- 导出  表 wts-trunk.alone_app_version 结构
CREATE TABLE IF NOT EXISTS `alone_app_version` (
  `version` varchar(32) NOT NULL,
  `update_time` varchar(32) DEFAULT NULL,
  `update_user` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.alone_app_version 的数据：~25 rows (大约)
/*!40000 ALTER TABLE `alone_app_version` DISABLE KEYS */;
/*!40000 ALTER TABLE `alone_app_version` ENABLE KEYS */;

-- 导出  表 wts-trunk.alone_auth_action 结构
CREATE TABLE IF NOT EXISTS `alone_auth_action` (
  `ID` varchar(32) NOT NULL,
  `AUTHKEY` varchar(128) NOT NULL,
  `NAME` varchar(64) NOT NULL,
  `COMMENTS` varchar(128) DEFAULT NULL,
  `CTIME` varchar(14) NOT NULL,
  `UTIME` varchar(14) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `MUSER` varchar(32) NOT NULL,
  `STATE` char(1) NOT NULL,
  `CHECKIS` char(1) NOT NULL,
  `LOGINIS` char(1) NOT NULL COMMENT '默认所有ACTION都要登录',
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='InnoDB free: 12288 kB alone_action';

-- 正在导出表  wts-trunk.alone_auth_action 的数据：66 rows
/*!40000 ALTER TABLE `alone_auth_action` DISABLE KEYS */;
/*!40000 ALTER TABLE `alone_auth_action` ENABLE KEYS */;

-- 导出  表 wts-trunk.alone_auth_actiontree 结构
CREATE TABLE IF NOT EXISTS `alone_auth_actiontree` (
  `ID` varchar(32) NOT NULL,
  `SORT` int(11) NOT NULL,
  `PARENTID` varchar(32) NOT NULL,
  `NAME` varchar(64) NOT NULL,
  `TREECODE` varchar(256) NOT NULL,
  `COMMENTS` varchar(128) DEFAULT NULL,
  `TYPE` char(1) NOT NULL COMMENT '分类、菜单、权限',
  `CTIME` varchar(14) NOT NULL,
  `UTIME` varchar(14) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `UUSER` varchar(32) NOT NULL,
  `STATE` char(1) NOT NULL,
  `ACTIONID` varchar(32) DEFAULT NULL,
  `DOMAIN` varchar(64) NOT NULL,
  `ICON` varchar(64) DEFAULT NULL,
  `IMGID` varchar(32) DEFAULT NULL,
  `PARAMS` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_Reference_7` (`ACTIONID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='InnoDB free: 12288 kB; (`action`) REFER `alone/alone_action`';

-- 正在导出表  wts-trunk.alone_auth_actiontree 的数据：47 rows
/*!40000 ALTER TABLE `alone_auth_actiontree` DISABLE KEYS */;
/*!40000 ALTER TABLE `alone_auth_actiontree` ENABLE KEYS */;

-- 导出  表 wts-trunk.alone_auth_organization 结构
CREATE TABLE IF NOT EXISTS `alone_auth_organization` (
  `ID` varchar(32) NOT NULL,
  `TREECODE` varchar(256) NOT NULL,
  `COMMENTS` varchar(128) DEFAULT NULL,
  `NAME` varchar(64) NOT NULL,
  `CTIME` varchar(14) NOT NULL,
  `UTIME` varchar(14) NOT NULL,
  `STATE` char(1) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `MUSER` varchar(32) NOT NULL,
  `PARENTID` varchar(32) DEFAULT NULL,
  `SORT` int(11) DEFAULT NULL,
  `TYPE` char(1) NOT NULL COMMENT '组织类型：1科室、2班组、3队组、0其他',
  `APPID` varchar(32) DEFAULT NULL,
  `UUID` varchar(32) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='组织类型：科室、班组、队组、其他';

-- 正在导出表  wts-trunk.alone_auth_organization 的数据：25 rows
/*!40000 ALTER TABLE `alone_auth_organization` DISABLE KEYS */;
/*!40000 ALTER TABLE `alone_auth_organization` ENABLE KEYS */;

-- 导出  表 wts-trunk.alone_auth_outuser 结构
CREATE TABLE IF NOT EXISTS `alone_auth_outuser` (
  `ID` varchar(32) NOT NULL,
  `CTIME` varchar(16) NOT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `PCONTENT` varchar(128) DEFAULT NULL,
  `USERID` varchar(32) DEFAULT NULL,
  `ACCOUNTID` varchar(64) NOT NULL,
  `ACCOUNTNAME` varchar(64) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_Reference_54` (`USERID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.alone_auth_outuser 的数据：3 rows
/*!40000 ALTER TABLE `alone_auth_outuser` DISABLE KEYS */;
/*!40000 ALTER TABLE `alone_auth_outuser` ENABLE KEYS */;

-- 导出  表 wts-trunk.alone_auth_pop 结构
CREATE TABLE IF NOT EXISTS `alone_auth_pop` (
  `ID` varchar(32) NOT NULL,
  `POPTYPE` varchar(1) NOT NULL COMMENT '1人、2组织机构、3岗位',
  `OID` varchar(32) NOT NULL COMMENT '人ID、组织机构ID、岗位ID',
  `ONAME` varchar(128) NOT NULL COMMENT '人NAME、组织机构NAME、岗位NAME',
  `TARGETTYPE` varchar(64) NOT NULL COMMENT '权限业务类型',
  `TARGETID` varchar(32) NOT NULL COMMENT '权限业务ID',
  `TARGETNAME` varchar(128) DEFAULT NULL,
  `CTIME` varchar(16) NOT NULL,
  `CUSERNAME` varchar(64) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `PCONTENT` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.alone_auth_pop 的数据：~0 rows (大约)
/*!40000 ALTER TABLE `alone_auth_pop` DISABLE KEYS */;
/*!40000 ALTER TABLE `alone_auth_pop` ENABLE KEYS */;

-- 导出  表 wts-trunk.alone_auth_post 结构
CREATE TABLE IF NOT EXISTS `alone_auth_post` (
  `ID` varchar(32) NOT NULL,
  `CTIME` varchar(16) NOT NULL,
  `ETIME` varchar(16) NOT NULL,
  `CUSERNAME` varchar(64) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `EUSERNAME` varchar(64) NOT NULL,
  `EUSER` varchar(32) NOT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `ORGANIZATIONID` varchar(32) DEFAULT NULL,
  `NAME` varchar(64) NOT NULL,
  `EXTENDIS` varchar(2) NOT NULL COMMENT '0:否1:是（默认否）',
  `UUID` varchar(32) NOT NULL,
  `SOURCEID` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_Reference_51` (`ORGANIZATIONID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.alone_auth_post 的数据：0 rows
/*!40000 ALTER TABLE `alone_auth_post` DISABLE KEYS */;
/*!40000 ALTER TABLE `alone_auth_post` ENABLE KEYS */;

-- 导出  表 wts-trunk.alone_auth_postaction 结构
CREATE TABLE IF NOT EXISTS `alone_auth_postaction` (
  `ID` varchar(32) NOT NULL,
  `MENUID` varchar(32) NOT NULL,
  `POSTID` varchar(32) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_Reference_8` (`MENUID`),
  KEY `FK_Reference_9` (`POSTID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='InnoDB free: 12288 kB; (`actionid`) REFER `alone/alone_actio';

-- 正在导出表  wts-trunk.alone_auth_postaction 的数据：0 rows
/*!40000 ALTER TABLE `alone_auth_postaction` DISABLE KEYS */;
/*!40000 ALTER TABLE `alone_auth_postaction` ENABLE KEYS */;

-- 导出  表 wts-trunk.alone_auth_user 结构
CREATE TABLE IF NOT EXISTS `alone_auth_user` (
  `ID` varchar(32) NOT NULL,
  `NAME` varchar(64) NOT NULL,
  `PASSWORD` varchar(128) NOT NULL COMMENT '密码(MD5旧密码或BCrypt新密码)',
  `COMMENTS` varchar(128) DEFAULT NULL,
  `TYPE` char(1) DEFAULT NULL COMMENT '1:系统用户:2其他3超级用户',
  `CTIME` varchar(14) NOT NULL,
  `UTIME` varchar(14) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `MUSER` varchar(32) NOT NULL,
  `STATE` char(1) NOT NULL,
  `LOGINNAME` varchar(64) NOT NULL,
  `LOGINTIME` varchar(14) DEFAULT NULL,
  `IMGID` varchar(32) DEFAULT NULL,
  `UUID` varchar(32) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='InnoDB free: 12288 kB alone_user';

-- 正在导出表  wts-trunk.alone_auth_user 的数据：204 rows
/*!40000 ALTER TABLE `alone_auth_user` DISABLE KEYS */;
/*!40000 ALTER TABLE `alone_auth_user` ENABLE KEYS */;

-- 导出  表 wts-trunk.alone_auth_userorg 结构
CREATE TABLE IF NOT EXISTS `alone_auth_userorg` (
  `ID` varchar(32) NOT NULL,
  `USERID` varchar(32) NOT NULL,
  `ORGANIZATIONID` varchar(32) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `USERORG_USERID` (`USERID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='InnoDB free: 12288 kB; (`organizationid`) REFER `alone/alone';

-- 正在导出表  wts-trunk.alone_auth_userorg 的数据：~458 rows (大约)
/*!40000 ALTER TABLE `alone_auth_userorg` DISABLE KEYS */;
/*!40000 ALTER TABLE `alone_auth_userorg` ENABLE KEYS */;

-- 导出  表 wts-trunk.alone_auth_userpost 结构
CREATE TABLE IF NOT EXISTS `alone_auth_userpost` (
  `ID` varchar(32) NOT NULL,
  `USERID` varchar(32) NOT NULL,
  `POSTID` varchar(32) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `USERPOST_USERID` (`USERID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.alone_auth_userpost 的数据：~0 rows (大约)
/*!40000 ALTER TABLE `alone_auth_userpost` DISABLE KEYS */;
/*!40000 ALTER TABLE `alone_auth_userpost` ENABLE KEYS */;

-- 导出  表 wts-trunk.alone_dictionary_entity 结构
CREATE TABLE IF NOT EXISTS `alone_dictionary_entity` (
  `ID` varchar(32) NOT NULL,
  `CTIME` varchar(12) NOT NULL,
  `UTIME` varchar(12) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `MUSER` varchar(32) NOT NULL,
  `STATE` char(1) NOT NULL DEFAULT '',
  `NAME` varchar(128) NOT NULL,
  `ENTITYINDEX` varchar(256) DEFAULT NULL,
  `COMMENTS` varchar(128) DEFAULT NULL,
  `TYPE` char(1) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='InnoDB free: 12288 kB';

-- 正在导出表  wts-trunk.alone_dictionary_entity 的数据：~7 rows (大约)
/*!40000 ALTER TABLE `alone_dictionary_entity` DISABLE KEYS */;
/*!40000 ALTER TABLE `alone_dictionary_entity` ENABLE KEYS */;

-- 导出  表 wts-trunk.alone_dictionary_type 结构
CREATE TABLE IF NOT EXISTS `alone_dictionary_type` (
  `CTIME` varchar(12) NOT NULL,
  `UTIME` varchar(12) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `MUSER` varchar(32) NOT NULL,
  `STATE` char(1) NOT NULL,
  `NAME` varchar(128) NOT NULL,
  `COMMENTS` varchar(128) DEFAULT NULL,
  `ENTITYTYPE` varchar(128) NOT NULL,
  `ENTITY` varchar(32) NOT NULL,
  `ID` varchar(32) NOT NULL DEFAULT '',
  `SORT` int(11) NOT NULL,
  `PARENTID` varchar(32) DEFAULT NULL,
  `TREECODE` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_Reference_8` (`ENTITY`) USING BTREE,
  CONSTRAINT `alone_dictionary_type_ibfk_1` FOREIGN KEY (`ENTITY`) REFERENCES `alone_dictionary_entity` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='InnoDB free: 12288 kB alone_dictionary_type';

-- 正在导出表  wts-trunk.alone_dictionary_type 的数据：~15 rows (大约)
/*!40000 ALTER TABLE `alone_dictionary_type` DISABLE KEYS */;
/*!40000 ALTER TABLE `alone_dictionary_type` ENABLE KEYS */;

-- 导出  表 wts-trunk.alone_parameter 结构
CREATE TABLE IF NOT EXISTS `alone_parameter` (
  `ID` varchar(32) NOT NULL,
  `CTIME` varchar(12) NOT NULL,
  `UTIME` varchar(12) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `MUSER` varchar(32) NOT NULL,
  `NAME` varchar(128) NOT NULL,
  `STATE` char(1) NOT NULL,
  `PKEY` varchar(64) NOT NULL,
  `PVALUE` varchar(2048) NOT NULL,
  `RULES` varchar(256) DEFAULT NULL,
  `DOMAIN` varchar(64) DEFAULT NULL,
  `COMMENTS` varchar(256) DEFAULT NULL,
  `VTYPE` char(1) NOT NULL COMMENT ' 文本：1 枚举：2',
  `USERABLE` varchar(1) NOT NULL COMMENT '0否，1是',
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='InnoDB free: 12288 kB; InnoDB free: 12288 kB alone_parameter';

-- 正在导出表  wts-trunk.alone_parameter 的数据：96 rows
/*!40000 ALTER TABLE `alone_parameter` DISABLE KEYS */;
/*!40000 ALTER TABLE `alone_parameter` ENABLE KEYS */;

-- 导出  表 wts-trunk.alone_parameter_local 结构
CREATE TABLE IF NOT EXISTS `alone_parameter_local` (
  `ID` varchar(32) NOT NULL,
  `PARAMETERID` varchar(32) NOT NULL,
  `EUSER` varchar(32) NOT NULL,
  `PVALUE` varchar(2048) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_Reference_50` (`PARAMETERID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.alone_parameter_local 的数据：0 rows
/*!40000 ALTER TABLE `alone_parameter_local` DISABLE KEYS */;
/*!40000 ALTER TABLE `alone_parameter_local` ENABLE KEYS */;

-- 导出  表 wts-trunk.farm_message_model 结构
CREATE TABLE IF NOT EXISTS `farm_message_model` (
  `ID` varchar(32) NOT NULL,
  `CTIME` varchar(16) NOT NULL,
  `ETIME` varchar(16) NOT NULL,
  `CUSERNAME` varchar(64) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `EUSERNAME` varchar(64) NOT NULL,
  `EUSER` varchar(32) NOT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `PCONTENT` varchar(128) DEFAULT NULL,
  `TITLE` varchar(512) NOT NULL,
  `TYPEKEY` varchar(128) NOT NULL,
  `OVERER` varchar(128) NOT NULL,
  `TITLEMODEL` varchar(512) NOT NULL,
  `CONTENTMODEL` varchar(512) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.farm_message_model 的数据：~0 rows (大约)
/*!40000 ALTER TABLE `farm_message_model` DISABLE KEYS */;
/*!40000 ALTER TABLE `farm_message_model` ENABLE KEYS */;

-- 导出  表 wts-trunk.farm_message_sender 结构
CREATE TABLE IF NOT EXISTS `farm_message_sender` (
  `ID` varchar(32) NOT NULL,
  `MODELID` varchar(32) NOT NULL,
  `APPID` varchar(32) NOT NULL,
  `TYPE` varchar(1) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_FARM_MES_REFERENCE_FARM_MES` (`MODELID`),
  CONSTRAINT `FK_FARM_MES_REFERENCE_FARM_MES` FOREIGN KEY (`MODELID`) REFERENCES `farm_message_model` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.farm_message_sender 的数据：~0 rows (大约)
/*!40000 ALTER TABLE `farm_message_sender` DISABLE KEYS */;
/*!40000 ALTER TABLE `farm_message_sender` ENABLE KEYS */;

-- 导出  表 wts-trunk.farm_qz_scheduler 结构
CREATE TABLE IF NOT EXISTS `farm_qz_scheduler` (
  `ID` varchar(32) NOT NULL,
  `AUTOIS` varchar(2) NOT NULL,
  `CTIME` varchar(16) NOT NULL,
  `ETIME` varchar(16) NOT NULL,
  `CUSERNAME` varchar(64) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `EUSERNAME` varchar(64) NOT NULL,
  `EUSER` varchar(32) NOT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `PCONTENT` varchar(128) DEFAULT NULL,
  `TASKID` varchar(32) NOT NULL,
  `TRIGGERID` varchar(32) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_Reference_18` (`TASKID`),
  KEY `FK_Reference_19` (`TRIGGERID`),
  CONSTRAINT `FK_Reference_18` FOREIGN KEY (`TASKID`) REFERENCES `farm_qz_task` (`ID`),
  CONSTRAINT `FK_Reference_19` FOREIGN KEY (`TRIGGERID`) REFERENCES `farm_qz_trigger` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.farm_qz_scheduler 的数据：~0 rows (大约)
/*!40000 ALTER TABLE `farm_qz_scheduler` DISABLE KEYS */;
/*!40000 ALTER TABLE `farm_qz_scheduler` ENABLE KEYS */;

-- 导出  表 wts-trunk.farm_qz_task 结构
CREATE TABLE IF NOT EXISTS `farm_qz_task` (
  `ID` varchar(32) NOT NULL,
  `CTIME` varchar(16) NOT NULL,
  `ETIME` varchar(16) NOT NULL,
  `CUSERNAME` varchar(64) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `EUSERNAME` varchar(64) NOT NULL,
  `EUSER` varchar(32) NOT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `PCONTENT` varchar(128) DEFAULT NULL,
  `JOBCLASS` varchar(128) NOT NULL,
  `NAME` varchar(128) NOT NULL,
  `JOBPARAS` varchar(1024) DEFAULT NULL,
  `JOBKEY` varchar(128) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.farm_qz_task 的数据：~0 rows (大约)
/*!40000 ALTER TABLE `farm_qz_task` DISABLE KEYS */;
/*!40000 ALTER TABLE `farm_qz_task` ENABLE KEYS */;

-- 导出  表 wts-trunk.farm_qz_trigger 结构
CREATE TABLE IF NOT EXISTS `farm_qz_trigger` (
  `ID` varchar(32) NOT NULL,
  `CTIME` varchar(16) NOT NULL,
  `ETIME` varchar(16) NOT NULL,
  `CUSERNAME` varchar(64) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `EUSERNAME` varchar(64) NOT NULL,
  `EUSER` varchar(32) NOT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `PCONTENT` varchar(128) DEFAULT NULL,
  `DESCRIPT` varchar(128) NOT NULL,
  `NAME` varchar(128) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.farm_qz_trigger 的数据：~0 rows (大约)
/*!40000 ALTER TABLE `farm_qz_trigger` DISABLE KEYS */;
/*!40000 ALTER TABLE `farm_qz_trigger` ENABLE KEYS */;

-- 导出  表 wts-trunk.farm_usermessage 结构
CREATE TABLE IF NOT EXISTS `farm_usermessage` (
  `ID` varchar(32) NOT NULL,
  `CTIME` varchar(16) NOT NULL,
  `CUSER` varchar(32) DEFAULT NULL,
  `CUSERNAME` varchar(64) DEFAULT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `PCONTENT` varchar(128) DEFAULT NULL,
  `READUSERID` varchar(32) NOT NULL,
  `CONTENT` text NOT NULL,
  `TITLE` varchar(64) NOT NULL,
  `READSTATE` varchar(2) NOT NULL COMMENT '0未读、1已读',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.farm_usermessage 的数据：~230 rows (大约)
/*!40000 ALTER TABLE `farm_usermessage` DISABLE KEYS */;
/*!40000 ALTER TABLE `farm_usermessage` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_card 结构
CREATE TABLE IF NOT EXISTS `wts_card` (
  `ID` varchar(32) NOT NULL,
  `PAPERID` varchar(32) NOT NULL,
  `ROOMID` varchar(32) NOT NULL,
  `USERID` varchar(32) NOT NULL,
  `POINT` float NOT NULL,
  `ADJUDGEUSERNAME` varchar(64) DEFAULT NULL,
  `ADJUDGEUSER` varchar(32) DEFAULT NULL,
  `ADJUDGETIME` varchar(16) DEFAULT NULL,
  `STARTTIME` varchar(16) DEFAULT NULL,
  `ENDTIME` varchar(16) DEFAULT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `PCONTENT` varchar(256) DEFAULT NULL,
  `COMPLETENUM` int(11) DEFAULT NULL,
  `ALLNUM` int(11) DEFAULT NULL,
  `OVERTIME` varchar(2) NOT NULL,
  `RESULTSTYPE` varchar(2) NOT NULL,
  `SUBMITTIME` varchar(16) DEFAULT NULL,
  `ROOMUUID` varchar(32) NOT NULL,
  `PAPERUUID` varchar(32) NOT NULL,
  `USERUUID` varchar(32) NOT NULL,
  `ADJUDGEUSERUUID` varchar(32) DEFAULT NULL,
  `STATISTICAL` varchar(2) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `USER_PAPER_CARD` (`PAPERID`,`USERID`,`ROOMID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_card 的数据：~0 rows (大约)
/*!40000 ALTER TABLE `wts_card` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_card` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_card_answer 结构
CREATE TABLE IF NOT EXISTS `wts_card_answer` (
  `ID` varchar(32) NOT NULL,
  `CARDID` varchar(32) NOT NULL,
  `ANSWERID` varchar(32) DEFAULT NULL,
  `VERSIONID` varchar(32) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `VALSTR` text NOT NULL,
  `CTIME` varchar(16) NOT NULL,
  `PCONTENT` varchar(256) DEFAULT NULL,
  `PSTATE` varchar(2) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `USER_ANSWER` (`CARDID`,`VERSIONID`,`ANSWERID`),
  KEY `USER_SUBVERSION` (`CARDID`,`VERSIONID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_card_answer 的数据：~0 rows (大约)
/*!40000 ALTER TABLE `wts_card_answer` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_card_answer` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_card_point 结构
CREATE TABLE IF NOT EXISTS `wts_card_point` (
  `ID` char(32) NOT NULL,
  `POINT` int(11) DEFAULT NULL,
  `CARDID` char(32) NOT NULL,
  `VERSIONID` char(32) NOT NULL,
  `COMPLETE` char(1) NOT NULL,
  `MPOINT` int(11) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `CARDID_CARD_POINT` (`CARDID`),
  KEY `VERSIONID_CARD_POINT` (`VERSIONID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_card_point 的数据：~0 rows (大约)
/*!40000 ALTER TABLE `wts_card_point` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_card_point` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_comment 结构
CREATE TABLE IF NOT EXISTS `wts_comment` (
  `ID` varchar(32) NOT NULL,
  `CTIME` varchar(16) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `CUSERNAME` varchar(64) NOT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `TEXT` text NOT NULL,
  `TYPE` varchar(1) NOT NULL,
  `BANDID` varchar(32) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_comment 的数据：0 rows
/*!40000 ALTER TABLE `wts_comment` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_comment` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_docfile 结构
CREATE TABLE IF NOT EXISTS `wts_docfile` (
  `DIR` varchar(256) NOT NULL,
  `SERVERID` varchar(32) NOT NULL,
  `TYPE` varchar(2) NOT NULL COMMENT '1图片',
  `NAME` varchar(512) DEFAULT NULL,
  `EXNAME` varchar(16) NOT NULL,
  `LEN` float NOT NULL,
  `FILENAME` varchar(64) NOT NULL,
  `ID` varchar(32) NOT NULL,
  `CTIME` varchar(16) NOT NULL,
  `ETIME` varchar(16) NOT NULL,
  `CUSERNAME` varchar(64) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `EUSERNAME` varchar(64) NOT NULL,
  `EUSER` varchar(32) NOT NULL,
  `PCONTENT` varchar(128) DEFAULT NULL,
  `PSTATE` varchar(2) NOT NULL COMMENT '1正常、0临时',
  `DOWNUM` int(11) NOT NULL,
  `APPID` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_docfile 的数据：~813 rows (大约)
/*!40000 ALTER TABLE `wts_docfile` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_docfile` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_docfile_text 结构
CREATE TABLE IF NOT EXISTS `wts_docfile_text` (
  `ID` varchar(32) NOT NULL,
  `DESCRIBES` text NOT NULL,
  `FILEID` varchar(32) NOT NULL,
  `DOCID` varchar(32) NOT NULL,
  `DESCRIBESMIN` varchar(128) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_Reference_49` (`FILEID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_docfile_text 的数据：0 rows
/*!40000 ALTER TABLE `wts_docfile_text` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_docfile_text` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_exam_pop 结构
CREATE TABLE IF NOT EXISTS `wts_exam_pop` (
  `ID` varchar(32) NOT NULL,
  `FUNTYPE` varchar(1) NOT NULL,
  `USERID` varchar(32) NOT NULL,
  `USERNAME` varchar(64) NOT NULL,
  `TYPEID` varchar(32) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_WTS_EXAM_REFERENCE_WTS_EXAM` (`TYPEID`),
  CONSTRAINT `FK_WTS_EXAM_REFERENCE_WTS_EXAM` FOREIGN KEY (`TYPEID`) REFERENCES `wts_exam_type` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_exam_pop 的数据：~400 rows (大约)
/*!40000 ALTER TABLE `wts_exam_pop` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_exam_pop` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_exam_stat 结构
CREATE TABLE IF NOT EXISTS `wts_exam_stat` (
  `ID` varchar(32) NOT NULL,
  `CTIME` varchar(16) NOT NULL,
  `ETIME` varchar(16) NOT NULL,
  `CUSERNAME` varchar(64) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `EUSERNAME` varchar(64) NOT NULL,
  `EUSER` varchar(32) NOT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `PCONTENT` varchar(128) DEFAULT NULL,
  `SUBJECTNUM` int(11) NOT NULL,
  `ERRORSUBNUM` int(11) NOT NULL,
  `PAPERNUM` int(11) NOT NULL,
  `TESTNUM` int(11) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_exam_stat 的数据：207 rows
/*!40000 ALTER TABLE `wts_exam_stat` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_exam_stat` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_exam_type 结构
CREATE TABLE IF NOT EXISTS `wts_exam_type` (
  `ID` varchar(32) NOT NULL,
  `TREECODE` varchar(256) NOT NULL,
  `COMMENTS` varchar(128) DEFAULT NULL,
  `NAME` varchar(64) NOT NULL,
  `CTIME` varchar(14) NOT NULL,
  `UTIME` varchar(14) NOT NULL,
  `STATE` char(1) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `MUSER` varchar(32) NOT NULL,
  `PARENTID` varchar(32) DEFAULT NULL,
  `SORT` int(11) DEFAULT NULL,
  `MNGPOP` char(1) NOT NULL,
  `ADJUDGEPOP` char(1) NOT NULL,
  `QUERYPOP` char(1) NOT NULL,
  `SUPERPOP` char(1) NOT NULL,
  `DOMAIN` varchar(16) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_exam_type 的数据：~8 rows (大约)
/*!40000 ALTER TABLE `wts_exam_type` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_exam_type` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_his_answer 结构
CREATE TABLE IF NOT EXISTS `wts_his_answer` (
  `ID` varchar(32) NOT NULL,
  `TITLE` varchar(512) NOT NULL,
  `SORT` int(11) NOT NULL,
  `RIGHTANSWER` varchar(2) NOT NULL,
  `ANSWERUUID` varchar(32) NOT NULL,
  `PAPERUUID` varchar(32) NOT NULL,
  `SUBJECTUUID` varchar(32) NOT NULL,
  `ROOMUUID` varchar(32) NOT NULL,
  `BACKVERSION` varchar(16) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_his_answer 的数据：0 rows
/*!40000 ALTER TABLE `wts_his_answer` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_his_answer` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_his_card 结构
CREATE TABLE IF NOT EXISTS `wts_his_card` (
  `ID` varchar(32) NOT NULL,
  `PAPERNAME` varchar(128) NOT NULL,
  `PAPERID` varchar(32) NOT NULL,
  `PAPERUUID` varchar(32) DEFAULT NULL,
  `ROOMNAME` varchar(64) NOT NULL,
  `ROOMID` varchar(32) NOT NULL,
  `ROOMUUID` varchar(32) DEFAULT NULL,
  `USERID` varchar(32) NOT NULL,
  `USERUUID` varchar(32) DEFAULT NULL,
  `USERNAME` varchar(64) NOT NULL,
  `POINT` float NOT NULL,
  `ADJUDGEUSER` varchar(32) DEFAULT NULL,
  `ADJUDGEUSERUUID` varchar(32) DEFAULT NULL,
  `ADJUDGEUSERNAME` varchar(64) DEFAULT NULL,
  `ADJUDGETIME` varchar(16) DEFAULT NULL,
  `STARTTIME` varchar(16) DEFAULT NULL,
  `ENDTIME` varchar(16) DEFAULT NULL,
  `SUBMITTIME` varchar(16) DEFAULT NULL,
  `OVERTIME` varchar(2) NOT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `PCONTENT` varchar(256) DEFAULT NULL,
  `COMPLETENUM` int(11) DEFAULT NULL,
  `ALLNUM` int(11) DEFAULT NULL,
  `BACKVERSION` varchar(16) NOT NULL,
  `ALLPOINT` int(11) DEFAULT NULL,
  `ALLTIME` int(11) DEFAULT NULL,
  `RESULTSTYPE` varchar(2) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_his_card 的数据：0 rows
/*!40000 ALTER TABLE `wts_his_card` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_his_card` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_his_card_answer 结构
CREATE TABLE IF NOT EXISTS `wts_his_card_answer` (
  `ID` varchar(32) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `VALSTR` text NOT NULL,
  `CTIME` varchar(16) NOT NULL,
  `PCONTENT` varchar(256) DEFAULT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `CARDID` varchar(32) NOT NULL,
  `VERSIONID` varchar(32) NOT NULL,
  `ANSWERID` varchar(32) DEFAULT NULL,
  `SUBJECTUUID` varchar(32) NOT NULL,
  `ANSWERUUID` varchar(32) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_his_card_answer 的数据：0 rows
/*!40000 ALTER TABLE `wts_his_card_answer` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_his_card_answer` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_his_card_point 结构
CREATE TABLE IF NOT EXISTS `wts_his_card_point` (
  `ID` char(32) NOT NULL,
  `POINT` int(11) DEFAULT NULL,
  `CARDID` char(32) NOT NULL,
  `VERSIONID` char(32) NOT NULL,
  `SUBJECTUUID` char(32) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_his_card_point 的数据：0 rows
/*!40000 ALTER TABLE `wts_his_card_point` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_his_card_point` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_his_subject 结构
CREATE TABLE IF NOT EXISTS `wts_his_subject` (
  `ID` varchar(32) NOT NULL,
  `TITLE` varchar(512) NOT NULL,
  `TYPE` varchar(1) NOT NULL,
  `SUBJECTUUID` varchar(32) NOT NULL,
  `PAPERUUID` varchar(32) NOT NULL,
  `SORT` int(11) NOT NULL,
  `POINT` int(11) NOT NULL,
  `ROOMUUID` varchar(32) DEFAULT NULL,
  `BACKVERSION` varchar(16) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_his_subject 的数据：0 rows
/*!40000 ALTER TABLE `wts_his_subject` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_his_subject` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_his_user 结构
CREATE TABLE IF NOT EXISTS `wts_his_user` (
  `ID` varchar(32) NOT NULL,
  `NAME` varchar(128) NOT NULL,
  `LOGINNAME` varchar(128) NOT NULL,
  `ORGNAME` varchar(128) DEFAULT NULL,
  `POSTNAMES` varchar(512) DEFAULT NULL,
  `POSTUUID` varchar(256) DEFAULT NULL,
  `ORGUUID` varchar(32) DEFAULT NULL,
  `USERUUID` varchar(32) NOT NULL,
  `BACKVERSION` varchar(16) NOT NULL,
  `ROOMUUID` varchar(32) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_his_user 的数据：0 rows
/*!40000 ALTER TABLE `wts_his_user` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_his_user` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_know 结构
CREATE TABLE IF NOT EXISTS `wts_know` (
  `ID` varchar(32) NOT NULL,
  `TREECODE` varchar(256) NOT NULL,
  `COMMENTS` varchar(128) DEFAULT NULL,
  `NAME` varchar(64) NOT NULL,
  `PKEY` varchar(64) NOT NULL,
  `CTIME` varchar(14) NOT NULL,
  `STATE` char(1) NOT NULL,
  `PARENTID` varchar(32) DEFAULT NULL,
  `RESURL` varchar(256) DEFAULT NULL,
  `SORT` int(11) DEFAULT NULL,
  `TYPE` varchar(1) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_know 的数据：18 rows
/*!40000 ALTER TABLE `wts_know` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_know` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_know_cardsta 结构
CREATE TABLE IF NOT EXISTS `wts_know_cardsta` (
  `ID` varchar(32) NOT NULL,
  `KNOWID` varchar(32) NOT NULL,
  `USERID` varchar(32) NOT NULL,
  `TIME8` varchar(8) NOT NULL,
  `CARDID` varchar(32) NOT NULL,
  `RIGHTNUM` int(11) NOT NULL,
  `WRONGNUM` int(11) NOT NULL,
  `ALLNUM` int(11) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_WTS_KNOW_REFERENCE_CARD` (`KNOWID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_know_cardsta 的数据：594 rows
/*!40000 ALTER TABLE `wts_know_cardsta` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_know_cardsta` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_know_monthsta 结构
CREATE TABLE IF NOT EXISTS `wts_know_monthsta` (
  `ID` varchar(32) NOT NULL,
  `KNOWID` varchar(32) NOT NULL,
  `USERID` varchar(32) NOT NULL,
  `TIME6` varchar(6) NOT NULL,
  `RIGHTNUM` int(11) NOT NULL,
  `WRONGNUM` int(11) NOT NULL,
  `ALLNUM` int(11) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_WTS_KNOW_REFERENCE_MONTH` (`KNOWID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_know_monthsta 的数据：0 rows
/*!40000 ALTER TABLE `wts_know_monthsta` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_know_monthsta` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_know_resource 结构
CREATE TABLE IF NOT EXISTS `wts_know_resource` (
  `ID` varchar(32) NOT NULL,
  `KNOWID` varchar(32) NOT NULL,
  `TYPE` varchar(8) NOT NULL,
  `URL` varchar(512) NOT NULL,
  `TITLE` varchar(64) NOT NULL,
  `NOTE` varchar(512) DEFAULT NULL,
  `PSTATE` varchar(1) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `CTIME` varchar(14) NOT NULL,
  `ETIME` varchar(14) NOT NULL,
  `EUSER` varchar(32) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_know_resource 的数据：0 rows
/*!40000 ALTER TABLE `wts_know_resource` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_know_resource` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_know_subject 结构
CREATE TABLE IF NOT EXISTS `wts_know_subject` (
  `ID` varchar(32) NOT NULL,
  `KNOWID` varchar(32) NOT NULL,
  `SUBJECTID` varchar(32) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_WTS_KNOW_REFERENCE1` (`SUBJECTID`),
  KEY `FK_WTS_KNOW_REFERENCE2` (`KNOWID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_know_subject 的数据：184 rows
/*!40000 ALTER TABLE `wts_know_subject` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_know_subject` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_log_point 结构
CREATE TABLE IF NOT EXISTS `wts_log_point` (
  `ID` varchar(32) NOT NULL,
  `UUID` varchar(32) NOT NULL,
  `CTIME` varchar(16) NOT NULL,
  `PCONTENT` varchar(128) DEFAULT NULL,
  `POINT` float NOT NULL,
  `ROOMNAME` varchar(256) DEFAULT NULL,
  `ROOMID` varchar(32) DEFAULT NULL,
  `PAPERNAME` varchar(256) DEFAULT NULL,
  `PAPERID` varchar(32) DEFAULT NULL,
  `PSHOWTYPE` varchar(2) DEFAULT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `USERID` varchar(32) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_log_point 的数据：182 rows
/*!40000 ALTER TABLE `wts_log_point` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_log_point` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_material 结构
CREATE TABLE IF NOT EXISTS `wts_material` (
  `ID` varchar(32) NOT NULL,
  `CTIME` varchar(16) NOT NULL,
  `ETIME` varchar(16) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `EUSER` varchar(32) NOT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `PCONTENT` varchar(128) DEFAULT NULL,
  `TEXT` text NOT NULL,
  `TITLE` varchar(512) NOT NULL,
  `UUID` varchar(32) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_material 的数据：1 rows
/*!40000 ALTER TABLE `wts_material` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_material` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_node_card 结构
CREATE TABLE IF NOT EXISTS `wts_node_card` (
  `ID` varchar(32) NOT NULL,
  `CTIME` varchar(16) NOT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `FUNC` varchar(2) NOT NULL,
  `PCONTENT` varchar(128) DEFAULT NULL,
  `USERID` varchar(32) DEFAULT NULL,
  `APPID` varchar(32) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_node_card 的数据：0 rows
/*!40000 ALTER TABLE `wts_node_card` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_node_card` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_paper 结构
CREATE TABLE IF NOT EXISTS `wts_paper` (
  `ID` varchar(32) NOT NULL,
  `EXAMTYPEID` varchar(32) DEFAULT NULL,
  `CTIME` varchar(16) NOT NULL,
  `ETIME` varchar(16) NOT NULL,
  `CUSERNAME` varchar(64) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `EUSERNAME` varchar(64) NOT NULL,
  `EUSER` varchar(32) NOT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `PCONTENT` varchar(128) DEFAULT NULL,
  `NAME` varchar(128) NOT NULL,
  `SUBJECTNUM` int(11) NOT NULL,
  `POINTNUM` int(11) NOT NULL,
  `COMPLETETNUM` int(11) NOT NULL,
  `AVGPOINT` int(11) NOT NULL,
  `TOPPOINT` int(11) NOT NULL,
  `LOWPOINT` int(11) NOT NULL,
  `ADVICETIME` int(11) NOT NULL,
  `PAPERNOTE` text,
  `BOOKNUM` int(11) NOT NULL,
  `UUID` varchar(32) NOT NULL,
  `KNOWID` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_WTS_PAPE_REFERENCE_WTS_EXAM` (`EXAMTYPEID`),
  CONSTRAINT `FK_WTS_PAPE_REFERENCE_WTS_EXAM` FOREIGN KEY (`EXAMTYPEID`) REFERENCES `wts_exam_type` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_paper 的数据：~6 rows (大约)
/*!40000 ALTER TABLE `wts_paper` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_paper` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_paper_chapter 结构
CREATE TABLE IF NOT EXISTS `wts_paper_chapter` (
  `ID` varchar(32) NOT NULL,
  `STYPE` varchar(2) NOT NULL,
  `PTYPE` varchar(2) NOT NULL,
  `INITPOINT` int(11) NOT NULL,
  `SUBJECTTYPEID` varchar(32) DEFAULT NULL,
  `SUBJECTNUM` int(11) DEFAULT NULL,
  `SUBJECTPOINT` int(11) DEFAULT NULL,
  `NAME` varchar(64) NOT NULL,
  `TEXTNOTE` text,
  `PARENTID` varchar(32) NOT NULL,
  `PAPERID` varchar(32) NOT NULL,
  `SORT` int(11) NOT NULL,
  `TREECODE` varchar(256) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_WTS_PAPE_REFERENCE_WTS_PAPE` (`PAPERID`),
  CONSTRAINT `FK_WTS_PAPE_REFERENCE_WTS_PAPE` FOREIGN KEY (`PAPERID`) REFERENCES `wts_paper` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_paper_chapter 的数据：~9 rows (大约)
/*!40000 ALTER TABLE `wts_paper_chapter` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_paper_chapter` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_paper_subject 结构
CREATE TABLE IF NOT EXISTS `wts_paper_subject` (
  `ID` varchar(32) NOT NULL,
  `VERSIONID` varchar(32) NOT NULL,
  `SUBJECTID` varchar(32) NOT NULL,
  `CHAPTERID` varchar(32) NOT NULL,
  `SORT` int(11) NOT NULL,
  `POINT` int(11) NOT NULL,
  `PAPERID` varchar(32) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_WTS_PAPE_REFERENCE_WTS_SUBJ` (`VERSIONID`),
  KEY `PAPERSUBJECT_PID_SID` (`PAPERID`,`SUBJECTID`),
  CONSTRAINT `FK_WTS_PAPE_REFERENCE_WTS_SUBJ` FOREIGN KEY (`VERSIONID`) REFERENCES `wts_subject_version` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_paper_subject 的数据：~54 rows (大约)
/*!40000 ALTER TABLE `wts_paper_subject` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_paper_subject` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_paper_userown 结构
CREATE TABLE IF NOT EXISTS `wts_paper_userown` (
  `ID` varchar(32) NOT NULL,
  `CTIME` varchar(16) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `CUSERNAME` varchar(64) NOT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `PCONTENT` varchar(128) DEFAULT NULL,
  `MODELTYPE` varchar(2) NOT NULL,
  `PAPERID` varchar(32) NOT NULL,
  `ROOMID` varchar(32) DEFAULT NULL,
  `PAPERNAME` varchar(128) NOT NULL,
  `ROOMNAME` varchar(128) DEFAULT NULL,
  `SCORE` float DEFAULT NULL,
  `RPCENT` int(11) DEFAULT NULL,
  `CARDID` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_paper_userown 的数据：570 rows
/*!40000 ALTER TABLE `wts_paper_userown` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_paper_userown` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_random_item 结构
CREATE TABLE IF NOT EXISTS `wts_random_item` (
  `ID` varchar(32) NOT NULL,
  `CTIME` varchar(16) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `PCONTENT` varchar(128) DEFAULT NULL,
  `NAME` varchar(64) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_random_item 的数据：1 rows
/*!40000 ALTER TABLE `wts_random_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_random_item` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_random_step 结构
CREATE TABLE IF NOT EXISTS `wts_random_step` (
  `ID` varchar(32) NOT NULL,
  `TYPEID` varchar(32) DEFAULT NULL,
  `TIPTYPE` varchar(2) NOT NULL,
  `SUBNUM` int(11) NOT NULL,
  `SUBPOINT` int(11) NOT NULL,
  `SORT` int(11) NOT NULL,
  `NAME` varchar(64) NOT NULL,
  `PCONTENT` varchar(128) DEFAULT NULL,
  `ITEMID` varchar(32) NOT NULL,
  `KNOWID` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_WTS_RAND_REFERENCE_WTS_RAND` (`ITEMID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_random_step 的数据：1 rows
/*!40000 ALTER TABLE `wts_random_step` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_random_step` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_room 结构
CREATE TABLE IF NOT EXISTS `wts_room` (
  `ID` varchar(32) NOT NULL,
  `DUSERNAME` varchar(32) DEFAULT NULL,
  `DUSER` varchar(32) DEFAULT NULL,
  `DTIME` varchar(14) DEFAULT NULL,
  `PCONTENT` varchar(128) DEFAULT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `EUSER` varchar(32) NOT NULL,
  `EUSERNAME` varchar(64) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `CUSERNAME` varchar(64) NOT NULL,
  `ETIME` varchar(16) NOT NULL,
  `CTIME` varchar(16) NOT NULL,
  `EXAMTYPEID` varchar(32) DEFAULT NULL,
  `TIMETYPE` varchar(2) NOT NULL,
  `STARTTIME` varchar(16) DEFAULT NULL,
  `ENDTIME` varchar(16) DEFAULT NULL,
  `WRITETYPE` varchar(2) NOT NULL,
  `ROOMNOTE` text,
  `TIMELEN` int(11) NOT NULL,
  `COUNTTYPE` varchar(2) NOT NULL,
  `NAME` varchar(64) NOT NULL,
  `RESTARTTYPE` varchar(2) NOT NULL,
  `IMGID` varchar(32) DEFAULT NULL,
  `SSORTTYPE` varchar(2) NOT NULL,
  `OSORTTYPE` varchar(2) NOT NULL,
  `PSHOWTYPE` varchar(2) NOT NULL,
  `UUID` varchar(32) NOT NULL,
  `RESULTSTYPE` varchar(2) NOT NULL,
  `PUBLICTYPE` varchar(2) NOT NULL,
  `ADJUDGETYPE` varchar(2) NOT NULL,
  `PICKTYPE` varchar(2) NOT NULL,
  `TYPE` varchar(2) DEFAULT NULL,
  `STATISTICAL` varchar(2) DEFAULT NULL,
  `TYPEMODEL` varchar(2) DEFAULT NULL,
  `PAPERVMODEL` varchar(2) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_WTS_ROOM_REFERENCE_WTS_EXAM` (`EXAMTYPEID`),
  CONSTRAINT `FK_WTS_ROOM_REFERENCE_WTS_EXAM` FOREIGN KEY (`EXAMTYPEID`) REFERENCES `wts_exam_type` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_room 的数据：~6 rows (大约)
/*!40000 ALTER TABLE `wts_room` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_room` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_room_paper 结构
CREATE TABLE IF NOT EXISTS `wts_room_paper` (
  `ID` varchar(32) NOT NULL,
  `ROOMID` varchar(32) NOT NULL,
  `PAPERID` varchar(32) NOT NULL,
  `NAME` varchar(512) DEFAULT NULL,
  `PASSPOINT` float DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_WTS_ROOM_REFERENCE_WTS_PAPE` (`PAPERID`),
  CONSTRAINT `FK_WTS_ROOM_REFERENCE_WTS_PAPE` FOREIGN KEY (`PAPERID`) REFERENCES `wts_paper` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_room_paper 的数据：~6 rows (大约)
/*!40000 ALTER TABLE `wts_room_paper` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_room_paper` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_room_scoreeval 结构
CREATE TABLE IF NOT EXISTS `wts_room_scoreeval` (
  `ID` varchar(32) NOT NULL,
  `NAME` varchar(64) NOT NULL,
  `POINTS` int(11) NOT NULL,
  `POINTE` int(11) NOT NULL,
  `DESCRIBES` text,
  `PAPERID` varchar(32) DEFAULT NULL,
  `ROOMID` varchar(32) NOT NULL,
  `NOTE` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_room_scoreeval 的数据：10 rows
/*!40000 ALTER TABLE `wts_room_scoreeval` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_room_scoreeval` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_room_user 结构
CREATE TABLE IF NOT EXISTS `wts_room_user` (
  `ID` varchar(32) NOT NULL,
  `USERID` varchar(32) NOT NULL,
  `ROOMID` varchar(32) NOT NULL,
  `GROUPID` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_WTS_ROOM_REFERENCE_WTS_ROOM` (`ROOMID`),
  CONSTRAINT `FK_WTS_ROOM_REFERENCE_WTS_ROOM` FOREIGN KEY (`ROOMID`) REFERENCES `wts_room` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_room_user 的数据：~858 rows (大约)
/*!40000 ALTER TABLE `wts_room_user` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_room_user` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_room_usergroup 结构
CREATE TABLE IF NOT EXISTS `wts_room_usergroup` (
  `ID` varchar(32) NOT NULL,
  `OBJID` varchar(32) NOT NULL,
  `OBJNAME` varchar(128) NOT NULL,
  `OBJTYPE` varchar(2) NOT NULL,
  `ROOMID` varchar(32) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_WTS_USERGROUP_REFERENCE_WTS_ROOM` (`ROOMID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_room_usergroup 的数据：2 rows
/*!40000 ALTER TABLE `wts_room_usergroup` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_room_usergroup` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_share_node 结构
CREATE TABLE IF NOT EXISTS `wts_share_node` (
  `ID` varchar(32) NOT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `PCONTENT` varchar(128) DEFAULT NULL,
  `BASEURL` varchar(128) NOT NULL,
  `GUESTURL` varchar(128) NOT NULL,
  `SECRET` varchar(128) NOT NULL,
  `APIUSERLNM` varchar(128) NOT NULL,
  `APIUSERPWD` varchar(128) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_share_node 的数据：0 rows
/*!40000 ALTER TABLE `wts_share_node` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_share_node` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_share_room 结构
CREATE TABLE IF NOT EXISTS `wts_share_room` (
  `ID` varchar(32) NOT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `PCONTENT` varchar(128) DEFAULT NULL,
  `NODEID` varchar(32) DEFAULT NULL,
  `ROOMID` varchar(32) DEFAULT NULL,
  `ETIME` varchar(16) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_WTS_SHAR_REFERENCE_WTS_SHAR` (`NODEID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_share_room 的数据：0 rows
/*!40000 ALTER TABLE `wts_share_room` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_share_room` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_share_user 结构
CREATE TABLE IF NOT EXISTS `wts_share_user` (
  `ID` varchar(32) NOT NULL,
  `USERID` varchar(32) NOT NULL,
  `ROOMID` varchar(32) NOT NULL,
  `NODEID` varchar(32) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_share_user 的数据：0 rows
/*!40000 ALTER TABLE `wts_share_user` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_share_user` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_statistical_backup 结构
CREATE TABLE IF NOT EXISTS `wts_statistical_backup` (
  `ID` varchar(32) NOT NULL,
  `VKEY` varchar(128) DEFAULT NULL,
  `VALSTR` varchar(512) DEFAULT NULL,
  `VALINT3` int(11) DEFAULT NULL,
  `VALINT2` int(11) DEFAULT NULL,
  `VALINT1` int(11) DEFAULT NULL,
  `VALFLOAT` float DEFAULT NULL,
  `VALTITLE` varchar(512) DEFAULT NULL,
  `SORT` int(11) DEFAULT NULL,
  `ID1` varchar(32) DEFAULT NULL,
  `ID2` varchar(32) DEFAULT NULL,
  `ID3` varchar(32) DEFAULT NULL,
  `ID4` varchar(32) DEFAULT NULL,
  `NOTE` varchar(512) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_statistical_backup 的数据：236 rows
/*!40000 ALTER TABLE `wts_statistical_backup` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_statistical_backup` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_statistical_card 结构
CREATE TABLE IF NOT EXISTS `wts_statistical_card` (
  `ID` varchar(32) NOT NULL,
  `CARDID` varchar(32) NOT NULL,
  `ROOMID` varchar(32) NOT NULL,
  `VKEY` varchar(128) DEFAULT NULL,
  `VALSTR` varchar(512) DEFAULT NULL,
  `VALINT1` int(11) DEFAULT NULL,
  `VALINT2` int(11) DEFAULT NULL,
  `VALINT3` int(11) DEFAULT NULL,
  `VALFLOAT` float DEFAULT NULL,
  `VALTITLE` varchar(512) DEFAULT NULL,
  `SORT` int(11) NOT NULL,
  `ID1` varchar(512) DEFAULT NULL,
  `ID2` varchar(512) DEFAULT NULL,
  `ID3` varchar(512) DEFAULT NULL,
  `NOTE` varchar(512) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `WTS_STATL_CARD_1` (`ID1`(333)),
  KEY `WTS_STATL_CARD_2` (`ID2`(333)),
  KEY `WTS_STATL_CARD_3` (`ID3`(333)),
  KEY `WTS_STATL_CARD_ID` (`CARDID`),
  KEY `WTS_STATL_CARD_ROOM` (`ROOMID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_statistical_card 的数据：2,907 rows
/*!40000 ALTER TABLE `wts_statistical_card` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_statistical_card` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_statistical_room 结构
CREATE TABLE IF NOT EXISTS `wts_statistical_room` (
  `ID` varchar(32) NOT NULL,
  `ROOMID` varchar(32) NOT NULL,
  `VKEY` varchar(128) DEFAULT NULL,
  `VALSTR` varchar(512) DEFAULT NULL,
  `VALINT1` int(11) DEFAULT NULL,
  `VALINT2` int(11) DEFAULT NULL,
  `VALINT3` int(11) DEFAULT NULL,
  `VALFLOAT` float DEFAULT NULL,
  `VALTITLE` varchar(512) DEFAULT NULL,
  `SORT` int(11) NOT NULL,
  `ID1` varchar(512) DEFAULT NULL,
  `ID2` varchar(512) DEFAULT NULL,
  `ID3` varchar(512) DEFAULT NULL,
  `ID4` varchar(512) DEFAULT NULL,
  `NOTE` varchar(512) DEFAULT NULL,
  `JSON1` text,
  `JSON2` text,
  `JSON3` text,
  `JSON4` text,
  PRIMARY KEY (`ID`),
  KEY `WTS_STATL_ROOM_1` (`ID1`(333)),
  KEY `WTS_STATL_ROOM_2` (`ID2`(333)),
  KEY `WTS_STATL_ROOM_3` (`ID3`(333)),
  KEY `WTS_STATL_ROOM_4` (`ID4`(333)),
  KEY `WTS_STATL_ROOM_ID` (`ROOMID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_statistical_room 的数据：559 rows
/*!40000 ALTER TABLE `wts_statistical_room` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_statistical_room` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_subject 结构
CREATE TABLE IF NOT EXISTS `wts_subject` (
  `ID` varchar(32) NOT NULL,
  `TYPEID` varchar(32) DEFAULT NULL,
  `VERSIONID` varchar(32) DEFAULT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `MATERIALID` varchar(32) DEFAULT NULL,
  `PRAISENUM` int(11) NOT NULL,
  `COMMENTNUM` int(11) NOT NULL,
  `ANALYSISNUM` int(11) NOT NULL,
  `DONUM` int(11) NOT NULL,
  `RIGHTNUM` int(11) NOT NULL,
  `UUID` varchar(32) NOT NULL,
  `INTRODUCTION` varchar(512) DEFAULT NULL,
  `LEVEL` int(11) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_WTS_SUBJ_REFERENCE_WTS_SUBJ` (`TYPEID`),
  KEY `WTS_SUBJECT_VERSIONID` (`VERSIONID`),
  CONSTRAINT `FK_WTS_SUBJ_REFERENCE_WTS_SUBJ` FOREIGN KEY (`TYPEID`) REFERENCES `wts_subject_type` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_subject 的数据：~188 rows (大约)
/*!40000 ALTER TABLE `wts_subject` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_subject` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_subject_analysis 结构
CREATE TABLE IF NOT EXISTS `wts_subject_analysis` (
  `ID` varchar(32) NOT NULL,
  `CTIME` varchar(16) NOT NULL,
  `CUSERNAME` varchar(64) NOT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `PCONTENT` varchar(128) DEFAULT NULL,
  `TEXT` text NOT NULL,
  `SUBJECTID` varchar(32) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_subject_analysis 的数据：33 rows
/*!40000 ALTER TABLE `wts_subject_analysis` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_subject_analysis` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_subject_answer 结构
CREATE TABLE IF NOT EXISTS `wts_subject_answer` (
  `ID` varchar(32) NOT NULL,
  `CTIME` varchar(16) NOT NULL,
  `CUSERNAME` varchar(64) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `PCONTENT` varchar(128) DEFAULT NULL,
  `VERSIONID` varchar(32) NOT NULL,
  `ANSWER` varchar(512) NOT NULL,
  `ANSWERNOTE` text,
  `SORT` int(11) NOT NULL,
  `RIGHTANSWER` varchar(2) NOT NULL,
  `POINTWEIGHT` int(11) DEFAULT NULL,
  `UUID` varchar(32) NOT NULL,
  `GROUPNO` int(11) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `SUBJECTANSWER_VERSIONID` (`VERSIONID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_subject_answer 的数据：~480 rows (大约)
/*!40000 ALTER TABLE `wts_subject_answer` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_subject_answer` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_subject_comment 结构
CREATE TABLE IF NOT EXISTS `wts_subject_comment` (
  `ID` varchar(32) NOT NULL,
  `CTIME` varchar(16) NOT NULL,
  `CUSERNAME` varchar(64) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `PCONTENT` varchar(128) DEFAULT NULL,
  `TEXT` text NOT NULL,
  `SUBJECTID` varchar(32) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_subject_comment 的数据：27 rows
/*!40000 ALTER TABLE `wts_subject_comment` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_subject_comment` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_subject_pop 结构
CREATE TABLE IF NOT EXISTS `wts_subject_pop` (
  `ID` varchar(32) NOT NULL,
  `FUNTYPE` varchar(1) NOT NULL,
  `USERID` varchar(32) NOT NULL,
  `USERNAME` varchar(64) NOT NULL,
  `TYPEID` varchar(32) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_subject_pop 的数据：~0 rows (大约)
/*!40000 ALTER TABLE `wts_subject_pop` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_subject_pop` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_subject_type 结构
CREATE TABLE IF NOT EXISTS `wts_subject_type` (
  `ID` varchar(32) NOT NULL,
  `TREECODE` varchar(256) NOT NULL,
  `COMMENTS` varchar(128) DEFAULT NULL,
  `NAME` varchar(64) NOT NULL,
  `CTIME` varchar(14) NOT NULL,
  `UTIME` varchar(14) NOT NULL,
  `STATE` char(1) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `MUSER` varchar(32) NOT NULL,
  `PARENTID` varchar(32) DEFAULT NULL,
  `SORT` int(11) DEFAULT NULL,
  `READPOP` char(1) NOT NULL,
  `WRITEPOP` char(1) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_subject_type 的数据：~0 rows (大约)
/*!40000 ALTER TABLE `wts_subject_type` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_subject_type` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_subject_userown 结构
CREATE TABLE IF NOT EXISTS `wts_subject_userown` (
  `ID` varchar(32) NOT NULL,
  `CTIME` varchar(16) NOT NULL,
  `CUSERNAME` varchar(64) NOT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `PCONTENT` varchar(128) DEFAULT NULL,
  `MODELTYPE` varchar(2) NOT NULL,
  `SUBJECTID` varchar(32) NOT NULL,
  `CARDID` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_WTS_SUBJ_RE_WTS_SUBJ0935` (`SUBJECTID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_subject_userown 的数据：2,263 rows
/*!40000 ALTER TABLE `wts_subject_userown` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_subject_userown` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_subject_version 结构
CREATE TABLE IF NOT EXISTS `wts_subject_version` (
  `ID` varchar(32) NOT NULL,
  `CTIME` varchar(16) NOT NULL,
  `CUSERNAME` varchar(64) NOT NULL,
  `CUSER` varchar(32) NOT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `PCONTENT` varchar(128) DEFAULT NULL,
  `TIPSTR` varchar(256) NOT NULL,
  `TIPNOTE` text,
  `TIPTYPE` varchar(2) NOT NULL,
  `SUBJECTID` varchar(32) NOT NULL,
  `ANSWERED` varchar(2) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `SUBJECTID_SUBJECT_VERSION` (`SUBJECTID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_subject_version 的数据：~0 rows (大约)
/*!40000 ALTER TABLE `wts_subject_version` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_subject_version` ENABLE KEYS */;

-- 导出  表 wts-trunk.wts_task_mirror 结构
CREATE TABLE IF NOT EXISTS `wts_task_mirror` (
  `ID` varchar(32) NOT NULL,
  `CTIME` varchar(16) NOT NULL,
  `PSTATE` varchar(2) NOT NULL,
  `PCONTENT` varchar(128) DEFAULT NULL,
  `CARDID` varchar(32) DEFAULT NULL,
  `VERSIONID` varchar(32) DEFAULT NULL,
  `ANSWERID` varchar(32) DEFAULT NULL,
  `VAL` text,
  `TYPE` varchar(2) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- 正在导出表  wts-trunk.wts_task_mirror 的数据：0 rows
/*!40000 ALTER TABLE `wts_task_mirror` DISABLE KEYS */;
/*!40000 ALTER TABLE `wts_task_mirror` ENABLE KEYS */;

/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
