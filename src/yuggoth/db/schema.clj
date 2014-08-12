(ns yuggoth.db.schema
  (:require [clojure.java.jdbc :as sql]))

;;file table
(defn create-file-table []
  (sql/create-table-ddl
    :file
    [:type "varchar(50)"]
    [:name "varchar(50)"]
    [:data "bytea"]))

;;blog table
(defn create-blog-table []
  (sql/create-table-ddl
    :blog
    [:id "SERIAL"]
    [:time :timestamp]
    [:title "varchar(100)"]
    [:content "TEXT"]
    [:author "varchar(100)"]
    [:public :boolean]))

;;comment table
(defn create-comments-table []
  (sql/create-table-ddl
    :comment
    [:id "SERIAL"]
    [:blogid :int]
    [:time :timestamp]
    [:content "TEXT"]
    [:author "varchar(100)"]))

;;tag table
(defn create-tag-table []
  (sql/create-table-ddl
    :tag
    [:name "varchar(50)"]))

(defn create-tag-map-table []
  (sql/create-table-ddl
    :tag_map
    [:blogid :int]
    [:tag "varchar(50)"]))

;;admin table
(defn create-admin-table []
  (sql/create-table-ddl
    :admin
    [:title "varchar(100)"]
    [:style "varchar(50)"]
    [:about "TEXT"]
    [:handle "varchar(100)"]
    [:pass   "varchar(100)"]
    [:email  "varchar(50)"]))

(defn drop-table
  "drops the supplied table from the DB, table name must be a keyword
eg: (drop-table :users)"
  [table]
  (try
   (sql/drop-table-ddl table)
   (catch Exception _)))

(defn reset-blog [db]
  (sql/with-db-transaction [t-conn db]
    (try
        (drop-table :admin)
        (drop-table :blog)
        (drop-table :comment)
        (drop-table :file)
        (drop-table :tag)
        (drop-table :tag_map)
        (catch java.sql.BatchUpdateException _))
    (sql/db-do-commands
      t-conn
      (create-admin-table)
      (create-blog-table)
      (create-comments-table)
      (create-file-table)
      (create-tag-table)
      (create-tag-map-table))))

