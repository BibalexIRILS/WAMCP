use wamcpapp;
CREATE TABLE item_info (
  id int (20) NOT NULL  AUTO_INCREMENT,
  name varchar(60) NOT NULL,
  width int(20) NOT NULL,
  height int(20) NOT NULL,
  levels smallint(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY IX_ID_NAME (name)
) ENGINE=InnoDB AUTO_INCREMENT=126 DEFAULT CHARSET=cp1256;
