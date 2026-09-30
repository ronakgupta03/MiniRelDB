#!/usr/bin/env python3
"""
Generate a Chen-style ER Diagram for TuxPages database
Uses classic ER notation: rectangles for entities, diamonds for relationships,
ovals for attributes, and lines with cardinality notation.
"""

import math
from xml.etree.ElementTree import Element, SubElement, tostring
from xml.dom import minidom

# Entity definitions with their attributes
ENTITIES = {
    "USERS": {
        "attrs": [
            "user_id",
            "email",
            "username",
            "password_hash",
            "name",
            "college",
            "branch",
            "city",
            "country",
            "profile_photo",
            "is_verified",
            "is_admin",
            "token_version",
            "created_at",
            "updated_at",
        ],
        "pk": "user_id",
    },
    "CATEGORIES": {
        "attrs": [
            "category_id",
            "name",
            "slug",
            "description",
            "parent_id",
            "display_order",
        ],
        "pk": "category_id",
    },
    "TAGS": {"attrs": ["tag_id", "name", "slug", "description"], "pk": "tag_id"},
    "POSTS": {
        "attrs": [
            "post_id",
            "author_id",
            "title",
            "slug",
            "content",
            "excerpt",
            "status",
            "visibility",
            "published_at",
            "created_at",
            "updated_at",
            "view_count",
        ],
        "pk": "post_id",
    },
    "POST_CATEGORIES": {
        "attrs": ["id", "post_id", "category_id"],
        "pk": "id",
        "weak": True,
    },
    "POST_TAGS": {"attrs": ["id", "post_id", "tag_id"], "pk": "id", "weak": True},
    "COMMENTS": {
        "attrs": [
            "comment_id",
            "post_id",
            "user_id",
            "parent_id",
            "content",
            "upvotes",
            "downvotes",
            "created_at",
            "updated_at",
        ],
        "pk": "comment_id",
    },
    "MEDIA": {
        "attrs": [
            "media_id",
            "post_id",
            "uploader_id",
            "filename",
            "file_type",
            "file_size",
            "url",
            "thumb_url",
            "width",
            "height",
            "alt_text",
            "caption",
            "uploaded_at",
        ],
        "pk": "media_id",
    },
    "PAGES": {
        "attrs": [
            "page_id",
            "title",
            "slug",
            "content",
            "template",
            "meta_title",
            "meta_desc",
            "status",
            "created_at",
            "updated_at",
        ],
        "pk": "page_id",
    },
    "LIKES": {"attrs": ["like_id", "user_id", "post_id", "liked_at"], "pk": "like_id"},
    "SHARES": {
        "attrs": ["share_id", "user_id", "post_id", "platform", "shared_at"],
        "pk": "share_id",
    },
    "FOLLOWERS": {
        "attrs": ["id", "follower_id", "following_id", "followed_at"],
        "pk": "id",
    },
    "SUBSCRIPTIONS": {
        "attrs": [
            "sub_id",
            "user_id",
            "plan",
            "start_date",
            "end_date",
            "status",
            "auto_renew",
        ],
        "pk": "sub_id",
    },
    "PAYMENTS": {
        "attrs": [
            "payment_id",
            "sub_id",
            "user_id",
            "amount",
            "currency",
            "method",
            "transaction_id",
            "status",
            "payment_date",
        ],
        "pk": "payment_id",
    },
    "ADVERTISEMENTS": {
        "attrs": [
            "ad_id",
            "advertiser_id",
            "title",
            "content",
            "target_url",
            "image_url",
            "impressions",
            "clicks",
            "budget",
            "daily_budget",
            "start_date",
            "end_date",
            "status",
        ],
        "pk": "ad_id",
    },
    "AD_CLICKS": {
        "attrs": [
            "click_id",
            "ad_id",
            "user_id",
            "ip_address",
            "user_agent",
            "clicked_at",
        ],
        "pk": "click_id",
    },
    "PAGE_VIEWS": {
        "attrs": [
            "view_id",
            "post_id",
            "user_id",
            "session_id",
            "ip_address",
            "referrer",
            "user_agent",
            "view_date",
            "duration",
        ],
        "pk": "view_id",
    },
    "SEARCH_KEYWORDS": {
        "attrs": [
            "search_id",
            "user_id",
            "keyword",
            "results_count",
            "clicked_post_id",
            "searched_at",
        ],
        "pk": "search_id",
    },
    "BOOKMARKS": {
        "attrs": ["bookmark_id", "user_id", "post_id", "note", "bookmarked_at"],
        "pk": "bookmark_id",
    },
    "NOTIFICATIONS": {
        "attrs": [
            "notif_id",
            "user_id",
            "type",
            "title",
            "message",
            "link",
            "is_read",
            "created_at",
        ],
        "pk": "notif_id",
    },
}

# Relationships with cardinality
RELATIONSHIPS = [
    ("USERS", "writes", "POSTS", "1", "1..*"),
    ("USERS", "authors", "COMMENTS", "1", "1..*"),
    ("USERS", "creates", "MEDIA", "1", "1..*"),
    ("USERS", "likes", "LIKES", "1", "1..*"),
    ("USERS", "shares", "SHARES", "1", "1..*"),
    ("USERS", "follows", "FOLLOWERS", "1", "1..*"),
    ("USERS", "subscribes", "SUBSCRIPTIONS", "1", "1..*"),
    ("USERS", "pays", "PAYMENTS", "1", "1..*"),
    ("USERS", "advertises", "ADVERTISEMENTS", "1", "1..*"),
    ("USERS", "clicks", "AD_CLICKS", "1", "1..*"),
    ("USERS", "views", "PAGE_VIEWS", "1", "1..*"),
    ("USERS", "searches", "SEARCH_KEYWORDS", "1", "1..*"),
    ("USERS", "bookmarks", "BOOKMARKS", "1", "1..*"),
    ("USERS", "receives", "NOTIFICATIONS", "1", "1..*"),
    ("POSTS", "has", "COMMENTS", "1", "0..*"),
    ("POSTS", "contains", "MEDIA", "1", "0..*"),
    ("POSTS", "has", "LIKES", "1", "0..*"),
    ("POSTS", "has", "SHARES", "1", "0..*"),
    ("POSTS", "has", "PAGE_VIEWS", "1", "0..*"),
    ("POSTS", "has", "BOOKMARKS", "1", "0..*"),
    ("POSTS", "categorized_in", "POST_CATEGORIES", "1", "1..*"),
    ("POSTS", "tagged_in", "POST_TAGS", "1", "1..*"),
    ("COMMENTS", "parent_of", "COMMENTS", "0..*", "0..*"),
    ("COMMENTS", "has", "LIKES", "1", "0..*"),
    ("CATEGORIES", "contains", "POST_CATEGORIES", "1", "1..*"),
    ("TAGS", "applied_to", "POST_TAGS", "1", "1..*"),
    ("SUBSCRIPTION", "generates", "PAYMENTS", "1", "1..*"),
    ("ADVERTISEMENT", "receives", "AD_CLICKS", "1", "0..*"),
]

# Layout positions for entities
ENTITY_POSITIONS = {
    "USERS": (200, 180),
    "POSTS": (700, 180),
    "CATEGORIES": (1200, 180),
    "TAGS": (1600, 180),
    "COMMENTS": (700, 650),
    "MEDIA": (1200, 650),
    "PAGES": (1600, 650),
    "LIKES": (200, 900),
    "SHARES": (500, 900),
    "FOLLOWERS": (800, 900),
    "SUBSCRIPTIONS": (1100, 900),
    "PAYMENTS": (1400, 900),
    "ADVERTISEMENTS": (200, 1200),
    "AD_CLICKS": (500, 1200),
    "PAGE_VIEWS": (800, 1200),
    "SEARCH_KEYWORDS": (1100, 1200),
    "BOOKMARKS": (1400, 1200),
    "NOTIFICATIONS": (1700, 1200),
    "POST_CATEGORIES": (1000, 1400),
    "POST_TAGS": (1300, 1400),
}


def generate_svg():
    width = 2200
    height = 1700

    svg = Element(
        "svg",
        {
            "xmlns": "http://www.w3.org/2000/svg",
            "width": str(width),
            "height": str(height),
            "viewBox": f"0 0 {width} {height}",
        },
    )

    # Styles
    defs = SubElement(svg, "defs")

    # Gradient for entities
    gradient = SubElement(
        defs,
        "linearGradient",
        {"id": "entityGrad", "x1": "0%", "y1": "0%", "x2": "100%", "y2": "100%"},
    )
    SubElement(
        gradient, "stop", {"offset": "0%", "style": "stop-color:#2a3f5f;stop-opacity:1"}
    )
    SubElement(
        gradient,
        "stop",
        {"offset": "100%", "style": "stop-color:#1a2a3f;stop-opacity:1"},
    )

    # Gradient for relationships
    rel_grad = SubElement(
        defs,
        "linearGradient",
        {"id": "relGrad", "x1": "0%", "y1": "0%", "x2": "100%", "y2": "100%"},
    )
    SubElement(
        rel_grad, "stop", {"offset": "0%", "style": "stop-color:#3d5a80;stop-opacity:1"}
    )
    SubElement(
        rel_grad,
        "stop",
        {"offset": "100%", "style": "stop-color:#2d4a70;stop-opacity:1"},
    )

    # Gradient for weak entities
    weak_grad = SubElement(
        defs,
        "linearGradient",
        {"id": "weakGrad", "x1": "0%", "y1": "0%", "x2": "100%", "y2": "100%"},
    )
    SubElement(
        weak_grad,
        "stop",
        {"offset": "0%", "style": "stop-color:#3a3550;stop-opacity:1"},
    )
    SubElement(
        weak_grad,
        "stop",
        {"offset": "100%", "style": "stop-color:#2a2540;stop-opacity:1"},
    )

    # Arrow marker
    marker = SubElement(
        defs,
        "marker",
        {
            "id": "arrow",
            "markerWidth": "10",
            "markerHeight": "10",
            "refX": "9",
            "refY": "3",
            "orient": "auto",
            "markerUnits": "strokeWidth",
        },
    )
    SubElement(marker, "path", {"d": "M0,0 L0,6 L9,3 z", "fill": "#5a8fa8"})

    # Background
    bg = SubElement(
        svg,
        "rect",
        {
            "x": "0",
            "y": "0",
            "width": str(width),
            "height": str(height),
            "fill": "#0d1520",
        },
    )

    # Grid pattern
    grid_group = SubElement(svg, "g", {"id": "grid", "opacity": "0.1"})
    for i in range(0, width, 50):
        line = SubElement(
            grid_group,
            "line",
            {
                "x1": str(i),
                "y1": "0",
                "x2": str(i),
                "y2": str(height),
                "stroke": "#4a6fa5",
                "stroke-width": "1",
            },
        )
    for i in range(0, height, 50):
        line = SubElement(
            grid_group,
            "line",
            {
                "x1": "0",
                "y1": str(i),
                "x2": str(width),
                "y2": str(i),
                "stroke": "#4a6fa5",
                "stroke-width": "1",
            },
        )

    # Title
    title = SubElement(
        svg,
        "text",
        {
            "x": str(width // 2),
            "y": "40",
            "text-anchor": "middle",
            "font-family": "Arial, sans-serif",
            "font-size": "28",
            "font-weight": "bold",
            "fill": "#7eb8da",
        },
    )
    title.text = "TuxPages Database - Entity Relationship Diagram"

    subtitle = SubElement(
        svg,
        "text",
        {
            "x": str(width // 2),
            "y": "65",
            "text-anchor": "middle",
            "font-family": "Arial, sans-serif",
            "font-size": "14",
            "fill": "#5a8fa8",
        },
    )
    subtitle.text = "Chen Notation Style"

    # Draw entities
    entity_boxes = {}
    for name, data in ENTITIES.items():
        pos = ENTITY_POSITIONS.get(name, (100, 100))
        x, y = pos

        # Calculate entity box size based on attributes
        num_attrs = len(data["attrs"])
        box_height = max(60, 35 + num_attrs * 18)
        box_width = max(160, max(len(attr) for attr in data["attrs"]) * 8 + 80)

        # Entity rectangle group
        g = SubElement(svg, "g", {"id": f"entity_{name}"})

        # Shadow
        shadow = SubElement(
            g,
            "rect",
            {
                "x": str(x + 3),
                "y": str(y + 3),
                "width": str(box_width),
                "height": str(box_height),
                "rx": "8",
                "ry": "8",
                "fill": "rgba(0,0,0,0.3)",
            },
        )

        # Main rectangle
        if data.get("weak", False):
            rect = SubElement(
                g,
                "rect",
                {
                    "x": str(x),
                    "y": str(y),
                    "width": str(box_width),
                    "height": str(box_height),
                    "rx": "8",
                    "ry": "8",
                    "fill": "url(#weakGrad)",
                    "stroke": "#6a5a80",
                    "stroke-width": "2",
                },
            )
            # Double border for weak entity
            rect2 = SubElement(
                g,
                "rect",
                {
                    "x": str(x - 2),
                    "y": str(y - 2),
                    "width": str(box_width + 4),
                    "height": str(box_height + 4),
                    "rx": "10",
                    "ry": "10",
                    "fill": "none",
                    "stroke": "#6a5a80",
                    "stroke-width": "1",
                },
            )
        else:
            rect = SubElement(
                g,
                "rect",
                {
                    "x": str(x),
                    "y": str(y),
                    "width": str(box_width),
                    "height": str(box_height),
                    "rx": "8",
                    "ry": "8",
                    "fill": "url(#entityGrad)",
                    "stroke": "#4a7fa8",
                    "stroke-width": "2",
                },
            )

        # Entity name header
        header = SubElement(
            g,
            "rect",
            {
                "x": str(x),
                "y": str(y),
                "width": str(box_width),
                "height": "30",
                "rx": "8",
                "ry": "8",
                "fill": "#3a6090",
            },
        )
        # Cover bottom corners of header
        cover = SubElement(
            g,
            "rect",
            {
                "x": str(x),
                "y": str(y + 20),
                "width": str(box_width),
                "height": "10",
                "fill": "#3a6090",
            },
        )

        # Entity name
        name_text = SubElement(
            g,
            "text",
            {
                "x": str(x + box_width // 2),
                "y": str(y + 20),
                "text-anchor": "middle",
                "font-family": "Arial, sans-serif",
                "font-size": "14",
                "font-weight": "bold",
                "fill": "#ffffff",
            },
        )
        name_text.text = name

        # Attributes
        for i, attr in enumerate(data["attrs"]):
            attr_y = y + 38 + i * 18

            # Attribute name
            attr_text = SubElement(
                g,
                "text",
                {
                    "x": str(x + 12),
                    "y": str(attr_y),
                    "font-family": "Monospace, monospace",
                    "font-size": "11",
                    "fill": "#a0c0e0",
                },
            )
            is_pk = attr == data["pk"]
            if is_pk:
                # PK indicator
                pk_circle = SubElement(
                    g,
                    "circle",
                    {
                        "cx": str(x + 6),
                        "cy": str(attr_y - 4),
                        "r": "4",
                        "fill": "#e8b86d",
                    },
                )
                attr_text.set("fill", "#e8b86d")
                # Underline for PK
                underline = SubElement(
                    g,
                    "line",
                    {
                        "x1": str(x + 12),
                        "y1": str(attr_y + 4),
                        "x2": str(x + 12 + len(attr) * 6),
                        "y2": str(attr_y + 4),
                        "stroke": "#e8b86d",
                        "stroke-width": "1",
                    },
                )
            attr_text.text = attr

        entity_boxes[name] = (x + box_width, y + box_height // 2)

    # Draw relationships as diamonds
    rel_positions = [
        ("USERS", "writes", "POSTS", 450, 360),
        ("USERS", "authors", "COMMENTS", 450, 540),
        ("POSTS", "has", "COMMENTS", 850, 480),
        ("USERS", "likes", "LIKES", 280, 700),
        ("POSTS", "has", "LIKES", 600, 540),
        ("USERS", "shares", "SHARES", 350, 700),
        ("POSTS", "has", "SHARES", 700, 540),
        ("USERS", "follows", "FOLLOWERS", 500, 700),
        ("USERS", "subscribes", "SUBSCRIPTIONS", 650, 700),
        ("SUBSCRIPTIONS", "generates", "PAYMENTS", 1250, 700),
        ("USERS", "advertises", "ADVERTISEMENTS", 310, 1000),
        ("ADVERTISEMENTS", "receives", "AD_CLICKS", 350, 1100),
        ("USERS", "views", "PAGE_VIEWS", 500, 1000),
        ("POSTS", "has", "PAGE_VIEWS", 750, 700),
        ("USERS", "searches", "SEARCH_KEYWORDS", 650, 1000),
        ("USERS", "bookmarks", "BOOKMARKS", 800, 1000),
        ("POSTS", "has", "BOOKMARKS", 950, 700),
        ("USERS", "receives", "NOTIFICATIONS", 900, 1000),
        ("POSTS", "categorized_in", "POST_CATEGORIES", 900, 900),
        ("CATEGORIES", "contains", "POST_CATEGORIES", 1100, 900),
        ("POSTS", "tagged_in", "POST_TAGS", 1000, 900),
        ("TAGS", "applied_to", "POST_TAGS", 1450, 900),
    ]

    drawn_rels = set()
    for from_ent, rel_name, to_ent, rx, ry in rel_positions:
        key = tuple(sorted([from_ent, to_ent]))
        if key in drawn_rels:
            continue
        drawn_rels.add(key)

        # Get entity positions
        from_pos = ENTITY_POSITIONS.get(from_ent, (0, 0))
        to_pos = ENTITY_POSITIONS.get(to_ent, (0, 0))

        # Calculate center points
        from_center = (from_pos[0] + 100, from_pos[1] + 50)
        to_center = (to_pos[0] + 100, to_pos[1] + 50)

        # Draw diamond (relationship)
        diamond_size = 60
        g = SubElement(svg, "g", {"id": f"rel_{rel_name}"})

        # Diamond shape
        points = f"{rx},{ry - diamond_size} {rx + diamond_size},{ry} {rx},{ry + diamond_size} {rx - diamond_size},{ry}"
        diamond = SubElement(
            g,
            "polygon",
            {
                "points": points,
                "fill": "url(#relGrad)",
                "stroke": "#6a9fbf",
                "stroke-width": "2",
            },
        )

        # Relationship name
        rel_text = SubElement(
            g,
            "text",
            {
                "x": str(rx),
                "y": str(ry + 4),
                "text-anchor": "middle",
                "font-family": "Arial, sans-serif",
                "font-size": "9",
                "font-weight": "bold",
                "fill": "#ffffff",
            },
        )
        rel_text.text = rel_name

        # Draw lines from entities to relationship
        # From entity line
        line1 = SubElement(
            svg,
            "line",
            {
                "x1": str(from_center[0]),
                "y1": str(from_center[1]),
                "x2": str(rx),
                "y2": str(ry - diamond_size),
                "stroke": "#5a8fa8",
                "stroke-width": "2",
            },
        )

        # To entity line
        line2 = SubElement(
            svg,
            "line",
            {
                "x1": str(rx),
                "y1": str(ry + diamond_size),
                "x2": str(to_center[0]),
                "y2": str(to_center[1]),
                "stroke": "#5a8fa8",
                "stroke-width": "2",
            },
        )

    # Legend
    legend_x, legend_y = 100, 1550

    # Legend box
    legend_bg = SubElement(
        svg,
        "rect",
        {
            "x": str(legend_x),
            "y": str(legend_y),
            "width": "400",
            "height": "130",
            "rx": "8",
            "ry": "8",
            "fill": "rgba(30, 50, 70, 0.9)",
            "stroke": "#4a6fa5",
            "stroke-width": "1",
        },
    )

    legend_title = SubElement(
        svg,
        "text",
        {
            "x": str(legend_x + 20),
            "y": str(legend_y + 22),
            "font-family": "Arial, sans-serif",
            "font-size": "14",
            "font-weight": "bold",
            "fill": "#7eb8da",
        },
    )
    legend_title.text = "Legend"

    # Legend items
    legend_items = [
        ("rect", "Entity (Strong)", "#2a3f5f"),
        ("rect", "Entity (Weak)", "#3a3550"),
        ("diamond", "Relationship", "#3d5a80"),
        ("circle", "Primary Key", "#e8b86d"),
    ]

    for i, (shape, label, color) in enumerate(legend_items):
        y_pos = legend_y + 45 + i * 20

        if shape == "rect":
            shape_elem = SubElement(
                svg,
                "rect",
                {
                    "x": str(legend_x + 15),
                    "y": str(y_pos - 8),
                    "width": "20",
                    "height": "14",
                    "fill": color,
                    "stroke": "#5a8fa8",
                },
            )
        elif shape == "diamond":
            points = f"{legend_x + 25},{y_pos - 8} {legend_x + 35},{y_pos} {legend_x + 25},{y_pos + 8} {legend_x + 15},{y_pos}"
            shape_elem = SubElement(
                svg, "polygon", {"points": points, "fill": color, "stroke": "#5a8fa8"}
            )
        elif shape == "circle":
            shape_elem = SubElement(
                svg,
                "circle",
                {"cx": str(legend_x + 25), "cy": str(y_pos), "r": "6", "fill": color},
            )

        label_text = SubElement(
            svg,
            "text",
            {
                "x": str(legend_x + 50),
                "y": str(y_pos + 4),
                "font-family": "Arial, sans-serif",
                "font-size": "12",
                "fill": "#a0c0e0",
            },
        )
        label_text.text = label

    # Cardinality notation explanation
    card_x, card_y = 550, 1550

    card_bg = SubElement(
        svg,
        "rect",
        {
            "x": str(card_x),
            "y": str(card_y),
            "width": "500",
            "height": "130",
            "rx": "8",
            "ry": "8",
            "fill": "rgba(30, 50, 70, 0.9)",
            "stroke": "#4a6fa5",
            "stroke-width": "1",
        },
    )

    card_title = SubElement(
        svg,
        "text",
        {
            "x": str(card_x + 20),
            "y": str(card_y + 22),
            "font-family": "Arial, sans-serif",
            "font-size": "14",
            "font-weight": "bold",
            "fill": "#7eb8da",
        },
    )
    card_title.text = "Cardinality Notation"

    card_items = [
        ("1", "One (exactly one)"),
        ("1..*", "One to Many"),
        ("0..*", "Zero to Many"),
    ]

    for i, (symbol, desc) in enumerate(card_items):
        y_pos = card_y + 50 + i * 25

        sym_text = SubElement(
            svg,
            "text",
            {
                "x": str(card_x + 30),
                "y": str(y_pos),
                "font-family": "Monospace, monospace",
                "font-size": "14",
                "font-weight": "bold",
                "fill": "#e8b86d",
            },
        )
        sym_text.text = symbol

        desc_text = SubElement(
            svg,
            "text",
            {
                "x": str(card_x + 80),
                "y": str(y_pos),
                "font-family": "Arial, sans-serif",
                "font-size": "12",
                "fill": "#a0c0e0",
            },
        )
        desc_text.text = desc

    # Pretty print
    rough_string = tostring(svg, encoding="unicode")
    reparsed = minidom.parseString(rough_string)
    pretty = reparsed.toprettyxml(indent="  ")

    # Remove XML declaration line
    lines = pretty.split("\n")
    lines = [l for l in lines if not l.startswith("<?xml")]
    pretty = "\n".join(lines)

    with open("/home/asus-ronak/Desktop/MiniRelDB/tuxpages_chen_er.svg", "w") as f:
        f.write(pretty)

    print("✓ Chen-style ER Diagram saved: tuxpages_chen_er.svg")


if __name__ == "__main__":
    generate_svg()
