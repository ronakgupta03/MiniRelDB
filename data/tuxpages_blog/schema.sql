-- ==========================================
-- 20-Table Blog Website Database Schema
-- Designed for normalization demonstrations
-- ==========================================

-- 1. Users table (1NF - atomic values)
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

-- 2. Posts table (1NF, 2NF, 3NF)
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

-- 3. Categories table (1NF)
CREATE TABLE categories (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    slug VARCHAR(100),
    description TEXT,
    parent_id INT,
    display_order INT
);

-- 4. Post_Categories junction (BCNF - composite PK eliminates redundancies)
CREATE TABLE post_categories (
    post_id INT,
    category_id INT,
    PRIMARY KEY (post_id, category_id)
);

-- 5. Tags table (4NF - no multi-valued dependencies)
CREATE TABLE tags (
    id INT PRIMARY KEY,
    name VARCHAR(50),
    slug VARCHAR(50),
    description TEXT
);

-- 6. Post_Tags junction (4NF)
CREATE TABLE post_tags (
    post_id INT,
    tag_id INT,
    PRIMARY KEY (post_id, tag_id)
);

-- 7. Comments table (1NF, 2NF)
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

-- 8. Media table (1NF)
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

-- 9. Pages table (static pages)
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

-- 10. Likes table (4NF - separate like types)
CREATE TABLE likes (
    id INT PRIMARY KEY,
    user_id INT,
    post_id INT,
    comment_id INT,
    liked_at VARCHAR(50)
);

-- 11. Shares table (4NF)
CREATE TABLE shares (
    id INT PRIMARY KEY,
    user_id INT,
    post_id INT,
    platform VARCHAR(50),
    shared_at VARCHAR(50)
);

-- 12. Bookmarks table (BCNF)
CREATE TABLE bookmarks (
    id INT PRIMARY KEY,
    user_id INT,
    post_id INT,
    note TEXT,
    bookmarked_at VARCHAR(50)
);

-- 13. Followers table (BCNF - many-to-many)
CREATE TABLE followers (
    follower_id INT,
    following_id INT,
    followed_at VARCHAR(50),
    PRIMARY KEY (follower_id, following_id)
);

-- 14. Notifications table (1NF)
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

-- 15. Subscriptions table (1NF)
CREATE TABLE subscriptions (
    id INT PRIMARY KEY,
    user_id INT,
    plan_type VARCHAR(50),
    start_date VARCHAR(50),
    end_date VARCHAR(50),
    status VARCHAR(20),
    auto_renew INT
);

-- 16. Payments table (1NF, 2NF - amount depends on subscription, not user)
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

-- 17. Advertisements table (BCNF)
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

-- 18. Ad_Clicks table (BCNF)
CREATE TABLE ad_clicks (
    id INT PRIMARY KEY,
    ad_id INT,
    user_id INT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    clicked_at VARCHAR(50)
);

-- 19. Page_Views table (4NF)
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

-- 20. Search_Keywords table (4NF)
CREATE TABLE search_keywords (
    id INT PRIMARY KEY,
    user_id INT,
    keyword VARCHAR(255),
    results_count INT,
    clicked_post_id INT,
    searched_at VARCHAR(50)
);
