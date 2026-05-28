-- Vote System tables
CREATE TABLE IF NOT EXISTS `vote_system_global` (
  `voteSite` tinyint(3) UNSIGNED NOT NULL,
  `lastRewardVotes` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`voteSite`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Default rows for up to 4 sites (ordinals 0-3)
INSERT IGNORE INTO `vote_system_global` (`voteSite`, `lastRewardVotes`) VALUES
(0, 0), (1, 0), (2, 0), (3, 0);

CREATE TABLE IF NOT EXISTS `vote_system_individual` (
  `voterIp`   varchar(40) NOT NULL,
  `voteSite`  tinyint(3) UNSIGNED NOT NULL,
  `rewardTime` bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`voterIp`, `voteSite`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
