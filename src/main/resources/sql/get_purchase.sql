SELECT p.id, p.market, p.date_bought, p.product_category, p.product_name, p.price, u.username, json_agg(mapping_json)
FROM purchases p
JOIN users u ON p.buyer_user_id = u.id
JOIN (SELECT m.purchase_id, json_build_object('username', u.username, 'consumption_share', m.consumption_share) AS mapping_json
    FROM purchase_mapping m
    JOIN users u ON m.user_id = u.id
    WHERE m.purchase_id = ?) m ON p.id = m.purchase_id
WHERE p.id = ?
GROUP BY p.id, p.market, p.date_bought, p.product_category, p.product_name, p.price, u.username