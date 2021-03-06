(ns boodle.model.expenses
  (:require [boodle.services.postgresql :as db]))

(defn select-all
  []
  (db/query ["SELECT id, date, id_category, category, item, amount FROM (
                SELECT e.id, TO_CHAR(e.date, 'dd/mm/yyyy') AS date,
                e.date as temp_date,
                c.id as id_category,
                c.name as category,
                e.item, e.amount FROM expenses e
                INNER JOIN categories c on e.id_category = c.id
                ORDER BY temp_date DESC
                LIMIT 20) t"]))

(defn select-by-id
  [id]
  (db/query ["SELECT * FROM expenses WHERE id = cast(? as integer)" id]))

(defn select-by-item
  [item]
  (db/query ["SELECT * FROM expenses WHERE item = ?" item]))

(defn categories-filter
  [l]
  (when-not (empty? l)
    (if (sequential? l)
      (str
       " AND e.id_category IN ("
       (->> (map #(str "cast(" % " as integer)") l)
            (interpose ", ")
            (apply str))
       ") ")
      (str "AND e.id_category = cast(" l " as integer) "))))

(defn select-by-date-and-categories
  [from to categories]
  (db/query
   [(str
     "SELECT id, date, id_category, category, item, amount FROM (
       SELECT e.id, TO_CHAR(e.date, 'dd/mm/yyyy') AS date,
       e.date as temp_date, c.id as id_category, c.name as category,
       e.item, e.amount
       FROM expenses e
       INNER JOIN categories c ON e.id_category = c.id
       WHERE e.date >= TO_DATE(?, 'DD/MM/YYYY')
       AND e.date <= TO_DATE(?, 'DD/MM/YYYY') "
     (categories-filter categories)
     " ORDER BY temp_date DESC) t")
    from to]))

(defn from-filter
  [from]
  (when-not (or (nil? from) (empty? from))
    (str " AND e.date >= TO_DATE('" from "', 'DD/MM/YYYY')")))

(defn item-filter
  [item]
  (when-not (or (nil? item) (empty? item))
    (str " AND e.item ilike '%" item "%'")))

(defn report
  [from to item categories]
  (db/query
   [(str
     "SELECT id, date, id_category, category, item, amount FROM (
       SELECT e.id, TO_CHAR(e.date, 'dd/mm/yyyy') AS date,
       e.date as temp_date, c.id as id_category, c.name as category,
       e.item, e.amount
       FROM expenses e
       INNER JOIN categories c ON e.id_category = c.id
       WHERE e.date <= TO_DATE(?, 'DD/MM/YYYY')"
     (from-filter from)
     (item-filter item)
     (categories-filter categories)
     " ORDER BY temp_date DESC) t")
    to]))

(defn totals-for-categories
  [from to item]
  (db/query
   [(str
     "SELECT id, date, id_category, category, item, amount FROM (
       SELECT e.id, TO_CHAR(e.date, 'dd/mm/yyyy') AS date,
       e.date as temp_date, c.id as id_category, c.name as category,
       e.item, e.amount
       FROM expenses e
       INNER JOIN categories c ON e.id_category = c.id
       WHERE e.date <= TO_DATE(?, 'DD/MM/YYYY')"
     (from-filter from)
     (item-filter item)
     " ORDER BY temp_date DESC) t")
    to]))

(defn insert!
  [expense]
  (let [{:keys [date id-category item amount]} expense]
    (db/update! ["INSERT INTO expenses(date, id_category, item, amount)
                  VALUES(
                    TO_DATE(?, 'DD/MM/YYYY'),
                    cast(? as integer),
                    ?,
                    cast(? as double precision)
                  )"
                 date id-category item amount])))

(defn update!
  [expense]
  (let [{:keys [id date id-category item amount]} expense]
    (db/update! ["UPDATE expenses SET date = TO_DATE(?, 'DD/MM/YYYY'),
                 id_category = cast(? as integer),
                 item = ?,
                 amount = cast(? as double precision)
                 WHERE id = ?"
                 date id-category item amount id])))

(defn delete!
  [id]
  (db/delete! ["DELETE FROM expenses WHERE id = cast(? as integer)" id]))
