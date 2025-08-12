-- ===================================
-- 3. 사용자 기본 정보 생성 (user 테이블)
-- ===================================

INSERT INTO `user` (id, team_id, nickname, gender, birth_year, role, profile_img, introduction, posts_public, stats_public, attendance_records_public) VALUES

-- 한화 이글스 팬 (13명)
(1, 1, '대전의불새', 'MALE', 1992, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/hanwha_fan1.jpg', '한화 이글스 평생 팬! 대전구장 단골입니다 🔥', true, true, true),
(2, 1, '이글스러버', 'FEMALE', 1995, 'USER', NULL, '우승까지 함께 가요! 한화화이팅', true, false, true),
(3, 1, '한화20년', 'MALE', 1985, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/hanwha_fan3.jpg', '1985년생 한화팬. 20년째 응원 중입니다', true, true, false),
(31, 1, '대전야구왕', 'MALE', 1991, 'USER', NULL, '한화 응원 5년차! 대전볼파크 시즌권 보유', true, true, true),
(32, 1, '이글스퀸', 'FEMALE', 1994, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/hanwha_fan4.jpg', '94년생 한화팬. 매주 대전 직관!', true, true, true),
(33, 1, '한화골수팬', 'MALE', 1988, 'USER', NULL, '88년생 한화 철새팬. 승패 상관없이 응원', true, false, true),
(34, 1, '대전의여왕', 'FEMALE', 1993, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/hanwha_fan5.jpg', '한화 여성팬클럽 운영진입니다', true, true, false),
(35, 1, '이글스매니아', 'MALE', 1990, 'USER', NULL, '90년생 한화팬. 원정 경기도 따라감', true, true, true),
(36, 1, '한화사랑해', 'FEMALE', 1996, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/hanwha_fan6.jpg', '96년생 대학생. 한화 응원 열심히!', false, true, true),
(37, 1, '대전볼파크', 'MALE', 1987, 'USER', NULL, '87년생 한화팬. 구장 모든 자리 경험해봄', true, true, true),
(38, 1, '이글스홀릭', 'FEMALE', 1995, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/hanwha_fan7.jpg', '한화 경기 빠짐없이 챙겨봄', true, true, true),
(39, 1, '한화15년', 'MALE', 1989, 'USER', NULL, '2010년부터 한화 응원. 진짜 팬', true, false, true),
(40, 1, '대전토박이', 'FEMALE', 1992, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/hanwha_fan8.jpg', '대전 출신 한화 평생팬', true, true, false),

-- LG 트윈스 팬 (13명)
(4, 2, '잠실의왕', 'MALE', 1990, 'USER', NULL, 'LG 트윈스 우승 기대합니다! 잠실에서 만나요', true, true, true),
(5, 2, '트윈스여신', 'FEMALE', 1998, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/lg_fan2.jpg', '98년생 트윈스 팬💕 매 경기 열정 응원!', true, true, true),
(6, 2, 'LG매니아', 'MALE', 1987, 'ADMIN', 'https://bbatty-s3.s3.amazonaws.com/profiles/lg_admin.jpg', '관리자 계정. LG 트윈스 20년 팬', true, true, true),
(41, 2, '잠실황제', 'MALE', 1986, 'USER', NULL, 'LG 트윈스 20년 팬. 잠실의 터줏대감', true, true, true),
(42, 2, '트윈스공주', 'FEMALE', 1993, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/lg_fan3.jpg', '93년생 LG팬. 매 경기 열정 응원!', true, true, true),
(43, 2, 'LG골수팬', 'MALE', 1985, 'USER', NULL, '85년생 LG 철새팬. 30년째 응원', true, false, true),
(44, 2, '잠실의별', 'FEMALE', 1997, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/lg_fan4.jpg', '97년생 LG팬. 트윈스 사랑해요💕', true, true, false),
(45, 2, '트윈스독', 'MALE', 1991, 'USER', NULL, '91년생 LG팬. 원정도 따라다님', true, true, true),
(46, 2, 'LG여왕', 'FEMALE', 1994, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/lg_fan5.jpg', 'LG 여성팬 대표! 잠실에서 만나요', false, true, true),
(47, 2, '잠실마스터', 'MALE', 1988, 'USER', NULL, '88년생 LG팬. 잠실 구석구석 다 알아', true, true, true),
(48, 2, '트윈스홀릭', 'FEMALE', 1996, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/lg_fan6.jpg', '96년생 LG팬. 매주 잠실 출근', true, true, true),
(49, 2, 'LG12년', 'MALE', 1990, 'USER', NULL, '2013년부터 LG 응원. 진성팬', true, false, true),
(50, 2, '잠실의신', 'FEMALE', 1995, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/lg_fan7.jpg', 'LG 경기 놓치는 날이 없어요', true, true, false),

-- 롯데 자이언츠 팬 (13명)
(7, 3, '부산갈매기', 'FEMALE', 1993, 'USER', NULL, '부산 토박이 롯데팬! 사직에서 매일 응원해요', true, true, true),
(8, 3, '자이언츠킹', 'MALE', 1988, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/lotte_fan2.jpg', '롯데 자이언츠 영원한 1등! 사직구장이 제 집', true, false, true),
(9, 3, '사직단골', 'MALE', 1996, 'USER', NULL, '96년생 롯데팬. 군대에서도 롯데 응원했어요', false, true, true),
(51, 3, '사직황제', 'MALE', 1987, 'USER', NULL, '롯데 자이언츠 20년 팬. 사직의 전설', true, true, true),
(52, 3, '부산공주', 'FEMALE', 1992, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/lotte_fan3.jpg', '부산 토박이 롯데팬. 매경기 참석!', true, true, true),
(53, 3, '롯데골수', 'MALE', 1984, 'USER', NULL, '84년생 롯데 철새팬. 25년째 응원', true, false, true),
(54, 3, '사직의별', 'FEMALE', 1998, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/lotte_fan4.jpg', '98년생 롯데팬. 자이언츠 사랑해💙', true, true, false),
(55, 3, '자이언츠독', 'MALE', 1989, 'USER', NULL, '89년생 롯데팬. 원정 경기도 따라감', true, true, true),
(56, 3, '부산여왕', 'FEMALE', 1993, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/lotte_fan5.jpg', '롯데 여성팬클럽 회장입니다', false, true, true),
(57, 3, '사직마스터', 'MALE', 1986, 'USER', NULL, '86년생 롯데팬. 사직구장 터줏대감', true, true, true),
(58, 3, '롯데홀릭', 'FEMALE', 1995, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/lotte_fan6.jpg', '95년생 롯데팬. 매주 사직 출근', true, true, true),
(59, 3, '자이언츠10년', 'MALE', 1991, 'USER', NULL, '2015년부터 롯데 응원. 진성팬', true, false, true),
(60, 3, '부산의신', 'FEMALE', 1994, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/lotte_fan7.jpg', '롯데 경기는 절대 안 놓쳐요', true, true, false),

-- KT 위즈 팬 (13명)
(10, 4, '수원마법사', 'FEMALE', 1994, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/kt_fan1.jpg', 'KT 위즈 창단 때부터 함께! 수원 최고💙', true, true, true),
(11, 4, '위즈덕후', 'MALE', 1991, 'USER', NULL, '2015년부터 시작된 KT 위즈 사랑', true, true, false),
(12, 4, 'KT야구광', 'MALE', 1989, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/kt_fan3.jpg', '수원KT위즈파크 연간회원. 매 경기 참석!', true, true, true),
(61, 4, '수원황제', 'MALE', 1988, 'USER', NULL, 'KT 위즈 창단부터 함께한 원년멤버', true, true, true),
(62, 4, '위즈공주', 'FEMALE', 1991, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/kt_fan4.jpg', '91년생 KT팬. 수원 매경기 참석!', true, true, true),
(63, 4, 'KT골수팬', 'MALE', 1985, 'USER', NULL, '85년생 KT 철새팬. 10년째 응원', true, false, true),
(64, 4, '수원의별', 'FEMALE', 1997, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/kt_fan5.jpg', '97년생 KT팬. 위즈 사랑해요💙', true, true, false),
(65, 4, '위즈독', 'MALE', 1990, 'USER', NULL, '90년생 KT팬. 원정도 따라다님', true, true, true),
(66, 4, 'KT여왕', 'FEMALE', 1994, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/kt_fan6.jpg', 'KT 여성팬 대표! 수원에서 만나요', false, true, true),
(67, 4, '수원마스터', 'MALE', 1987, 'USER', NULL, '87년생 KT팬. 수원KT위즈파크 터줏대감', true, true, true),
(68, 4, '위즈홀릭', 'FEMALE', 1996, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/kt_fan7.jpg', '96년생 KT팬. 매주 수원 출근', true, true, true),
(69, 4, 'KT8년', 'MALE', 1992, 'USER', NULL, '2017년부터 KT 응원. 진성팬', true, false, true),
(70, 4, '수원의신', 'FEMALE', 1995, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/kt_fan8.jpg', 'KT 경기는 절대 안 놓쳐요', true, true, false),

-- 삼성 라이온즈 팬 (13명)
(13, 5, '대구사자', 'MALE', 1986, 'USER', NULL, '삼성 라이온즈 황금기를 함께한 30대 팬', true, true, true),
(14, 5, '라이온킹', 'FEMALE', 1997, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/samsung_fan2.jpg', '97년생 삼성팬! 대구에서 평생 응원할게요', true, true, true),
(15, 5, '삼성10년', 'MALE', 1992, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/samsung_fan3.jpg', '2014년부터 삼성 응원. 라이온즈 화이팅!', true, false, true),
(71, 5, '대구황제', 'MALE', 1983, 'USER', NULL, '삼성 라이온즈 황금기를 함께한 전설', true, true, true),
(72, 5, '라이온공주', 'FEMALE', 1990, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/samsung_fan4.jpg', '90년생 삼성팬. 대구 매경기 참석!', true, true, true),
(73, 5, '삼성골수', 'MALE', 1984, 'USER', NULL, '84년생 삼성 철새팬. 30년째 응원', true, false, true),
(74, 5, '대구의별', 'FEMALE', 1998, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/samsung_fan5.jpg', '98년생 삼성팬. 라이온즈 사랑해🦁', true, true, false),
(75, 5, '라이온독', 'MALE', 1989, 'USER', NULL, '89년생 삼성팬. 원정도 따라다님', true, true, true),
(76, 5, '삼성여왕', 'FEMALE', 1993, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/samsung_fan6.jpg', '삼성 여성팬클럽 운영진입니다', false, true, true),
(77, 5, '대구마스터', 'MALE', 1986, 'USER', NULL, '86년생 삼성팬. 대구라이온즈파크 터줏대감', true, true, true),
(78, 5, '삼성홀릭', 'FEMALE', 1995, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/samsung_fan7.jpg', '95년생 삼성팬. 매주 대구 출근', true, true, true),
(79, 5, '라이온15년', 'MALE', 1991, 'USER', NULL, '2010년부터 삼성 응원. 진성팬', true, false, true),
(80, 5, '대구의신', 'FEMALE', 1994, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/samsung_fan8.jpg', '삼성 경기는 절대 안 놓쳐요', true, true, false),

-- KIA 타이거즈 팬 (13명)
(16, 6, '광주호랑이', 'FEMALE', 1990, 'USER', NULL, 'KIA 타이거즈 평생팬! 광주챔스 매일 가요', true, true, true),
(17, 6, '타이거스독', 'MALE', 1984, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/kia_fan2.jpg', '84년생 KIA 골수팬. 우승 기대합니다', true, true, true),
(18, 6, 'KIA사랑', 'FEMALE', 1999, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/kia_fan3.jpg', '99년생 대학생 KIA팬💛 응원 열심히 해요', false, true, true),
(81, 6, '광주황제', 'MALE', 1982, 'USER', NULL, 'KIA 타이거즈 30년 팬. 광주의 전설', true, true, true),
(82, 6, '타이거공주', 'FEMALE', 1989, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/kia_fan4.jpg', '89년생 KIA팬. 광주 매경기 참석!', true, true, true),
(83, 6, 'KIA골수', 'MALE', 1985, 'USER', NULL, '85년생 KIA 철새팬. 25년째 응원', true, false, true),
(84, 6, '광주의별', 'FEMALE', 1997, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/kia_fan5.jpg', '97년생 KIA팬. 타이거즈 사랑해🐅', true, true, false),
(85, 6, '타이거독', 'MALE', 1988, 'USER', NULL, '88년생 KIA팬. 원정도 따라다님', true, true, true),
(86, 6, 'KIA여왕', 'FEMALE', 1992, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/kia_fan6.jpg', 'KIA 여성팬 대표! 광주에서 만나요', false, true, true),
(87, 6, '광주마스터', 'MALE', 1987, 'USER', NULL, '87년생 KIA팬. 광주챔피언스필드 터줏대감', true, true, true),
(88, 6, '타이거홀릭', 'FEMALE', 1996, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/kia_fan7.jpg', '96년생 KIA팬. 매주 광주 출근', true, true, true),
(89, 6, 'KIA12년', 'MALE', 1991, 'USER', NULL, '2013년부터 KIA 응원. 진성팬', true, false, true),
(90, 6, '광주의신', 'FEMALE', 1993, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/kia_fan8.jpg', 'KIA 경기는 절대 안 놓쳐요', true, true, false),

-- SSG 랜더스 팬 (13명)
(19, 7, '인천바다', 'MALE', 1987, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/ssg_fan1.jpg', 'SK부터 SSG까지! 인천야구 20년 역사', true, true, true),
(20, 7, '랜더스홀릭', 'FEMALE', 1995, 'USER', NULL, 'SSG 랜더스 신생팀 응원! 인천에서 만나요', true, true, false),
(21, 7, 'SSG파이팅', 'MALE', 1993, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/ssg_fan3.jpg', '인천 토박이 SSG팬. 문학경기장 최고!', true, false, true),
(91, 7, '인천황제', 'MALE', 1986, 'USER', NULL, 'SK부터 SSG까지! 인천야구 25년 역사', true, true, true),
(92, 7, '랜더스공주', 'FEMALE', 1990, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/ssg_fan4.jpg', '90년생 SSG팬. 인천 매경기 참석!', true, true, true),
(93, 7, 'SSG골수', 'MALE', 1984, 'USER', NULL, '84년생 SSG 철새팬. 20년째 응원', true, false, true),
(94, 7, '인천의별', 'FEMALE', 1998, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/ssg_fan5.jpg', '98년생 SSG팬. 랜더스 사랑해💚', true, true, false),
(95, 7, '랜더스독', 'MALE', 1989, 'USER', NULL, '89년생 SSG팬. 원정도 따라다님', true, true, true),
(96, 7, 'SSG여왕', 'FEMALE', 1993, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/ssg_fan6.jpg', 'SSG 여성팬클럽 운영진입니다', false, true, true),
(97, 7, '인천마스터', 'MALE', 1987, 'USER', NULL, '87년생 SSG팬. 인천SSG랜더스필드 터줏대감', true, true, true),
(98, 7, '랜더스홀릭', 'FEMALE', 1995, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/ssg_fan7.jpg', '95년생 SSG팬. 매주 인천 출근', true, true, true),
(99, 7, 'SSG8년', 'MALE', 1992, 'USER', NULL, '2017년부터 SSG 응원. 진성팬', true, false, true),
(100, 7, '인천의신', 'FEMALE', 1994, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/ssg_fan8.jpg', 'SSG 경기는 절대 안 놓쳐요', true, true, false),

-- 두산 베어스 팬 (13명)
(25, 9, '잠실곰돌이', 'FEMALE', 1994, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/doosan_fan1.jpg', '두산 베어스 평생팬! 잠실이 제 놀이터', true, true, true),
(26, 9, '베어스킹', 'MALE', 1983, 'USER', NULL, '83년생 두산 골수팬. 30년째 응원 중', true, false, true),
(27, 9, '곰돌이사랑', 'FEMALE', 1998, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/doosan_fan3.jpg', '98년생 대학생. 두산 베어스 사랑해요💙', true, true, true),
(111, 9, '베어스황제', 'MALE', 1984, 'USER', NULL, '두산 베어스 30년 팬. 잠실의 전설', true, true, true),
(112, 9, '곰돌이공주', 'FEMALE', 1988, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/doosan_fan4.jpg', '88년생 두산팬. 잠실 매경기 참석!', true, true, true),
(113, 9, '두산골수', 'MALE', 1982, 'USER', NULL, '82년생 두산 철새팬. 35년째 응원', true, false, true),
(114, 9, '잠실의별', 'FEMALE', 1996, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/doosan_fan5.jpg', '96년생 두산팬. 베어스 사랑해🐻', true, true, false),
(115, 9, '베어스독', 'MALE', 1987, 'USER', NULL, '87년생 두산팬. 원정도 따라다님', true, true, true),
(116, 9, '두산여왕', 'FEMALE', 1991, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/doosan_fan6.jpg', '두산 여성팬클럽 운영진입니다', false, true, true),
(117, 9, '잠실마스터', 'MALE', 1986, 'USER', NULL, '86년생 두산팬. 잠실야구장 터줏대감', true, true, true),
(118, 9, '베어스홀릭', 'FEMALE', 1995, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/doosan_fan7.jpg', '95년생 두산팬. 매주 잠실 출근', true, true, true),
(119, 9, '두산15년', 'MALE', 1990, 'USER', NULL, '2010년부터 두산 응원. 진성팬', true, false, true),
(120, 9, '곰돌이신', 'FEMALE', 1993, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/doosan_fan8.jpg', '두산 경기는 절대 안 놓쳐요', true, true, false),

-- 키움 히어로즈 팬 (13명)
(28, 10, '고척영웅', 'MALE', 1989, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/kiwoom_fan1.jpg', '키움 히어로즈 고척돔 단골! 영원한 팬', true, true, true),
(29, 10, '히어로덕후', 'FEMALE', 1992, 'USER', NULL, '넥센부터 키움까지! 10년 넘게 응원', true, true, false),
(30, 10, '키움사랑', 'MALE', 1997, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/kiwoom_fan3.jpg', '97년생 키움팬. 고척스카이돔 최고!', false, true, true),
(121, 10, '고척황제', 'MALE', 1985, 'USER', NULL, '넥센부터 키움까지! 15년 역사 함께', true, true, true),
(122, 10, '히어로공주', 'FEMALE', 1989, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/kiwoom_fan4.jpg', '89년생 키움팬. 고척 매경기 참석!', true, true, true),
(123, 10, '키움골수', 'MALE', 1983, 'USER', NULL, '83년생 키움 철새팬. 15년째 응원', true, false, true),
(124, 10, '고척의별', 'FEMALE', 1997, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/kiwoom_fan5.jpg', '97년생 키움팬. 히어로즈 사랑해🦸', true, true, false),
(125, 10, '히어로독', 'MALE', 1988, 'USER', NULL, '88년생 키움팬. 원정도 따라다님', true, true, true),
(126, 10, '키움여왕', 'FEMALE', 1992, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/kiwoom_fan6.jpg', '키움 여성팬 대표! 고척에서 만나요', false, true, true),
(127, 10, '고척마스터', 'MALE', 1987, 'USER', NULL, '87년생 키움팬. 고척스카이돔 터줏대감', true, true, true),
(128, 10, '히어로홀릭', 'FEMALE', 1996, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/kiwoom_fan7.jpg', '96년생 키움팬. 매주 고척 출근', true, true, true),
(129, 10, '키움12년', 'MALE', 1991, 'USER', NULL, '2013년부터 키움 응원. 진성팬', true, false, true),
(130, 10, '고척의신', 'FEMALE', 1993, 'USER', 'https://bbatty-s3.s3.amazonaws.com/profiles/kiwoom_fan8.jpg', '키움 경기는 절대 안 놓쳐요', true, true, false);

-- ===================================
-- 4. 사용자 상세 정보 생성 (user_info 테이블)
-- ===================================

INSERT INTO `user_info` (user_id, kakao_id, email) VALUES
                                                       (1, '2401567890', 'hanwha.fan1@kakao.com'),
                                                       (2, '2401567891', 'eagles.lover@gmail.com'),
                                                       (3, '2401567892', 'hanwha20year@naver.com'),
                                                       (4, '2401567893', 'jamsil.king@kakao.com'),
                                                       (5, '2401567894', 'twins.goddess@gmail.com'),
                                                       (6, '2401567895', 'lg.admin@ssafy.com'),
                                                       (7, '2401567896', 'busan.seagull@kakao.com'),
                                                       (8, '2401567897', 'giants.king@gmail.com'),
                                                       (9, '2401567898', 'sajik.regular@naver.com'),
                                                       (10, '2401567899', 'suwon.wizard@kakao.com'),
                                                       (11, '2401567900', 'wiz.geek@gmail.com'),
                                                       (12, '2401567901', 'kt.baseball@naver.com'),
                                                       (13, '2401567902', 'daegu.lion@kakao.com'),
                                                       (14, '2401567903', 'lion.king@gmail.com'),
                                                       (15, '2401567904', 'samsung10@naver.com'),
                                                       (16, '2401567905', 'gwangju.tiger@kakao.com'),
                                                       (17, '2401567906', 'tigers.fan@gmail.com'),
                                                       (18, '2401567907', 'kia.love@naver.com'),
                                                       (19, '2401567908', 'incheon.sea@kakao.com'),
                                                       (20, '2401567909', 'landers.fan@gmail.com'),
                                                       (21, '2401567910', 'ssg.fighting@naver.com'),
                                                       (25, '2401567914', 'jamsil.bear@kakao.com'),
                                                       (26, '2401567915', 'bears.king@gmail.com'),
                                                       (27, '2401567916', 'bear.love@naver.com'),
                                                       (28, '2401567917', 'gocheok.hero@kakao.com'),
                                                       (29, '2401567918', 'heroes.fan@gmail.com'),
                                                       (30, '2401567919', 'kiwoom.love@naver.com'),

-- 추가 사용자 상세 정보 (ID 31~130)
                                                       (31, '2401567920', 'hanwha.king@kakao.com'),
                                                       (32, '2401567921', 'eagles.queen@gmail.com'),
                                                       (33, '2401567922', 'hanwha.core@naver.com'),
                                                       (34, '2401567923', 'daejeon.queen@kakao.com'),
                                                       (35, '2401567924', 'eagles.mania@gmail.com'),
                                                       (36, '2401567925', 'hanwha.love2@naver.com'),
                                                       (37, '2401567926', 'daejeon.park@kakao.com'),
                                                       (38, '2401567927', 'eagles.holic@gmail.com'),
                                                       (39, '2401567928', 'hanwha15@naver.com'),
                                                       (40, '2401567929', 'daejeon.native@kakao.com'),

                                                       (41, '2401567930', 'jamsil.emperor@gmail.com'),
                                                       (42, '2401567931', 'twins.princess@naver.com'),
                                                       (43, '2401567932', 'lg.core@kakao.com'),
                                                       (44, '2401567933', 'jamsil.star@gmail.com'),
                                                       (45, '2401567934', 'twins.dog@naver.com'),
                                                       (46, '2401567935', 'lg.queen@kakao.com'),
                                                       (47, '2401567936', 'jamsil.master@gmail.com'),
                                                       (48, '2401567937', 'twins.holic@naver.com'),
                                                       (49, '2401567938', 'lg12year@kakao.com'),
                                                       (50, '2401567939', 'jamsil.god@gmail.com'),

                                                       (51, '2401567940', 'sajik.emperor@naver.com'),
                                                       (52, '2401567941', 'busan.princess@kakao.com'),
                                                       (53, '2401567942', 'lotte.core@gmail.com'),
                                                       (54, '2401567943', 'sajik.star@naver.com'),
                                                       (55, '2401567944', 'giants.dog@kakao.com'),
                                                       (56, '2401567945', 'busan.queen@gmail.com'),
                                                       (57, '2401567946', 'sajik.master@naver.com'),
                                                       (58, '2401567947', 'lotte.holic@kakao.com'),
                                                       (59, '2401567948', 'giants10@gmail.com'),
                                                       (60, '2401567949', 'busan.god@naver.com'),

                                                       (61, '2401567950', 'suwon.emperor@kakao.com'),
                                                       (62, '2401567951', 'wiz.princess@gmail.com'),
                                                       (63, '2401567952', 'kt.core@naver.com'),
                                                       (64, '2401567953', 'suwon.star@kakao.com'),
                                                       (65, '2401567954', 'wiz.dog@gmail.com'),
                                                       (66, '2401567955', 'kt.queen@naver.com'),
                                                       (67, '2401567956', 'suwon.master@kakao.com'),
                                                       (68, '2401567957', 'wiz.holic@gmail.com'),
                                                       (69, '2401567958', 'kt8year@naver.com'),
                                                       (70, '2401567959', 'suwon.god@kakao.com'),

                                                       (71, '2401567960', 'daegu.emperor@gmail.com'),
                                                       (72, '2401567961', 'lion.princess@naver.com'),
                                                       (73, '2401567962', 'samsung.core@kakao.com'),
                                                       (74, '2401567963', 'daegu.star@gmail.com'),
                                                       (75, '2401567964', 'lion.dog@naver.com'),
                                                       (76, '2401567965', 'samsung.queen@kakao.com'),
                                                       (77, '2401567966', 'daegu.master@gmail.com'),
                                                       (78, '2401567967', 'samsung.holic@naver.com'),
                                                       (79, '2401567968', 'lion15@kakao.com'),
                                                       (80, '2401567969', 'daegu.god@gmail.com'),

                                                       (81, '2401567970', 'gwangju.emperor@naver.com'),
                                                       (82, '2401567971', 'tiger.princess@kakao.com'),
                                                       (83, '2401567972', 'kia.core@gmail.com'),
                                                       (84, '2401567973', 'gwangju.star@naver.com'),
                                                       (85, '2401567974', 'tiger.dog@kakao.com'),
                                                       (86, '2401567975', 'kia.queen@gmail.com'),
                                                       (87, '2401567976', 'gwangju.master@naver.com'),
                                                       (88, '2401567977', 'tiger.holic@kakao.com'),
                                                       (89, '2401567978', 'kia12year@gmail.com'),
                                                       (90, '2401567979', 'gwangju.god@naver.com'),

                                                       (91, '2401567980', 'incheon.emperor@kakao.com'),
                                                       (92, '2401567981', 'landers.princess@gmail.com'),
                                                       (93, '2401567982', 'ssg.core@naver.com'),
                                                       (94, '2401567983', 'incheon.star@kakao.com'),
                                                       (95, '2401567984', 'landers.dog@gmail.com'),
                                                       (96, '2401567985', 'ssg.queen@naver.com'),
                                                       (97, '2401567986', 'incheon.master@kakao.com'),
                                                       (98, '2401567987', 'landers.holic@gmail.com'),
                                                       (99, '2401567988', 'ssg8year@naver.com'),
                                                       (100, '2401567989', 'incheon.god@kakao.com'),

                                                       (111, '2401568000', 'bears.emperor@naver.com'),
                                                       (112, '2401568001', 'doosan.princess@kakao.com'),
                                                       (113, '2401568002', 'bears.core@gmail.com'),
                                                       (114, '2401568003', 'jamsil.star2@naver.com'),
                                                       (115, '2401568004', 'bears.dog@kakao.com'),
                                                       (116, '2401568005', 'doosan.queen@gmail.com'),
                                                       (117, '2401568006', 'jamsil.master2@naver.com'),
                                                       (118, '2401568007', 'bears.holic@kakao.com'),
                                                       (119, '2401568008', 'doosan15@gmail.com'),
                                                       (120, '2401568009', 'bears.god@naver.com'),

                                                       (121, '2401568010', 'gocheok.emperor@kakao.com'),
                                                       (122, '2401568011', 'heroes.princess@gmail.com'),
                                                       (123, '2401568012', 'kiwoom.core@naver.com'),
                                                       (124, '2401568013', 'gocheok.star@kakao.com'),
                                                       (125, '2401568014', 'heroes.dog@gmail.com'),
                                                       (126, '2401568015', 'kiwoom.queen@naver.com'),
                                                       (127, '2401568016', 'gocheok.master@kakao.com'),
                                                       (128, '2401568017', 'heroes.holic@gmail.com'),
                                                       (129, '2401568018', 'kiwoom12@naver.com'),
                                                       (130, '2401568019', 'gocheok.god@kakao.com');

-- ===================================
-- 5. 사용자 직관 기록 생성 (user_attended 테이블)
-- ===================================

-- 각 팀별 사용자들에게 10~30경기 직관 기록 배정
-- 모든 직관 기록은 자신의 응원팀 경기만 포함 (타팀 경기 직관 불가)

-- 한화 이글스 팬들 (ID 1,2,3,31~40) - 13명
INSERT INTO user_attended (user_id, game_id)
SELECT 1, g.id FROM game g WHERE (g.home_team_id = 1 OR g.away_team_id = 1) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 30;

INSERT INTO user_attended (user_id, game_id)
SELECT 2, g.id FROM game g WHERE (g.home_team_id = 1 OR g.away_team_id = 1) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 15;

INSERT INTO user_attended (user_id, game_id)
SELECT 3, g.id FROM game g WHERE (g.home_team_id = 1 OR g.away_team_id = 1) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 12;

INSERT INTO user_attended (user_id, game_id)
SELECT 31, g.id FROM game g WHERE (g.home_team_id = 1 OR g.away_team_id = 1) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 25;

INSERT INTO user_attended (user_id, game_id)
SELECT 32, g.id FROM game g WHERE (g.home_team_id = 1 OR g.away_team_id = 1) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 22;

INSERT INTO user_attended (user_id, game_id)
SELECT 33, g.id FROM game g WHERE (g.home_team_id = 1 OR g.away_team_id = 1) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 18;

INSERT INTO user_attended (user_id, game_id)
SELECT 34, g.id FROM game g WHERE (g.home_team_id = 1 OR g.away_team_id = 1) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 20;

INSERT INTO user_attended (user_id, game_id)
SELECT 35, g.id FROM game g WHERE (g.home_team_id = 1 OR g.away_team_id = 1) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 16;

INSERT INTO user_attended (user_id, game_id)
SELECT 36, g.id FROM game g WHERE (g.home_team_id = 1 OR g.away_team_id = 1) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 14;

INSERT INTO user_attended (user_id, game_id)
SELECT 37, g.id FROM game g WHERE (g.home_team_id = 1 OR g.away_team_id = 1) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 23;

INSERT INTO user_attended (user_id, game_id)
SELECT 38, g.id FROM game g WHERE (g.home_team_id = 1 OR g.away_team_id = 1) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 19;

INSERT INTO user_attended (user_id, game_id)
SELECT 39, g.id FROM game g WHERE (g.home_team_id = 1 OR g.away_team_id = 1) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 17;

INSERT INTO user_attended (user_id, game_id)
SELECT 40, g.id FROM game g WHERE (g.home_team_id = 1 OR g.away_team_id = 1) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 21;

-- LG 트윈스 팬들 (ID 4,5,6,41~50) - 13명
INSERT INTO user_attended (user_id, game_id)
SELECT 4, g.id FROM game g WHERE (g.home_team_id = 2 OR g.away_team_id = 2) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 18;

INSERT INTO user_attended (user_id, game_id)
SELECT 5, g.id FROM game g WHERE (g.home_team_id = 2 OR g.away_team_id = 2) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 13;

INSERT INTO user_attended (user_id, game_id)
SELECT 6, g.id FROM game g WHERE (g.home_team_id = 2 OR g.away_team_id = 2) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 28;

INSERT INTO user_attended (user_id, game_id)
SELECT 41, g.id FROM game g WHERE (g.home_team_id = 2 OR g.away_team_id = 2) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 24;

INSERT INTO user_attended (user_id, game_id)
SELECT 42, g.id FROM game g WHERE (g.home_team_id = 2 OR g.away_team_id = 2) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 21;

INSERT INTO user_attended (user_id, game_id)
SELECT 43, g.id FROM game g WHERE (g.home_team_id = 2 OR g.away_team_id = 2) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 19;

INSERT INTO user_attended (user_id, game_id)
SELECT 44, g.id FROM game g WHERE (g.home_team_id = 2 OR g.away_team_id = 2) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 16;

INSERT INTO user_attended (user_id, game_id)
SELECT 45, g.id FROM game g WHERE (g.home_team_id = 2 OR g.away_team_id = 2) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 22;

INSERT INTO user_attended (user_id, game_id)
SELECT 46, g.id FROM game g WHERE (g.home_team_id = 2 OR g.away_team_id = 2) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 15;

INSERT INTO user_attended (user_id, game_id)
SELECT 47, g.id FROM game g WHERE (g.home_team_id = 2 OR g.away_team_id = 2) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 25;

INSERT INTO user_attended (user_id, game_id)
SELECT 48, g.id FROM game g WHERE (g.home_team_id = 2 OR g.away_team_id = 2) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 20;

INSERT INTO user_attended (user_id, game_id)
SELECT 49, g.id FROM game g WHERE (g.home_team_id = 2 OR g.away_team_id = 2) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 17;

INSERT INTO user_attended (user_id, game_id)
SELECT 50, g.id FROM game g WHERE (g.home_team_id = 2 OR g.away_team_id = 2) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 23;

-- 롯데 자이언츠 팬들 (ID 7,8,9,51~60) - 13명
INSERT INTO user_attended (user_id, game_id)
SELECT 7, g.id FROM game g WHERE (g.home_team_id = 3 OR g.away_team_id = 3) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 11;
INSERT INTO user_attended (user_id, game_id)
SELECT 8, g.id FROM game g WHERE (g.home_team_id = 3 OR g.away_team_id = 3) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 14;
INSERT INTO user_attended (user_id, game_id)
SELECT 9, g.id FROM game g WHERE (g.home_team_id = 3 OR g.away_team_id = 3) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 10;
INSERT INTO user_attended (user_id, game_id)
SELECT 51, g.id FROM game g WHERE (g.home_team_id = 3 OR g.away_team_id = 3) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 26;
INSERT INTO user_attended (user_id, game_id)
SELECT 52, g.id FROM game g WHERE (g.home_team_id = 3 OR g.away_team_id = 3) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 23;
INSERT INTO user_attended (user_id, game_id)
SELECT 53, g.id FROM game g WHERE (g.home_team_id = 3 OR g.away_team_id = 3) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 20;
INSERT INTO user_attended (user_id, game_id)
SELECT 54, g.id FROM game g WHERE (g.home_team_id = 3 OR g.away_team_id = 3) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 18;
INSERT INTO user_attended (user_id, game_id)
SELECT 55, g.id FROM game g WHERE (g.home_team_id = 3 OR g.away_team_id = 3) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 21;
INSERT INTO user_attended (user_id, game_id)
SELECT 56, g.id FROM game g WHERE (g.home_team_id = 3 OR g.away_team_id = 3) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 16;
INSERT INTO user_attended (user_id, game_id)
SELECT 57, g.id FROM game g WHERE (g.home_team_id = 3 OR g.away_team_id = 3) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 24;
INSERT INTO user_attended (user_id, game_id)
SELECT 58, g.id FROM game g WHERE (g.home_team_id = 3 OR g.away_team_id = 3) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 19;
INSERT INTO user_attended (user_id, game_id)
SELECT 59, g.id FROM game g WHERE (g.home_team_id = 3 OR g.away_team_id = 3) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 17;
INSERT INTO user_attended (user_id, game_id)
SELECT 60, g.id FROM game g WHERE (g.home_team_id = 3 OR g.away_team_id = 3) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 22;
-- KT 위즈 팬들 (ID 10,11,12,61~70) - 13명
INSERT INTO user_attended (user_id, game_id)
SELECT 10, g.id FROM game g WHERE (g.home_team_id = 4 OR g.away_team_id = 4) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 16;
INSERT INTO user_attended (user_id, game_id)
SELECT 11, g.id FROM game g WHERE (g.home_team_id = 4 OR g.away_team_id = 4) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 12;
INSERT INTO user_attended (user_id, game_id)
SELECT 12, g.id FROM game g WHERE (g.home_team_id = 4 OR g.away_team_id = 4) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 15;
INSERT INTO user_attended (user_id, game_id)
SELECT 61, g.id FROM game g WHERE (g.home_team_id = 4 OR g.away_team_id = 4) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 25;
INSERT INTO user_attended (user_id, game_id)
SELECT 62, g.id FROM game g WHERE (g.home_team_id = 4 OR g.away_team_id = 4) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 22;
INSERT INTO user_attended (user_id, game_id)
SELECT 63, g.id FROM game g WHERE (g.home_team_id = 4 OR g.away_team_id = 4) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 19;
INSERT INTO user_attended (user_id, game_id)
SELECT 64, g.id FROM game g WHERE (g.home_team_id = 4 OR g.away_team_id = 4) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 17;
INSERT INTO user_attended (user_id, game_id)
SELECT 65, g.id FROM game g WHERE (g.home_team_id = 4 OR g.away_team_id = 4) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 20;
INSERT INTO user_attended (user_id, game_id)
SELECT 66, g.id FROM game g WHERE (g.home_team_id = 4 OR g.away_team_id = 4) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 14;
INSERT INTO user_attended (user_id, game_id)
SELECT 67, g.id FROM game g WHERE (g.home_team_id = 4 OR g.away_team_id = 4) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 23;
INSERT INTO user_attended (user_id, game_id)
SELECT 68, g.id FROM game g WHERE (g.home_team_id = 4 OR g.away_team_id = 4) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 18;
INSERT INTO user_attended (user_id, game_id)
SELECT 69, g.id FROM game g WHERE (g.home_team_id = 4 OR g.away_team_id = 4) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 21;
INSERT INTO user_attended (user_id, game_id)
SELECT 70, g.id FROM game g WHERE (g.home_team_id = 4 OR g.away_team_id = 4) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 24;

-- 삼성 라이온즈 팬들 (ID 13,14,15,71~80) - 13명
INSERT INTO user_attended (user_id, game_id)
SELECT 13, g.id FROM game g WHERE (g.home_team_id = 5 OR g.away_team_id = 5) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 13;
INSERT INTO user_attended (user_id, game_id)
SELECT 14, g.id FROM game g WHERE (g.home_team_id = 5 OR g.away_team_id = 5) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 11;
INSERT INTO user_attended (user_id, game_id)
SELECT 15, g.id FROM game g WHERE (g.home_team_id = 5 OR g.away_team_id = 5) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 14;
INSERT INTO user_attended (user_id, game_id)
SELECT 71, g.id FROM game g WHERE (g.home_team_id = 5 OR g.away_team_id = 5) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 27;
INSERT INTO user_attended (user_id, game_id)
SELECT 72, g.id FROM game g WHERE (g.home_team_id = 5 OR g.away_team_id = 5) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 24;
INSERT INTO user_attended (user_id, game_id)
SELECT 73, g.id FROM game g WHERE (g.home_team_id = 5 OR g.away_team_id = 5) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 21;
INSERT INTO user_attended (user_id, game_id)
SELECT 74, g.id FROM game g WHERE (g.home_team_id = 5 OR g.away_team_id = 5) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 18;
INSERT INTO user_attended (user_id, game_id)
SELECT 75, g.id FROM game g WHERE (g.home_team_id = 5 OR g.away_team_id = 5) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 22;
INSERT INTO user_attended (user_id, game_id)
SELECT 76, g.id FROM game g WHERE (g.home_team_id = 5 OR g.away_team_id = 5) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 15;
INSERT INTO user_attended (user_id, game_id)
SELECT 77, g.id FROM game g WHERE (g.home_team_id = 5 OR g.away_team_id = 5) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 25;
INSERT INTO user_attended (user_id, game_id)
SELECT 78, g.id FROM game g WHERE (g.home_team_id = 5 OR g.away_team_id = 5) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 19;
INSERT INTO user_attended (user_id, game_id)
SELECT 79, g.id FROM game g WHERE (g.home_team_id = 5 OR g.away_team_id = 5) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 16;
INSERT INTO user_attended (user_id, game_id)
SELECT 80, g.id FROM game g WHERE (g.home_team_id = 5 OR g.away_team_id = 5) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 23;

-- KIA 타이거즈 팬들 (ID 16,17,18,81~90) - 13명
INSERT INTO user_attended (user_id, game_id)
SELECT 16, g.id FROM game g WHERE (g.home_team_id = 6 OR g.away_team_id = 6) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 17;
INSERT INTO user_attended (user_id, game_id)
SELECT 17, g.id FROM game g WHERE (g.home_team_id = 6 OR g.away_team_id = 6) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 14;
INSERT INTO user_attended (user_id, game_id)
SELECT 18, g.id FROM game g WHERE (g.home_team_id = 6 OR g.away_team_id = 6) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 12;
INSERT INTO user_attended (user_id, game_id)
SELECT 81, g.id FROM game g WHERE (g.home_team_id = 6 OR g.away_team_id = 6) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 28;
INSERT INTO user_attended (user_id, game_id)
SELECT 82, g.id FROM game g WHERE (g.home_team_id = 6 OR g.away_team_id = 6) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 25;
INSERT INTO user_attended (user_id, game_id)
SELECT 83, g.id FROM game g WHERE (g.home_team_id = 6 OR g.away_team_id = 6) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 22;
INSERT INTO user_attended (user_id, game_id)
SELECT 84, g.id FROM game g WHERE (g.home_team_id = 6 OR g.away_team_id = 6) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 19;
INSERT INTO user_attended (user_id, game_id)
SELECT 85, g.id FROM game g WHERE (g.home_team_id = 6 OR g.away_team_id = 6) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 23;
INSERT INTO user_attended (user_id, game_id)
SELECT 86, g.id FROM game g WHERE (g.home_team_id = 6 OR g.away_team_id = 6) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 16;
INSERT INTO user_attended (user_id, game_id)
SELECT 87, g.id FROM game g WHERE (g.home_team_id = 6 OR g.away_team_id = 6) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 26;
INSERT INTO user_attended (user_id, game_id)
SELECT 88, g.id FROM game g WHERE (g.home_team_id = 6 OR g.away_team_id = 6) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 20;
INSERT INTO user_attended (user_id, game_id)
SELECT 89, g.id FROM game g WHERE (g.home_team_id = 6 OR g.away_team_id = 6) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 18;
INSERT INTO user_attended (user_id, game_id)
SELECT 90, g.id FROM game g WHERE (g.home_team_id = 6 OR g.away_team_id = 6) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 24;

-- SSG 랜더스 팬들 (ID 19,20,21,91~100) - 13명
INSERT INTO user_attended (user_id, game_id)
SELECT 19, g.id FROM game g WHERE (g.home_team_id = 7 OR g.away_team_id = 7) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 15;
INSERT INTO user_attended (user_id, game_id)
SELECT 20, g.id FROM game g WHERE (g.home_team_id = 7 OR g.away_team_id = 7) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 13;
INSERT INTO user_attended (user_id, game_id)
SELECT 21, g.id FROM game g WHERE (g.home_team_id = 7 OR g.away_team_id = 7) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 11;
INSERT INTO user_attended (user_id, game_id)
SELECT 91, g.id FROM game g WHERE (g.home_team_id = 7 OR g.away_team_id = 7) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 27;
INSERT INTO user_attended (user_id, game_id)
SELECT 92, g.id FROM game g WHERE (g.home_team_id = 7 OR g.away_team_id = 7) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 24;
INSERT INTO user_attended (user_id, game_id)
SELECT 93, g.id FROM game g WHERE (g.home_team_id = 7 OR g.away_team_id = 7) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 21;
INSERT INTO user_attended (user_id, game_id)
SELECT 94, g.id FROM game g WHERE (g.home_team_id = 7 OR g.away_team_id = 7) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 18;
INSERT INTO user_attended (user_id, game_id)
SELECT 95, g.id FROM game g WHERE (g.home_team_id = 7 OR g.away_team_id = 7) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 22;
INSERT INTO user_attended (user_id, game_id)
SELECT 96, g.id FROM game g WHERE (g.home_team_id = 7 OR g.away_team_id = 7) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 16;
INSERT INTO user_attended (user_id, game_id)
SELECT 97, g.id FROM game g WHERE (g.home_team_id = 7 OR g.away_team_id = 7) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 25;
INSERT INTO user_attended (user_id, game_id)
SELECT 98, g.id FROM game g WHERE (g.home_team_id = 7 OR g.away_team_id = 7) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 19;
INSERT INTO user_attended (user_id, game_id)
SELECT 99, g.id FROM game g WHERE (g.home_team_id = 7 OR g.away_team_id = 7) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 17;
INSERT INTO user_attended (user_id, game_id)
SELECT 100, g.id FROM game g WHERE (g.home_team_id = 7 OR g.away_team_id = 7) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 23;

-- 두산 베어스 팬들 (ID 25,26,27,111~120) - 13명
INSERT INTO user_attended (user_id, game_id)
SELECT 25, g.id FROM game g WHERE (g.home_team_id = 9 OR g.away_team_id = 9) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 18;
INSERT INTO user_attended (user_id, game_id)
SELECT 26, g.id FROM game g WHERE (g.home_team_id = 9 OR g.away_team_id = 9) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 15;
INSERT INTO user_attended (user_id, game_id)
SELECT 27, g.id FROM game g WHERE (g.home_team_id = 9 OR g.away_team_id = 9) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 13;
INSERT INTO user_attended (user_id, game_id)
SELECT 111, g.id FROM game g WHERE (g.home_team_id = 9 OR g.away_team_id = 9) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 29;
INSERT INTO user_attended (user_id, game_id)
SELECT 112, g.id FROM game g WHERE (g.home_team_id = 9 OR g.away_team_id = 9) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 26;
INSERT INTO user_attended (user_id, game_id)
SELECT 113, g.id FROM game g WHERE (g.home_team_id = 9 OR g.away_team_id = 9) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 23;
INSERT INTO user_attended (user_id, game_id)
SELECT 114, g.id FROM game g WHERE (g.home_team_id = 9 OR g.away_team_id = 9) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 20;
INSERT INTO user_attended (user_id, game_id)
SELECT 115, g.id FROM game g WHERE (g.home_team_id = 9 OR g.away_team_id = 9) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 24;
INSERT INTO user_attended (user_id, game_id)
SELECT 116, g.id FROM game g WHERE (g.home_team_id = 9 OR g.away_team_id = 9) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 17;
INSERT INTO user_attended (user_id, game_id)
SELECT 117, g.id FROM game g WHERE (g.home_team_id = 9 OR g.away_team_id = 9) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 27;
INSERT INTO user_attended (user_id, game_id)
SELECT 118, g.id FROM game g WHERE (g.home_team_id = 9 OR g.away_team_id = 9) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 21;
INSERT INTO user_attended (user_id, game_id)
SELECT 119, g.id FROM game g WHERE (g.home_team_id = 9 OR g.away_team_id = 9) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 19;
INSERT INTO user_attended (user_id, game_id)
SELECT 120, g.id FROM game g WHERE (g.home_team_id = 9 OR g.away_team_id = 9) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 25;

-- 키움 히어로즈 팬들 (ID 28,29,30,121~130) - 13명
INSERT INTO user_attended (user_id, game_id)
SELECT 28, g.id FROM game g WHERE (g.home_team_id = 10 OR g.away_team_id = 10) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 16;
INSERT INTO user_attended (user_id, game_id)
SELECT 29, g.id FROM game g WHERE (g.home_team_id = 10 OR g.away_team_id = 10) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 14;
INSERT INTO user_attended (user_id, game_id)
SELECT 30, g.id FROM game g WHERE (g.home_team_id = 10 OR g.away_team_id = 10) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 11;
INSERT INTO user_attended (user_id, game_id)
SELECT 121, g.id FROM game g WHERE (g.home_team_id = 10 OR g.away_team_id = 10) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 28;
INSERT INTO user_attended (user_id, game_id)
SELECT 122, g.id FROM game g WHERE (g.home_team_id = 10 OR g.away_team_id = 10) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 25;
INSERT INTO user_attended (user_id, game_id)
SELECT 123, g.id FROM game g WHERE (g.home_team_id = 10 OR g.away_team_id = 10) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 22;
INSERT INTO user_attended (user_id, game_id)
SELECT 124, g.id FROM game g WHERE (g.home_team_id = 10 OR g.away_team_id = 10) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 19;
INSERT INTO user_attended (user_id, game_id)
SELECT 125, g.id FROM game g WHERE (g.home_team_id = 10 OR g.away_team_id = 10) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 23;
INSERT INTO user_attended (user_id, game_id)
SELECT 126, g.id FROM game g WHERE (g.home_team_id = 10 OR g.away_team_id = 10) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 16;
INSERT INTO user_attended (user_id, game_id)
SELECT 127, g.id FROM game g WHERE (g.home_team_id = 10 OR g.away_team_id = 10) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 26;
INSERT INTO user_attended (user_id, game_id)
SELECT 128, g.id FROM game g WHERE (g.home_team_id = 10 OR g.away_team_id = 10) AND g.status = 'FINISHED' ORDER BY g.date_time DESC LIMIT 20;
INSERT INTO user_attended (user_id, game_id)
SELECT 129, g.id FROM game g WHERE (g.home_team_id = 10 OR g.away_team_id = 10) AND g.status = 'FINISHED' ORDER BY RAND() LIMIT 18;
INSERT INTO user_attended (user_id, game_id)
SELECT 130, g.id FROM game g WHERE (g.home_team_id = 10 OR g.away_team_id = 10) AND g.status = 'FINISHED' ORDER BY g.date_time LIMIT 24;
