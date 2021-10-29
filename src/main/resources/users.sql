SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

CREATE TABLE `users` (
  `id` int(18) NOT NULL,
  `username` varchar(50) NOT NULL,
  `password` varchar(50) NOT NULL,
  `info` text
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `users` (`id`, `username`, `password`, `info`) VALUES
(1, 'user1', 'psw1', 'user1 info'),
(2, 'user2', 'psw2', 'user2 info'),
(3, 'uzi', 'yyds', 'wuzi!\r\nyyds!'),
(4, 'clearlove', '4396', 'dream back to s7'),
(0, 'admin', 'admin', 'This is the admin, but he still can not beat uzi.'),
(5, 'xiange', 'xiange', '贤哥，几乎打败污渍的男人'),
(6, 'timyond', 'timyond', '波子，从宇宙射线中提取出玻色子的男人，2042年诺贝尔物理学奖获得者。');
COMMIT;

