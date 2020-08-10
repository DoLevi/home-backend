SELECT
    s.id,
    s.date_bought,
    s.product_name,
    CASE
        -- verbose: (s.buyer_user_id <> ? OR s.consumer_id = ?) AND s.consumer_id = ?
        -- meaning: ? is the consumer
        WHEN s.consumer_id = ?
            THEN s.price * s.consumption_share / s.share_sum
        -- verbose: s.buyer_user_id = ? AND s.consumer_id <> ?
        -- meaning: ? is not the consumer but the buyer
        WHEN s.buyer_user_id = ?
            THEN - s.price * s.consumption_share / s.share_sum
    END AS price
FROM (
    SELECT
        p.id,
        p.date_bought,
        p.product_name,
        p.buyer_user_id,
        m.user_id AS consumer_id,
        p.price,
        m.consumption_share,
        SUM(m.consumption_share) OVER (PARTITION BY p.id) AS share_sum,
        (RANK() OVER (PARTITION BY p.id ORDER BY (p.buyer_user_id = ?)::int + (m.user_id = ?)::int DESC)) = 1 AS is_relevant
    FROM purchases p
    JOIN purchase_mapping m ON p.id = m.purchase_id) s
WHERE is_relevant AND (s.buyer_user_id = ? OR s.consumer_id = ?) AND s.date_bought >= ? AND s.date_bought <= ?
ORDER BY s.date_bought DESC, s.id