#!/usr/bin/env python3
"""Generate all INSERT statements for 20 tables"""
import random
import string

def rs(n): return ''.join(random.choices(string.ascii_letters, k=n))
def ts(): return f"'{rs(8)}-{rs(4)}-{rs(4)} {rs(2)}:{rs(2)}:{rs(2)}'"
def q(s): return f"'{s}'"

# Users (50 rows)
for i in range(1, 51):
    print(f"INSERT INTO users VALUES ({i}, {q(rs(12))}, {q(f'user{i}@ex.com')}, {q(rs(8))}, {q(rs(12))}, {q(rs(20))}, {q(f'url/{i}.jpg')}, {q(random.choice(['admin','user','author']))}, {ts()}, {ts()}, {random.randint(0,1)});")

# Posts (100 rows)
for i in range(1, 101):
    print(f"INSERT INTO posts VALUES ({i}, {random.randint(1,50)}, {q(rs(15))}, {q(rs(8))}, {q(rs(30))}, {q(rs(20))}, {q(random.choice(['published','draft','private']))}, {q('public')}, {ts()}, {ts()}, {ts()}, {random.randint(100,5000)});")

# Categories (20 rows)
for i in range(1, 21):
    print(f"INSERT INTO categories VALUES ({i}, {q(rs(10))}, {q(rs(6))}, {q(rs(20))}, {random.randint(0,10)}, {i});")

# Post_Categories (150 rows)
for i in range(1, 151):
    print(f"INSERT INTO post_categories VALUES ({i}, {random.randint(1,100)}, {random.randint(1,20)});")

# Tags (30 rows)
for i in range(1, 31):
    print(f"INSERT INTO tags VALUES ({i}, {q(rs(8))}, {q(rs(6))}, {q(rs(15))});")

# Post_Tags (200 rows)
for i in range(1, 201):
    print(f"INSERT INTO post_tags VALUES ({i}, {random.randint(1,100)}, {random.randint(1,30)});")

# Comments (200 rows)
for i in range(1, 201):
    print(f"INSERT INTO comments VALUES ({i}, {random.randint(1,100)}, {random.randint(1,50)}, {random.randint(0,50) if random.random()>0.5 else 0}, {q(rs(30))}, {q('approved')}, {ts()}, {ts()});")

# Media (80 rows)
for i in range(1, 81):
    print(f"INSERT INTO media VALUES ({i}, {random.randint(1,100)}, {random.randint(1,50)}, {q(f'file{i}.jpg')}, {q('image')}, {random.randint(1000,50000)}, {q(f'url/{i}.jpg')}, {q(f'thumb/{i}.jpg')}, {random.randint(100,2000)}, {random.randint(100,2000)}, {q(f'alt{i}')}, {q(rs(15))}, {ts()});")

# Likes (300 rows)
for i in range(1, 301):
    print(f"INSERT INTO likes VALUES ({i}, {random.randint(1,50)}, {random.randint(1,100)}, {random.randint(1,200) if random.random()>0.5 else 0}, {ts()});")

# Shares (100 rows)
for i in range(1, 101):
    print(f"INSERT INTO shares VALUES ({i}, {random.randint(1,50)}, {random.randint(1,100)}, {q(random.choice(['twitter','facebook','linkedin']))}, {ts()});")

# Bookmarks (80 rows)
for i in range(1, 81):
    print(f"INSERT INTO bookmarks VALUES ({i}, {random.randint(1,50)}, {random.randint(1,100)}, {q(rs(15)) if random.random()>0.5 else q('')}, {ts()});")

# Followers (150 rows)
for i in range(1, 151):
    print(f"INSERT INTO followers VALUES ({i}, {random.randint(1,50)}, {random.randint(1,50)}, {ts()});")

# Notifications (250 rows)
for i in range(1, 251):
    print(f"INSERT INTO notifications VALUES ({i}, {random.randint(1,50)}, {q(random.choice(['comment','like','follow','share']))}, {q(f'Notif {i}')}, {q(f'Msg {i}')}, {q('/link')}, {random.randint(0,1)}, {ts()});")

# Subscriptions (30 rows)
for i in range(1, 31):
    print(f"INSERT INTO subscriptions VALUES ({i}, {random.randint(1,50)}, {q(random.choice(['free','basic','premium']))}, {ts()}, {ts()}, {q('active')}, {random.randint(0,1)});")

# Payments (50 rows)
for i in range(1, 51):
    print(f"INSERT INTO payments VALUES ({i}, {random.randint(1,30)}, {random.randint(1,50)}, {q(f'{random.randint(10,200)}.{random.randint(0,99):02d}')}, {q('USD')}, {q('credit_card')}, {q(f'TXN{i:04d}')}, {q('success')}, {ts()});")

# Advertisements (20 rows)
for i in range(1, 21):
    print(f"INSERT INTO advertisements VALUES ({i}, {random.randint(1,50)}, {q(rs(12))}, {q(rs(20))}, {q(f'url/{i}')}, {q(f'ad/{i}.jpg')}, {random.randint(1000,10000)}, {random.randint(50,500)}, {q('100.00')}, {q('10.00')}, {ts()}, {ts()}, {q('active')});")

# Ad_Clicks (200 rows)
for i in range(1, 201):
    print(f"INSERT INTO ad_clicks VALUES ({i}, {random.randint(1,20)}, {random.randint(1,50)}, {q(f'192.168.{random.randint(1,255)}.{random.randint(1,255)}')}, {q('Mozilla/5.0')}, {ts()});")

# Page_Views (500 rows)
for i in range(1, 501):
    print(f"INSERT INTO page_views VALUES ({i}, {random.randint(1,100)}, {random.randint(1,50)}, {q(f'SESS{i:05d}')}, {q(f'192.168.{random.randint(1,255)}.{random.randint(1,255)}')}, {q(random.choice(['google.com','facebook.com','']))}, {q('Chrome')}, {ts()}, {random.randint(10,600)});")

# Search_Keywords (100 rows)
for i in range(1, 101):
    print(f"INSERT INTO search_keywords VALUES ({i}, {random.randint(1,50)}, {q(rs(8))}, {random.randint(1,1000)}, {random.randint(1,100) if random.random()>0.3 else 0}, {ts()});")

# Pages (15 rows)
for i in range(1, 16):
    print(f"INSERT INTO pages VALUES ({i}, {q(rs(12))}, {q(rs(8))}, {q(rs(30))}, {q('default')}, {q(rs(15))}, {q(rs(20))}, {q('published')}, {ts()}, {ts()});")
