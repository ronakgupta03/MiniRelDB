-- ============================================================
-- MINIRELDB PRESENTATION SQL QUERIES
-- 20-Table Blog Database
-- Demonstrates: joins, insert, select, create, drop, delete, subqueries
-- Normalization: 1NF, 2NF, 3NF, BCNF, 4NF, 5NF
-- ============================================================

-- ============================================================
-- PART 1: CREATE TABLE STATEMENTS
-- ============================================================

-- 1. Users Table
CREATE TABLE users (
    id INT PRIMARY KEY,
    username VARCHAR(50),
    email VARCHAR(100),
    password_hash VARCHAR(255),
    full_name VARCHAR(100),
    bio TEXT,
    avatar_url VARCHAR(255),
    role VARCHAR(20),
    created_at VARCHAR(50),
    updated_at VARCHAR(50),
    is_active INT
);

-- 2. Posts Table
CREATE TABLE posts (
    id INT PRIMARY KEY,
    author_id INT,
    title VARCHAR(255),
    slug VARCHAR(255),
    content TEXT,
    excerpt TEXT,
    status VARCHAR(20),
    visibility VARCHAR(20),
    published_at VARCHAR(50),
    created_at VARCHAR(50),
    updated_at VARCHAR(50),
    view_count INT
);

-- 3. Categories Table
CREATE TABLE categories (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    slug VARCHAR(100),
    description TEXT,
    parent_id INT,
    display_order INT
);

-- 4. Post_Categories Junction Table (BCNF)
CREATE TABLE post_categories (
    post_id INT,
    category_id INT,
    PRIMARY KEY (post_id, category_id)
);

-- 5. Tags Table
CREATE TABLE tags (
    id INT PRIMARY KEY,
    name VARCHAR(50),
    slug VARCHAR(50),
    description TEXT
);

-- 6. Post_Tags Junction Table (4NF - no multi-valued deps)
CREATE TABLE post_tags (
    post_id INT,
    tag_id INT,
    PRIMARY KEY (post_id, tag_id)
);

-- 7. Comments Table
CREATE TABLE comments (
    id INT PRIMARY KEY,
    post_id INT,
    author_id INT,
    parent_comment_id INT,
    content TEXT,
    status VARCHAR(20),
    created_at VARCHAR(50),
    updated_at VARCHAR(50)
);

-- 8. Media Table
CREATE TABLE media (
    id INT PRIMARY KEY,
    post_id INT,
    uploader_id INT,
    filename VARCHAR(255),
    file_type VARCHAR(50),
    file_size INT,
    url VARCHAR(255),
    thumbnail_url VARCHAR(255),
    width INT,
    height INT,
    alt_text VARCHAR(255),
    caption TEXT,
    uploaded_at VARCHAR(50)
);

-- 9. Likes Table (4NF - separate interactions)
CREATE TABLE likes (
    id INT PRIMARY KEY,
    user_id INT,
    post_id INT,
    comment_id INT,
    liked_at VARCHAR(50)
);

-- 10. Shares Table (4NF)
CREATE TABLE shares (
    id INT PRIMARY KEY,
    user_id INT,
    post_id INT,
    platform VARCHAR(50),
    shared_at VARCHAR(50)
);

-- 11. Followers Table (BCNF - many-to-many)
CREATE TABLE followers (
    follower_id INT,
    following_id INT,
    followed_at VARCHAR(50),
    PRIMARY KEY (follower_id, following_id)
);

-- 12. Subscriptions Table
CREATE TABLE subscriptions (
    id INT PRIMARY KEY,
    user_id INT,
    plan_type VARCHAR(50),
    start_date VARCHAR(50),
    end_date VARCHAR(50),
    status VARCHAR(20),
    auto_renew INT
);

-- 13. Payments Table (2NF - amount depends on subscription, not user)
CREATE TABLE payments (
    id INT PRIMARY KEY,
    subscription_id INT,
    user_id INT,
    amount VARCHAR(20),
    currency VARCHAR(3),
    payment_method VARCHAR(50),
    transaction_id VARCHAR(100),
    status VARCHAR(20),
    payment_date VARCHAR(50)
);

-- 14. Advertisements Table (BCNF)
CREATE TABLE advertisements (
    id INT PRIMARY KEY,
    advertiser_id INT,
    title VARCHAR(255),
    content TEXT,
    target_url VARCHAR(255),
    image_url VARCHAR(255),
    impressions INT,
    clicks INT,
    budget VARCHAR(20),
    daily_budget VARCHAR(20),
    start_date VARCHAR(50),
    end_date VARCHAR(50),
    status VARCHAR(20)
);

-- 15. Ad_Clicks Table (BCNF)
CREATE TABLE ad_clicks (
    id INT PRIMARY KEY,
    ad_id INT,
    user_id INT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    clicked_at VARCHAR(50)
);

-- 16. Page_Views Table (4NF)
CREATE TABLE page_views (
    id INT PRIMARY KEY,
    post_id INT,
    user_id INT,
    session_id VARCHAR(100),
    ip_address VARCHAR(45),
    referrer VARCHAR(255),
    user_agent TEXT,
    view_date VARCHAR(50),
    duration_seconds INT
);

-- 17. Search_Keywords Table (4NF)
CREATE TABLE search_keywords (
    id INT PRIMARY KEY,
    user_id INT,
    keyword VARCHAR(255),
    results_count INT,
    clicked_post_id INT,
    searched_at VARCHAR(50)
);

-- 18. Bookmarks Table
CREATE TABLE bookmarks (
    id INT PRIMARY KEY,
    user_id INT,
    post_id INT,
    note TEXT,
    bookmarked_at VARCHAR(50)
);

-- 19. Notifications Table
CREATE TABLE notifications (
    id INT PRIMARY KEY,
    user_id INT,
    type VARCHAR(50),
    title VARCHAR(255),
    message TEXT,
    link VARCHAR(255),
    is_read INT,
    created_at VARCHAR(50)
);

-- 20. Pages Table
CREATE TABLE pages (
    id INT PRIMARY KEY,
    title VARCHAR(255),
    slug VARCHAR(255),
    content TEXT,
    template VARCHAR(50),
    meta_title VARCHAR(255),
    meta_description TEXT,
    status VARCHAR(20),
    created_at VARCHAR(50),
    updated_at VARCHAR(50)
);


-- ============================================================
-- PART 2: INSERT DATA (Sample Data)
-- ============================================================

-- Insert Users
INSERT INTO users VALUES (1, 'john_doe', 'john@example.com', 'hash123', 'John Doe', 'Bio here', 'https://img.com/1', 'admin', '2024-01-01', '2024-01-01', 1);
INSERT INTO users VALUES (2, 'jane_smith', 'jane@example.com', 'hash456', 'Jane Smith', 'Developer', 'https://img.com/2', 'author', '2024-01-02', '2024-01-02', 1);
INSERT INTO users VALUES (3, 'bob_wilson', 'bob@example.com', 'hash789', 'Bob Wilson', 'Tech enthusiast', 'https://img.com/3', 'user', '2024-01-03', '2024-01-03', 1);
INSERT INTO users VALUES (4, 'alice_brown', 'alice@example.com', 'hash101', 'Alice Brown', 'Blogger', 'https://img.com/4', 'author', '2024-01-04', '2024-01-04', 1);
INSERT INTO users VALUES (5, 'charlie_davis', 'charlie@example.com', 'hash202', 'Charlie Davis', 'Writer', 'https://img.com/5', 'user', '2024-01-05', '2024-01-05', 1);

-- Insert Categories
INSERT INTO categories VALUES (1, 'Technology', 'technology', 'Tech related posts', NULL, 1);
INSERT INTO categories VALUES (2, 'Programming', 'programming', 'Coding articles', 1, 2);
INSERT INTO categories VALUES (3, 'Web Development', 'web-dev', 'Web tips', 1, 3);
INSERT INTO categories VALUES (4, 'Database', 'database', 'DB articles', 1, 4);
INSERT INTO categories VALUES (5, 'Lifestyle', 'lifestyle', 'Life posts', NULL, 5);
INSERT INTO categories VALUES (6, 'Travel', 'travel', 'Travel stories', 5, 6);
INSERT INTO categories VALUES (7, 'Food', 'food', 'Food reviews', 5, 7);
INSERT INTO categories VALUES (8, 'Business', 'business', 'Business articles', NULL, 8);

-- Insert Posts
INSERT INTO posts VALUES (1, 1, 'Introduction to SQL', 'intro-sql', 'SQL is powerful...', 'Learn SQL basics', 'published', 'public', '2024-01-10', '2024-01-10', '2024-01-10', 1500);
INSERT INTO posts VALUES (2, 2, 'Understanding Normal Forms', 'normal-forms', '1NF 2NF 3NF...', 'Database normalization', 'published', 'public', '2024-01-11', '2024-01-11', '2024-01-11', 2300);
INSERT INTO posts VALUES (3, 2, 'BCNF Explained', 'bcnf-explained', 'Boyce-Codd Normal Form...', 'BCNF tutorial', 'published', 'public', '2024-01-12', '2024-01-12', '2024-01-12', 1800);
INSERT INTO posts VALUES (4, 3, 'Java Programming', 'java-programming', 'Java is versatile...', 'Java basics', 'published', 'public', '2024-01-13', '2024-01-13', '2024-01-13', 2100);
INSERT INTO posts VALUES (5, 4, 'Web Dev Tips', 'web-dev-tips', 'Build better websites...', 'Web tips', 'published', 'public', '2024-01-14', '2024-01-14', '2024-01-14', 1200);
INSERT INTO posts VALUES (6, 1, 'Database Design', 'database-design', 'Design efficient DB...', 'DB design guide', 'published', 'public', '2024-01-15', '2024-01-15', '2024-01-15', 3000);
INSERT INTO posts VALUES (7, 4, 'Travel Guide Europe', 'travel-europe', 'Best places in Europe...', 'Europe travel', 'published', 'public', '2024-01-16', '2024-01-16', '2024-01-16', 2500);
INSERT INTO posts VALUES (8, 2, 'Coding Best Practices', 'coding-best-practices', 'Write clean code...', 'Code tips', 'published', 'private', '2024-01-17', '2024-01-17', '2024-01-17', 1600);

-- Insert Tags
INSERT INTO tags VALUES (1, 'sql', 'sql', 'SQL language');
INSERT INTO tags VALUES (2, 'database', 'database', 'Database topics');
INSERT INTO tags VALUES (3, 'programming', 'programming', 'Coding');
INSERT INTO tags VALUES (4, 'web', 'web', 'Web development');
INSERT INTO tags VALUES (5, 'java', 'java', 'Java language');
INSERT INTO tags VALUES (6, 'tutorial', 'tutorial', 'How-to guides');
INSERT INTO tags VALUES (7, 'tips', 'tips', 'Helpful tips');
INSERT INTO tags VALUES (8, 'travel', 'travel', 'Travel stories');

-- Insert Post_Categories (Junction Table - BCNF)
INSERT INTO post_categories VALUES (1, 1);
INSERT INTO post_categories VALUES (1, 2);
INSERT INTO post_categories VALUES (2, 1);
INSERT INTO post_categories VALUES (2, 4);
INSERT INTO post_categories VALUES (3, 1);
INSERT INTO post_categories VALUES (3, 4);
INSERT INTO post_categories VALUES (4, 1);
INSERT INTO post_categories VALUES (4, 3);
INSERT INTO post_categories VALUES (5, 3);
INSERT INTO post_categories VALUES (6, 1);
INSERT INTO post_categories VALUES (6, 4);
INSERT INTO post_categories VALUES (7, 5);
INSERT INTO post_categories VALUES (7, 6);

-- Insert Post_Tags (Junction Table - 4NF)
INSERT INTO post_tags VALUES (1, 1);
INSERT INTO post_tags VALUES (1, 2);
INSERT INTO post_tags VALUES (1, 6);
INSERT INTO post_tags VALUES (2, 2);
INSERT INTO post_tags VALUES (2, 6);
INSERT INTO post_tags VALUES (3, 2);
INSERT INTO post_tags VALUES (3, 6);
INSERT INTO post_tags VALUES (4, 3);
INSERT INTO post_tags VALUES (4, 5);
INSERT INTO post_tags VALUES (5, 4);
INSERT INTO post_tags VALUES (5, 7);
INSERT INTO post_tags VALUES (6, 2);
INSERT INTO post_tags VALUES (6, 7);
INSERT INTO post_tags VALUES (7, 8);
INSERT INTO post_tags VALUES (8, 3);
INSERT INTO post_tags VALUES (8, 7);

-- Insert Comments
INSERT INTO comments VALUES (1, 1, 2, NULL, 'Great article!', 'approved', '2024-01-10', '2024-01-10');
INSERT INTO comments VALUES (2, 1, 3, NULL, 'Very helpful, thanks!', 'approved', '2024-01-10', '2024-01-10');
INSERT INTO comments VALUES (3, 2, 1, NULL, 'Well explained!', 'approved', '2024-01-11', '2024-01-11');
INSERT INTO comments VALUES (4, 2, 4, 3, 'Agreed!', 'approved', '2024-01-11', '2024-01-11');
INSERT INTO comments VALUES (5, 3, 2, NULL, 'Could you add more examples?', 'pending', '2024-01-12', '2024-01-12');
INSERT INTO comments VALUES (6, 4, 1, NULL, 'Love this!', 'approved', '2024-01-13', '2024-01-13');
INSERT INTO comments VALUES (7, 5, 3, NULL, 'Very useful tips', 'approved', '2024-01-14', '2024-01-14');
INSERT INTO comments VALUES (8, 6, 4, NULL, 'Best guide ever!', 'approved', '2024-01-15', '2024-01-15');

-- Insert Media
INSERT INTO media VALUES (1, 1, 1, 'sql-intro.jpg', 'image', 50000, 'https://img.com/sql.jpg', 'https://img.com/sql-thumb.jpg', 800, 600, 'SQL Logo', 'Intro to SQL', '2024-01-10');
INSERT INTO media VALUES (2, 2, 2, 'normal-forms.png', 'image', 75000, 'https://img.com/nf.png', 'https://img.com/nf-thumb.png', 1024, 768, 'Normal Forms', 'NF Diagram', '2024-01-11');
INSERT INTO media VALUES (3, 3, 2, 'bcnf-diagram.jpg', 'image', 60000, 'https://img.com/bcnf.jpg', 'https://img.com/bcnf-thumb.jpg', 800, 600, 'BCNF', 'BCNF Explanation', '2024-01-12');
INSERT INTO media VALUES (4, 6, 1, 'db-design.png', 'image', 80000, 'https://img.com/db.png', 'https://img.com/db-thumb.png', 1024, 768, 'DB Design', 'Database Design', '2024-01-15');

-- Insert Likes
INSERT INTO likes VALUES (1, 2, 1, NULL, '2024-01-10');
INSERT INTO likes VALUES (2, 3, 1, NULL, '2024-01-10');
INSERT INTO likes VALUES (3, 4, 1, NULL, '2024-01-10');
INSERT INTO likes VALUES (4, 1, 2, NULL, '2024-01-11');
INSERT INTO likes VALUES (5, 3, 2, NULL, '2024-01-11');
INSERT INTO likes VALUES (6, 4, 3, NULL, '2024-01-12');
INSERT INTO likes VALUES (7, 2, 4, NULL, '2024-01-13');
INSERT INTO likes VALUES (8, 5, 5, NULL, '2024-01-14');
INSERT INTO likes VALUES (9, 1, 6, NULL, '2024-01-15');
INSERT INTO likes VALUES (10, 3, 6, NULL, '2024-01-15');

-- Insert Shares
INSERT INTO shares VALUES (1, 2, 1, 'twitter', '2024-01-10');
INSERT INTO shares VALUES (2, 3, 1, 'facebook', '2024-01-10');
INSERT INTO shares VALUES (3, 4, 2, 'twitter', '2024-01-11');
INSERT INTO shares VALUES (4, 1, 3, 'linkedin', '2024-01-12');
INSERT INTO shares VALUES (5, 5, 5, 'twitter', '2024-01-14');

-- Insert Followers (BCNF - many-to-many)
INSERT INTO followers VALUES (1, 2, '2024-01-05');
INSERT INTO followers VALUES (1, 3, '2024-01-05');
INSERT INTO followers VALUES (2, 1, '2024-01-06');
INSERT INTO followers VALUES (2, 3, '2024-01-06');
INSERT INTO followers VALUES (3, 1, '2024-01-07');
INSERT INTO followers VALUES (3, 2, '2024-01-07');
INSERT INTO followers VALUES (4, 1, '2024-01-08');
INSERT INTO followers VALUES (4, 2, '2024-01-08');
INSERT INTO followers VALUES (5, 1, '2024-01-09');

-- Insert Subscriptions
INSERT INTO subscriptions VALUES (1, 1, 'premium', '2024-01-01', '2025-01-01', 'active', 1);
INSERT INTO subscriptions VALUES (2, 2, 'basic', '2024-01-02', '2025-01-02', 'active', 1);
INSERT INTO subscriptions VALUES (3, 3, 'free', '2024-01-03', '2024-02-03', 'expired', 0);
INSERT INTO subscriptions VALUES (4, 4, 'premium', '2024-01-04', '2025-01-04', 'active', 1);

-- Insert Payments
INSERT INTO payments VALUES (1, 1, 1, '99.99', 'USD', 'credit_card', 'TXN001', 'success', '2024-01-01');
INSERT INTO payments VALUES (2, 2, 2, '49.99', 'USD', 'paypal', 'TXN002', 'success', '2024-01-02');
INSERT INTO payments VALUES (3, 3, 3, '0.00', 'USD', 'free', 'TXN003', 'success', '2024-01-03');
INSERT INTO payments VALUES (4, 4, 4, '99.99', 'USD', 'credit_card', 'TXN004', 'success', '2024-01-04');

-- Insert Advertisements
INSERT INTO advertisements VALUES (1, 1, 'Learn SQL Course', 'Enroll now...', 'https://course.com/sql', 'https://ads.com/course.jpg', 10000, 250, '500.00', '50.00', '2024-01-01', '2024-12-31', 'active');
INSERT INTO advertisements VALUES (2, 2, 'Web Hosting', 'Fast hosting...', 'https://host.com', 'https://ads.com/host.jpg', 5000, 100, '200.00', '20.00', '2024-01-15', '2024-06-30', 'active');

-- Insert Ad_Clicks
INSERT INTO ad_clicks VALUES (1, 1, 1, '192.168.1.1', 'Chrome', '2024-01-15');
INSERT INTO ad_clicks VALUES (2, 1, 2, '192.168.1.2', 'Firefox', '2024-01-15');
INSERT INTO ad_clicks VALUES (3, 2, 1, '192.168.1.3', 'Safari', '2024-01-16');
INSERT INTO ad_clicks VALUES (4, 2, 3, '192.168.1.4', 'Chrome', '2024-01-16');

-- Insert Page_Views
INSERT INTO page_views VALUES (1, 1, 1, 'SESS001', '192.168.1.1', 'google.com', 'Chrome', '2024-01-15', 120);
INSERT INTO page_views VALUES (2, 2, 2, 'SESS002', '192.168.1.2', 'twitter.com', 'Firefox', '2024-01-15', 180);
INSERT INTO page_views VALUES (3, 1, 3, 'SESS003', '192.168.1.3', 'facebook.com', 'Safari', '2024-01-16', 90);
INSERT INTO page_views VALUES (4, 3, 1, 'SESS004', '192.168.1.1', NULL, 'Chrome', '2024-01-16', 200);
INSERT INTO page_views VALUES (5, 4, 2, 'SESS005', '192.168.1.2', 'google.com', 'Firefox', '2024-01-17', 150);

-- Insert Search_Keywords
INSERT INTO search_keywords VALUES (1, 1, 'sql tutorial', 50, 1, '2024-01-15');
INSERT INTO search_keywords VALUES (2, 2, 'normal forms', 25, 2, '2024-01-15');
INSERT INTO search_keywords VALUES (3, 3, 'java programming', 100, 4, '2024-01-16');
INSERT INTO search_keywords VALUES (4, 1, 'database design', 30, 6, '2024-01-16');
INSERT INTO search_keywords VALUES (5, 4, 'web development', 75, 5, '2024-01-17');

-- Insert Bookmarks
INSERT INTO bookmarks VALUES (1, 1, 1, 'To read later', '2024-01-15');
INSERT INTO bookmarks VALUES (2, 2, 2, 'Important', '2024-01-15');
INSERT INTO bookmarks VALUES (3, 3, 1, NULL, '2024-01-16');

-- Insert Notifications
INSERT INTO notifications VALUES (1, 2, 'comment', 'New comment on your post', 'john_doe commented...', '/post/1', 0, '2024-01-15');
INSERT INTO notifications VALUES (2, 3, 'like', 'Someone liked your post', 'jane_smith liked...', '/post/2', 1, '2024-01-15');
INSERT INTO notifications VALUES (3, 1, 'follow', 'New follower', 'bob_wilson followed you', '/user/bob', 0, '2024-01-16');

-- Insert Pages
INSERT INTO pages VALUES (1, 'About Us', 'about', 'We are a tech blog...', 'default', 'About Us - TechBlog', 'Learn about our mission', 'published', '2024-01-01', '2024-01-01');
INSERT INTO pages VALUES (2, 'Contact', 'contact', 'Get in touch...', 'contact', 'Contact - TechBlog', 'Reach us anytime', 'published', '2024-01-02', '2024-01-02');


-- ============================================================
-- PART 3: BASIC CRUD OPERATIONS
-- ============================================================

-- INSERT Examples
INSERT INTO users VALUES (6, 'new_user', 'new@example.com', 'newhash', 'New User', 'New bio', 'https://img.com/new', 'user', '2024-01-20', '2024-01-20', 1);

-- SELECT Examples
SELECT * FROM users;
SELECT id, username, email FROM users;
SELECT * FROM posts WHERE status = 'published';
SELECT * FROM posts WHERE view_count > 2000;

-- UPDATE Examples
UPDATE users SET full_name = 'John Updated' WHERE id = 1;
UPDATE posts SET view_count = view_count + 1 WHERE id = 1;

-- DELETE Examples
DELETE FROM users WHERE id = 6;
DELETE FROM posts WHERE status = 'draft';


-- ============================================================
-- PART 4: JOINS - Demonstration
-- ============================================================

-- INNER JOIN: Get posts with author names
SELECT p.id, p.title, u.username AS author 
FROM posts p 
INNER JOIN users u ON p.author_id = u.id;

-- LEFT JOIN: All posts with their categories (even if no category)
SELECT p.title, c.name AS category 
FROM posts p 
LEFT JOIN post_categories pc ON p.id = pc.post_id 
LEFT JOIN categories c ON pc.category_id = c.id;

-- RIGHT JOIN: All categories with post counts
SELECT c.name, COUNT(pc.post_id) AS post_count 
FROM categories c 
LEFT JOIN post_categories pc ON c.id = pc.category_id 
GROUP BY c.name;

-- MULTI-JOIN: Posts with author and category
SELECT p.title, u.username AS author, c.name AS category
FROM posts p
JOIN users u ON p.author_id = u.id
JOIN post_categories pc ON p.id = pc.post_id
JOIN categories c ON pc.category_id = c.id;

-- SELF JOIN: Users following other users
SELECT u1.username AS follower, u2.username AS following
FROM followers f
JOIN users u1 ON f.follower_id = u1.id
JOIN users u2 ON f.following_id = u2.id;

-- JOIN with Aggregation: Posts with like counts
SELECT p.title, COUNT(l.id) AS like_count
FROM posts p
LEFT JOIN likes l ON p.id = l.post_id
GROUP BY p.title;


-- ============================================================
-- PART 5: SUBQUERIES - Demonstration
-- ============================================================

-- Subquery in WHERE: Posts by authors with more than 2 posts
SELECT title, author_id 
FROM posts 
WHERE author_id IN (
    SELECT author_id 
    FROM posts 
    GROUP BY author_id 
    HAVING COUNT(*) > 2
);

-- Subquery in FROM: Average post views per author
SELECT author_name, AVG(view_count) AS avg_views
FROM (
    SELECT u.username AS author_name, p.view_count
    FROM posts p
    JOIN users u ON p.author_id = u.id
) author_stats
GROUP BY author_name;

-- Subquery in SELECT: Posts with comment count
SELECT p.title, p.view_count,
    (SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id) AS comment_count
FROM posts p;

-- Correlated Subquery: Users who have never posted
SELECT username FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM posts p WHERE p.author_id = u.id
);

-- Subquery with ANY/ALL: Posts with above average views
SELECT title, view_count
FROM posts
WHERE view_count > (
    SELECT AVG(view_count) FROM posts
);


-- ============================================================
-- PART 6: NORMALIZATION DEMONSTRATION
-- ============================================================

-- ============================================================
-- 1NF (First Normal Form) - Atomic Values
-- ============================================================
-- Each column contains atomic, indivisible values
-- NO repeating groups or arrays

-- Example: Users table is in 1NF
-- Each field (id, username, email) contains single values

-- ============================================================
-- 2NF (Second Normal Form) - No Partial Dependencies
-- ============================================================
-- Must be in 1NF
-- No non-key attribute depends on part of a composite primary key

-- Example: payments table is in 2NF
-- amount depends on subscription_id (the whole key), not partially
-- user_id is stored for convenience but doesn't cause dependency

-- ============================================================
-- 3NF (Third Normal Form) - No Transitive Dependencies
-- ============================================================
-- Must be in 2NF
-- No transitive dependencies (non-key -> non-key -> non-key)

-- Example: posts table - view_count depends on id (PK), nothing else
-- author_id is FK, title/content are independent data

-- ============================================================
-- BCNF (Boyce-Codd Normal Form)
-- ============================================================
-- Every determinant must be a candidate key

-- Example: post_categories junction table
-- (post_id, category_id) -> only these together identify a row
-- Neither post_id nor category_id alone determines the other

-- ============================================================
-- 4NF (Fourth Normal Form)
-- ============================================================
-- Must be in BCNF
-- No multi-valued dependencies

-- Example: post_tags table (4NF compliant)
-- A post can have multiple tags, but tags are independent
-- No dependency like: post_id ->> tag_id (multi-valued)

-- Example: likes table (4NF)
-- Likes are independent of comments - can like post without commenting

-- ============================================================
-- 5NF (Fifth Normal Form / Project-Join Normal Form)
-- ============================================================
-- Must be in 4NF
-- Cannot be decomposed into smaller tables without losing data

-- Example: Complex relationships preserved
-- post_categories + post_tags + posts can reconstruct all relationships

-- Demonstration: Reconstruct original data through joins
SELECT p.title, c.name AS category, t.name AS tag
FROM posts p
LEFT JOIN post_categories pc ON p.id = pc.post_id
LEFT JOIN categories c ON pc.category_id = c.id
LEFT JOIN post_tags pt ON p.id = pt.post_id
LEFT JOIN tags t ON pt.tag_id = t.id;


-- ============================================================
-- PART 7: ADVANCED QUERIES
-- ============================================================

-- GROUP BY with HAVING
SELECT author_id, COUNT(*) AS post_count, SUM(view_count) AS total_views
FROM posts
GROUP BY author_id
HAVING COUNT(*) > 2;

-- ORDER BY with multiple columns
SELECT * FROM posts ORDER BY status ASC, view_count DESC;

-- LIMIT and OFFSET
SELECT * FROM posts ORDER BY view_count DESC LIMIT 3 OFFSET 2;

-- DISTINCT
SELECT DISTINCT author_id FROM posts;

-- LIKE pattern matching
SELECT * FROM posts WHERE title LIKE '%SQL%';
SELECT * FROM users WHERE email LIKE '%@example.com';

-- BETWEEN operator
SELECT * FROM posts WHERE view_count BETWEEN 1000 AND 2000;

-- IN operator
SELECT * FROM posts WHERE author_id IN (1, 2, 3);

-- UNION
SELECT username FROM users WHERE role = 'admin'
UNION
SELECT username FROM users WHERE is_active = 1;


-- ============================================================
-- PART 8: DROP TABLE (Demonstration)
-- ============================================================

-- Drop a table (careful!)
-- DROP TABLE IF EXISTS test_table;


-- ============================================================
-- PART 9: QUERY OPTIMIZATION WITH INDEX (if supported)
-- ============================================================

-- Create index for faster queries
-- CREATE INDEX idx_posts_author ON posts(author_id);
-- CREATE INDEX idx_posts_views ON posts(view_count);

-- Query using index (faster for large datasets)
SELECT * FROM posts WHERE author_id = 1;


-- ============================================================
-- END OF PRESENTATION QUERIES
-- ============================================================
