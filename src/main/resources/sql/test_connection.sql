SELECT 1
FROM purchase_mapping pm
JOIN purchases p ON pm.purchase_id = p.id
JOIN users u ON pm.user_id = u.id