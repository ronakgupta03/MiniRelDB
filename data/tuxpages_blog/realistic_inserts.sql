#!/usr/bin/env python3
"""Generate realistic random data for 20 tables"""
import random
import string
from datetime import datetime, timedelta

def rs(n): return ''.join(random.choices(string.ascii_letters, k=n))
def rstr(min_, max_): return ''.join(random.choices(string.ascii_letters + ' ', k=random.randint(min_, max_)))
def rname(): return random.choice(['John', 'Jane', 'Mike', 'Sarah', 'David', 'Emma', 'Chris', 'Lisa', 'Tom', 'Amy']) + ' ' + random.choice(['Smith', 'Johnson', 'Williams', 'Brown', 'Jones', 'Davis', 'Miller', 'Wilson', 'Moore', 'Taylor'])
def remail(i): return f"{random.choice(['john', 'jane', 'mike', 'sarah', 'david', 'emma', 'chris', 'lisa', 'tom', 'amy'])}{i}@example.com"
def rdate(): dt = datetime.now() - timedelta(days=random.randint(1, 365)); return f"'{dt.strftime('%Y-%m-%d %H:%M:%S')}'"
def q(s): return f"'{s}'"

# Users (50 rows)
print("-- Users (50 rows)")
for i in range(1, 51):
    print(f"INSERT INTO users VALUES ({i}, {q(rs(10))}, {q(remail(i))}, {q(rs(12))}, {q(rname())}, {q(rstr(20, 50))}, {q(f'https://img.com/avatar{i}.jpg')}, {q(random.choice(['admin', 'author', 'user', 'editor']))}, {rdate()}, {rdate()}, {random.randint(0, 1)});")

# Posts (100 rows)
print("\n-- Posts (100 rows)")
for i in range(1, 101):
    print(f"INSERT INTO posts VALUES ({i}, {random.randint(1, 50)}, {q(rstr(10, 50))}, {q(rs(8))}, {q(rstr(50, 200))}, {q(rstr(20, 80))}, {q(random.choice(['published', 'draft', 'private']))}, {q(random.choice(['public', 'private', 'friends']))}, {rdate()}, {rdate()}, {rdate()}, {random.randint(50, 5000)});")

# Categories (20 rows)
print("\n-- Categories (20 rows)")
cats = ['Technology', 'Programming', 'Web Development', 'Database', 'Lifestyle', 'Travel', 'Food', 'Business', 'Health', 'Sports', 'Science', 'Education', 'Entertainment', 'News', 'Opinion', 'Tutorial', 'Review', 'News', 'Guide', 'Opinion']
for i, c in enumerate(cats, 1):
    parent = random.randint(0, 5) if random.random() > 0.3 else 0
    print(f"INSERT INTO categories VALUES ({i}, {q(c)}, {q(c.lower().replace(' ', '-'))}, {q(rstr(20, 80))}, {parent}, {i});")

# Post_Categories (150 rows) - junction table for BCNF
print("\n-- Post_Categories (150 rows) - junction table")
for i in range(1, 151):
    print(f"INSERT INTO post_categories VALUES ({i}, {random.randint(1, 100)}, {random.randint(1, 20)});")

# Tags (30 rows)
print("\n-- Tags (30 rows)")
tag_names = ['sql', 'database', 'java', 'python', 'web', 'javascript', 'tutorial', 'tips', 'programming', 'coding', 'beginner', 'advanced', 'news', 'review', 'tutorial', 'guide', 'how-to', 'tips', 'news', 'technology', 'science', 'math', 'health', 'fitness', 'food', 'travel', 'lifestyle', 'business', 'finance', 'career']
for i, t in enumerate(tag_names, 1):
    print(f"INSERT INTO tags VALUES ({i}, {q(t)}, {q(t)}, {q(rstr(10, 40))});")

# Post_Tags (200 rows) - junction table for 4NF
print("\n-- Post_Tags (200 rows)")
for i in range(1, 201):
    print(f"INSERT INTO post_tags VALUES ({i}, {random.randint(1, 100)}, {random.randint(1, 30)});")

# Comments (200 rows)
print("\n-- Comments (200 rows)")
comments = ['Great article!', 'Very helpful, thanks!', 'Well explained!', 'Could you add more examples?', 'Love this!', 'Very useful tips', 'Best guide ever!', 'Thanks for sharing!', 'Interesting perspective', 'I disagree with some points']
for i in range(1, 201):
    parent = random.randint(1, 50) if random.random() > 0.5 else 0
    print(f"INSERT INTO comments VALUES ({i}, {random.randint(1, 100)}, {random.randint(1, 50)}, {parent}, {q(random.choice(comments))}, {q(random.choice(['approved', 'pending', 'spam']))}, {rdate()}, {rdate()});")

# Media (80 rows)
print("\n-- Media (80 rows)")
for i in range(1, 81):
    print(f"INSERT INTO media VALUES ({i}, {random.randint(1, 100)}, {random.randint(1, 50)}, {q(f'image_{i}.jpg')}, {q(random.choice(['image', 'video', 'audio']))}, {random.randint(1000, 5000000)}, {q(f'https://media.com/{i}.jpg')}, {q(f'https://media.com/thumb/{i}.jpg')}, {random.randint(100, 2000)}, {random.randint(100, 2000)}, {q(f'Image {i}')}, {q(rstr(10, 30))}, {rdate()});")

# Likes (300 rows) - 4NF demo
print("\n-- Likes (300 rows)")
for i in range(1, 301):
    print(f"INSERT INTO likes VALUES ({i}, {random.randint(1, 50)}, {random.randint(1, 100)}, {random.randint(1, 200) if random.random() > 0.5 else 0}, {rdate()});")

# Shares (100 rows) - 4NF
print("\n-- Shares (100 rows)")
for i in range(1, 101):
    print(f"INSERT INTO shares VALUES ({i}, {random.randint(1, 50)}, {random.randint(1, 100)}, {q(random.choice(['twitter', 'facebook', 'linkedin', 'reddit', 'pinterest']))}, {rdate()});")

# Bookmarks (80 rows)
print("\n-- Bookmarks (80 rows)")
for i in range(1, 81):
    note = q(rstr(10, 30)) if random.random() > 0.3 else q('')
    print(f"INSERT INTO bookmarks VALUES ({i}, {random.randint(1, 50)}, {random.randint(1, 100)}, {note}, {rdate()});")

# Followers (150 rows) - BCNF many-to-many
print("\n-- Followers (150 rows)")
for i in range(1, 151):
    print(f"INSERT INTO followers VALUES ({i}, {random.randint(1, 50)}, {random.randint(1, 50)}, {rdate()});")

# Notifications (250 rows)
print("\n-- Notifications (250 rows)")
notif_types = ['comment', 'like', 'follow', 'share', 'mention', 'system']
for i in range(1, 251):
    print(f"INSERT INTO notifications VALUES ({i}, {random.randint(1, 50)}, {q(random.choice(notif_types))}, {q(rstr(10, 30))}, {q(rstr(20, 80))}, {q(f'/post/{random.randint(1, 100)}')}, {random.randint(0, 1)}, {rdate()});")

# Subscriptions (30 rows)
print("\n-- Subscriptions (30 rows)")
plans = ['free', 'basic', 'premium', 'enterprise']
for i in range(1, 31):
    print(f"INSERT INTO subscriptions VALUES ({i}, {random.randint(1, 50)}, {q(random.choice(plans))}, {rdate()}, {rdate()}, {q(random.choice(['active', 'expired', 'cancelled']))}, {random.randint(0, 1)});")

# Payments (50 rows) - 2NF demo
print("\n-- Payments (50 rows)")
for i in range(1, 51):
    print(f"INSERT INTO payments VALUES ({i}, {random.randint(1, 30)}, {random.randint(1, 50)}, {q(f'{random.randint(10, 200)}.{random.randint(0, 99):02d}')}, {q(random.choice(['USD', 'EUR', 'GBP']))}, {q(random.choice(['credit_card', 'paypal', 'bank_transfer']))}, {q(f'TXN{i:05d}')}, {q(random.choice(['success', 'pending', 'failed']))}, {rdate()});")

# Advertisements (20 rows) - BCNF
print("\n-- Advertisements (20 rows)")
for i in range(1, 21):
    print(f"INSERT INTO advertisements VALUES ({i}, {random.randint(1, 50)}, {q(rstr(10, 30))}, {q(rstr(30, 100))}, {q(f'https://ad.com/{i}')}, {q(f'https://ads.com/banner{i}.jpg')}, {random.randint(1000, 100000)}, {random.randint(50, 5000)}, {q(f'{random.randint(100, 1000)}.{random.randint(0, 99):02d}')}, {q(f'{random.randint(10, 100)}.{random.randint(0, 99):02d}')}, {rdate()}, {rdate()}, {q(random.choice(['active', 'paused', 'ended']))});")

# Ad_Clicks (200 rows) - BCNF
print("\n-- Ad_Clicks (200 rows)")
user_agents = ['Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0', 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) Safari/604.1', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) Firefox/121.0']
for i in range(1, 201):
    print(f"INSERT INTO ad_clicks VALUES ({i}, {random.randint(1, 20)}, {random.randint(1, 50)}, {q(f'192.168.{random.randint(1, 255)}.{random.randint(1, 255)}')}, {q(random.choice(user_agents))}, {rdate()});")

# Page_Views (500 rows) - 4NF
print("\n-- Page_Views (500 rows)")
refs = ['google.com', 'facebook.com', 'twitter.com', 'linkedin.com', 'reddit.com', '']
for i in range(1, 501):
    print(f"INSERT INTO page_views VALUES ({i}, {random.randint(1, 100)}, {random.randint(1, 50)}, {q(f'SESSION{random.randint(1000, 9999)}')}, {q(f'192.168.{random.randint(1, 255)}.{random.randint(1, 255)}')}, {q(random.choice(refs))}, {q('Chrome/120')}, {rdate()}, {random.randint(10, 600)});")

# Search_Keywords (100 rows) - 4NF
print("\n-- Search_Keywords (100 rows)")
keywords = ['sql tutorial', 'python programming', 'web development', 'database design', 'java basics', 'javascript tips', 'css tricks', 'react tutorial', 'nodejs guide', 'python for beginners', 'sql joins', 'database normalization', 'bcnf explained', 'web security', 'api design']
for i in range(1, 101):
    post = random.randint(1, 100) if random.random() > 0.3 else 0
    print(f"INSERT INTO search_keywords VALUES ({i}, {random.randint(1, 50)}, {q(random.choice(keywords))}, {random.randint(5, 500)}, {post}, {rdate()});")

# Pages (15 rows)
print("\n-- Pages (15 rows)")
page_titles = ['About Us', 'Contact', 'Privacy Policy', 'Terms of Service', 'FAQ', 'Sitemap', 'Careers', 'Press', 'Advertise', 'Blog', 'Support', 'Community', 'Documentation', 'API Reference', 'Login']
for i, t in enumerate(page_titles, 1):
    print(f"INSERT INTO pages VALUES ({i}, {q(t)}, {q(t.lower().replace(' ', '-'))}, {q(rstr(50, 200))}, {q('default')}, {q(t + ' - MySite')}, {q(rstr(30, 100))}, {q(random.choice(['published', 'draft']))}, {rdate()}, {rdate()});")
