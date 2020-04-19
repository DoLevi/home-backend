SELECT u1.username AS buyer, u2.username AS consumer, SUM(p.price * m.consumption_share / s.share_sum) AS delta
FROM purchases p
JOIN (SELECT p.id, SUM(m.consumption_share) as share_sum
    FROM purchases p
    JOIN purchase_mapping m ON p.id = m.purchase_id
    GROUP BY p.id) s on p.id = s.id
JOIN purchase_mapping m ON p.id = m.purchase_id
JOIN users u1 ON p.buyer_user_id = u1.id
JOIN users u2 ON m.user_id = u2.id
WHERE (u1.username = ? OR u2.username = ?) AND p.date_bought >= ? AND p.date_bought <= ?
GROUP BY u1.username, u2.username