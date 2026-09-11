-- Remove answer sheets that no longer belong to an existing room.
DELETE ca
FROM wts_card_answer ca
LEFT JOIN wts_card c ON c.ID = ca.CARDID
LEFT JOIN wts_room r ON r.ID = c.ROOMID
WHERE c.ID IS NULL OR r.ID IS NULL;

DELETE cp
FROM wts_card_point cp
LEFT JOIN wts_card c ON c.ID = cp.CARDID
LEFT JOIN wts_room r ON r.ID = c.ROOMID
WHERE c.ID IS NULL OR r.ID IS NULL;

DELETE c
FROM wts_card c
LEFT JOIN wts_room r ON r.ID = c.ROOMID
WHERE r.ID IS NULL;
