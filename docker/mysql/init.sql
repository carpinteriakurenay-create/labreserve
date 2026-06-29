-- LabReserve 数据库初始化
CREATE DATABASE IF NOT EXISTS labreserve_dev;
GRANT ALL PRIVILEGES ON labreserve_dev.* TO 'labreserve'@'%';
FLUSH PRIVILEGES;
