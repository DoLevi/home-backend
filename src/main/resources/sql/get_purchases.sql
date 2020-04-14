SELECT p.id, p.date_bought, p.product_name, (p.price * m.consumption_share / s.share_sum) AS price
FROM purchases p JOIN purchase_mapping m ON p.id = m.purchase_id
JOIN users u ON m.user_id = u.id
JOIN (SELECT p.id, SUM(m.consumption_share) as share_sum
    FROM purchases p
    JOIN purchase_mapping m ON p.id = m.purchase_id
    GROUP BY p.id) s ON p.id = s.id
WHERE u.id = ?