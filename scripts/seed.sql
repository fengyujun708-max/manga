-- 漫界测试数据（修复版 - 使用 camelCase 列名）
INSERT INTO comics (id, title, "altTitle", description, author, status, tags, rating, views, "chapterCount", "favoritesCount", "createdAt", "updatedAt")
VALUES
  (gen_random_uuid(), '海贼王', 'One Piece', '拥有财富、名声、权力，这世界上的一切的男人"海贼王"哥尔·D·罗杰，在被行刑受死之前说了一句话，让全世界的人都涌向了大海。', '尾田荣一郎', 'ongoing', '{热血,冒险,友情,战斗}', 9.5, 1000000, 1120, 50000, NOW(), NOW()),
  (gen_random_uuid(), '咒术回战', 'Jujutsu Kaisen', '虎杖悠仁是一名普通的高中生，某天他吞下了一根诅咒的手指，从此卷入了咒术的世界...', '芥见下下', 'ongoing', '{热血,战斗,奇幻,黑暗}', 9.2, 800000, 265, 35000, NOW(), NOW()),
  (gen_random_uuid(), '鬼灭之刃', 'Demon Slayer', '大正时代，少年炭治郎为了拯救变成鬼的妹妹，踏上了杀鬼之旅...', '吾峠呼世晴', 'completed', '{热血,冒险,战斗,亲情}', 9.4, 900000, 205, 40000, NOW(), NOW()),
  (gen_random_uuid(), '进击的巨人', 'Attack on Titan', '人类生活在被巨人支配的世界中，艾伦·耶格尔为了自由而战...', '谏山创', 'completed', '{黑暗,奇幻,战斗,悬疑}', 9.3, 950000, 139, 45000, NOW(), NOW()),
  (gen_random_uuid(), '一拳超人', 'One Punch Man', '埼玉是一名兴趣使然的英雄，无论什么敌人都能一拳解决...', 'ONE', 'ongoing', '{热血,搞笑,战斗,英雄}', 9.1, 750000, 245, 30000, NOW(), NOW());

-- 插入章节
DO $$
DECLARE
  cid uuid;
BEGIN
  SELECT id INTO cid FROM comics WHERE title = '海贼王' LIMIT 1;
  FOR i IN 1..10 LOOP
    INSERT INTO chapters (id, "comicId", "sourceId", title, "chapterNumber", "pageCount", "createdAt", "updatedAt")
    VALUES (gen_random_uuid(), cid, 'system', '第 ' || i || ' 话', i, 15 + i, NOW(), NOW());
  END LOOP;
END $$;

-- 插入测试用户
INSERT INTO users (id, phone, "phoneVerified", "passwordHash", nickname, role, status, "createdAt", "updatedAt")
VALUES (gen_random_uuid(), '13800138000', true, '$2a$10$dummy_hash', '测试管理员', 'admin', 'active', NOW(), NOW());
INSERT INTO users (id, phone, "phoneVerified", "passwordHash", nickname, role, status, "createdAt", "updatedAt")
VALUES (gen_random_uuid(), '13900139000', true, '$2a$10$dummy_hash2', '漫界用户', 'user', 'active', NOW(), NOW());

-- 插入公告
INSERT INTO announcements (id, title, content, priority, "isActive", "startAt", "endAt", "createdAt", "updatedAt")
VALUES (gen_random_uuid(), '漫界正式上线！', '欢迎使用漫界漫画阅读平台，这里有你喜欢的各种漫画！', 'important', true, NOW(), NOW() + INTERVAL '30 days', NOW(), NOW());

-- 插入帖子
INSERT INTO posts (id, "userId", title, content, tags, type, status, "createdAt", "updatedAt")
SELECT gen_random_uuid(), id, '推荐一部超好看的漫画', '最近发现海贼王真的太好看了，强烈推荐！', '{推荐,海贼王}', 'normal', 'published', NOW(), NOW()
FROM users WHERE nickname = '测试管理员' LIMIT 1;

INSERT INTO posts (id, "userId", title, content, tags, type, status, "createdAt", "updatedAt")
SELECT gen_random_uuid(), id, '求这部漫画的名字', '记得是几年前看的，主角有特殊能力...', '{求漫}', 'normal', 'published', NOW(), NOW()
FROM users WHERE nickname = '测试管理员' LIMIT 1;