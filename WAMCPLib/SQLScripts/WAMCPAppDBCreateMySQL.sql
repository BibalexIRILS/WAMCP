-- User: `${username}`

DROP USER `${username}`;

CREATE USER `${username}`;

SET PASSWORD FOR `${username}` = PASSWORD('${password}');

-- use lowercase names, because of mysql case craziness
-- IMPORTANT: if running on linux make sure to set lowercase-table-names=1 in /etc/mysql.cnf 
-- Database: `${username}` --> 'wamcpapp';

DROP DATABASE `wamcpapp`; 

CREATE DATABASE `wamcpapp`
DEFAULT CHARACTER SET cp1256 COLLATE cp1256_general_ci;

GRANT INSERT, SELECT, UPDATE, DELETE ON `wamcpapp`.* to `${username}`; 

USE `wamcpapp`;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `USERNAME` varchar(50) NOT NULL,
  `PASSWORD` varchar(50) NOT NULL,
  `ENABLED` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`USERNAME`)
) ENGINE=InnoDB DEFAULT CHARSET=cp1256;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `authorities`
--

DROP TABLE IF EXISTS `authorities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `authorities` (
  `USERNAME` varchar(50) NOT NULL,
  `AUTHORITY` varchar(50) NOT NULL,
  UNIQUE KEY `IX_AUTH_USERNAME` (`USERNAME`,`AUTHORITY`),
  CONSTRAINT `FK_AUTHORITIES_USERS` FOREIGN KEY (`USERNAME`) REFERENCES `users` (`USERNAME`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=cp1256;
/*!40101 SET character_set_client = @saved_cs_client */;

-- Admin user with the default password

INSERT INTO users(username, password, enabled) VALUES("${default_username}", "${default_password}", 1);
INSERT INTO authorities VALUES("${default_username}", "ROLE_ADMIN");
INSERT INTO authorities VALUES("${default_username}", "ROLE_AUTHENTICATED");


---------------------------------------------  ACL --------------------------------------------

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";

-- --------------------------------------------------------


--
-- Table structure for table `acl_sid`
--

DROP TABLE IF EXISTS `acl_sid`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `acl_sid` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `PRINCIPAL` tinyint(1) NOT NULL,
  `SID` varchar(100) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UNIQUE_UK_1` (`PRINCIPAL`,`SID`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=cp1256;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `acl_class`
--

DROP TABLE IF EXISTS `acl_class`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `acl_class` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `CLASS` varchar(100) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UNIQUE_UK_2` (`CLASS`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=cp1256;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `acl_object_identity`
--

DROP TABLE IF EXISTS `acl_object_identity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `acl_object_identity` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `OBJECT_ID_CLASS` bigint(20) NOT NULL,
  `OBJECT_ID_IDENTITY` bigint(20) NOT NULL,
  `PARENT_OBJECT` bigint(20) DEFAULT NULL,
  `OWNER_SID` bigint(20) DEFAULT NULL,
  `ENTRIES_INHERITING` tinyint(1) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UNIQUE_UK_3` (`OBJECT_ID_CLASS`,`OBJECT_ID_IDENTITY`),
  KEY `OWNER_SID` (`OWNER_SID`),
  KEY `PARENT_OBJECT` (`PARENT_OBJECT`),
  CONSTRAINT `FK_OID_CLASS` FOREIGN KEY (`OBJECT_ID_CLASS`) REFERENCES `acl_class` (`ID`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `FK_OID_PARENTOID` FOREIGN KEY (`PARENT_OBJECT`) REFERENCES `acl_object_identity` (`ID`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `FK_OID_SID` FOREIGN KEY (`OWNER_SID`) REFERENCES `acl_sid` (`ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=266 DEFAULT CHARSET=cp1256;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `acl_entry`
--

DROP TABLE IF EXISTS `acl_entry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `acl_entry` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `ACL_OBJECT_IDENTITY` bigint(20) NOT NULL,
  `ACE_ORDER` int(11) NOT NULL,
  `SID` bigint(20) NOT NULL,
  `MASK` int(11) NOT NULL,
  `GRANTING` tinyint(1) NOT NULL,
  `AUDIT_SUCCESS` tinyint(1) NOT NULL,
  `AUDIT_FAILURE` tinyint(1) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UNIQUE_UK_4` (`ACL_OBJECT_IDENTITY`,`ACE_ORDER`),
  KEY `SID` (`SID`),
  CONSTRAINT `FK_ACE_OID` FOREIGN KEY (`ACL_OBJECT_IDENTITY`) REFERENCES `acl_object_identity` (`ID`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `FK_ACE_SID` FOREIGN KEY (`SID`) REFERENCES `acl_sid` (`ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=335 DEFAULT CHARSET=cp1256;
/*!40101 SET character_set_client = @saved_cs_client */;


---------------------- FILL THE SID WITH ROLES --------------------------
-- INSERT INTO `acl_sid`(`PRINCIPAL`,`SID`) VALUES(0,'ROLE_XYZ'); 


--
-- Table structure for table `wf_artifacts`
--

DROP TABLE IF EXISTS `wf_artifacts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `wf_artifacts` (
  `OBJECT_ID_IDENTITY` bigint(20) NOT NULL AUTO_INCREMENT,
  `ARTIFACT_NAME` varchar(100) NOT NULL,
  PRIMARY KEY (`OBJECT_ID_IDENTITY`),
  UNIQUE KEY `UK_ARTIFACT_ID` (`ARTIFACT_NAME`)
) ENGINE=InnoDB AUTO_INCREMENT=159 DEFAULT CHARSET=cp1256;
/*!40101 SET character_set_client = @saved_cs_client */;

-- MYSQL STILL has a problem with this constraint.. so let's live withouy it
-- ALTER TABLE wamcpusers.wf_artifacts
-- ADD CONSTRAINT FK_ARTIF_OID FOREIGN KEY (OBJECT_ID_IDENTITY) REFERENCES wamcpusers.acl_object_identity (OBJECT_ID_IDENTITY) ON UPDATE CASCADE ON DELETE RESTRICT;


INSERT INTO wf_artifacts (OBJECT_ID_IDENTITY,ARTIFACT_NAME) VALUES (1,'WFACL_CREATION_PROTECTION_ARTIFACT');

----------------------------------------------------------------------------

CREATE INDEX `IX_ACE_MASK` ON `acl_entry`(`MASK`); -- Bitmap index would be great here.. use it if MySQL supports it

---------------------------------------------------------------------------

--
-- Table structure for table `wamcpstorage_locks`
--

DROP TABLE IF EXISTS `wamcpstorage_locks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `wamcpstorage_locks` (
  `FILE_URLSTR` varchar(512) NOT NULL,
  `HOLDER` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`FILE_URLSTR`)
) ENGINE=InnoDB DEFAULT CHARSET=cp1256;
/*!40101 SET character_set_client = @saved_cs_client */;
