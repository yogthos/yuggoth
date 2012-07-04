(ns yuggoth.views.util
  (:use hiccup.form))

(defn make-form [& fields]
  (reduce-kv 
    (fn [table i [id name value]]
      (conj table
            [:tr 
             [:td (label id name)] 
             [:td ((if (.startsWith id "pass") password-field text-field) 
                    {:tabindex (inc i)} id value)]]))
    [:table]
    (vec (partition 3 fields))))

(defn format-time
  ([time] (format-time time "dd MM yyyy"))
  ([time fmt]
    (.format (new java.text.SimpleDateFormat fmt) time)))
