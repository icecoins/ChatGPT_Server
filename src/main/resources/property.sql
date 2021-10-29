SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

CREATE TABLE `property` (
  `username` varchar(50) NOT NULL,
  `coin` int(18) NOT NULL,
  `level` int(18) NOT NULL,
  `exp` int(18) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `property` (`username`, `coin`, `level`, `exp`) VALUES
('user1', 999, 1, 100),
('user2', 1, 10, 0),
('admin', 99999, 999, 0),
('uzi', 99999, 1000, 0),
('xiange', 100, 1, 5),
('timyond', 100, 1, 5);
COMMIT;

