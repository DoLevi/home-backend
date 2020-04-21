SELECT
    s.id,
    s.date_bought,
    s.product_name,
    CASE WHEN s.consumer_id = ? THEN s.price * s.consumption_share / s.share_sum
         ELSE 0
    END AS price
FROM (
    SELECT
        p.id,
        p.date_bought,
        p.product_name,
        m.user_id AS consumer_id,
        p.price,
        m.consumption_share,
        SUM(m.consumption_share) OVER (PARTITION BY p.id) AS share_sum,
        (RANK() OVER (PARTITION BY p.id ORDER BY (p.buyer_user_id = ?)::int + (m.user_id = ?)::int DESC)) = 1 AS is_relevant
    FROM purchases p
    JOIN purchase_mapping m ON p.id = m.purchase_id
    WHERE p.buyer_user_id = ? OR m.user_id = ? AND p.date_bought >= ? AND p.date_bought <= ?) s
WHERE is_relevant
ORDER BY s.date_bought DESC, s.id