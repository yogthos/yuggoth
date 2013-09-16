(ns yuggoth.models.schema  
  (:require [clojure.java.jdbc :as sql]))

;;file table
(defn create-file-table []
  (sql/create-table
    :file
    [:type "varchar(50)"]
    [:name "varchar(50)"]
    [:data "bytea"]))

;;blog table
; DONE - Add columns for page? and slug to support "static" pages
(defn create-blog-table []
  (sql/create-table
    :blog
    [:id "SERIAL"]
    [:time :timestamp]
    [:title "varchar(100)"]
    [:tease "TEXT"]
    [:content "TEXT"]
    [:author "varchar(100)"]
    [:slug "varchar(100)"]
    [:public :boolean]
    [:page :boolean]))

;;comment table 
(defn create-comments-table []
  (sql/create-table
    :comment
    [:id "SERIAL"]
    [:blogid :int]
    [:time :timestamp]    
    [:content "TEXT"]
    [:approved :boolean]
    [:author "varchar(100)"]))

;;tag table
(defn create-tag-table []
  (do
    (sql/create-table
     :tag
     [:id "SERIAL"]
     [:name "varchar(50)"]
     [:slug "varchar(50)"])
    #_(create-index :tag_slug :tag [:slug] :unique)))

(defn create-tag-map-table []
  (sql/create-table
    :tag_map
    [:blogid :int]
    [:tagid :int]))

;;admin table
(defn create-admin-table []
  (sql/create-table
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
   (sql/drop-table table)
   (catch Exception _)))

(defn reset-blog [db]  
  (sql/with-connection 
    db
    (drop-table :admin)
    (drop-table :blog)
    (drop-table :comment)
    (drop-table :file)
    (drop-table :tag)
    (drop-table :tag_map)
    (sql/transaction
      (create-admin-table)
      (create-blog-table)
      (create-comments-table)
      (create-file-table)
      (create-tag-table)
      (create-tag-map-table))    
    nil))
