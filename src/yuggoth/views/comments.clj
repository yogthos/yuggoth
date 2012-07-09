(ns yuggoth.views.comments
  (:use hiccup.element hiccup.form noir.core)
  (:require [yuggoth.views.util :as util]
            [yuggoth.models.db :as db]
            [noir.session :as session]
            [noir.response :as resp])
  (:import net.sf.jlue.util.Captcha
           javax.imageio.ImageIO
           [java.io ByteArrayInputStream ByteArrayOutputStream]))

(defn append-comment [comments {:keys [author content time]}]
  (conj
    comments
    [:div.comment
     [:h4 (util/format-time time) " - " author]
     [:div#comment-content (markdown/md-to-html-string content)]]))

(defn get-comments [blog-id]
  (let [header [:div.comments [:h2 "comments"] [:hr]]]
    (if-let [comments (db/get-comments blog-id)]
      (conj (reduce append-comment header comments) [:hr])
      header)))

(defn make-comment [blog-id]    
  (form-to [:post "/comment"]
           (hidden-field "blog-id" blog-id)           
           (if-let [admin (session/get :admin)]
             (hidden-field "author" (:handle admin))
             [:div
              (text-field {:tabindex 2} "author" "anonymous")
              [:br]
              [:div#captcha-image(image "/captcha")]
              [:div#captcha-text (text-field  {:placeholder "captcha" :tabindex 3} "captcha")]]) 
           
           [:br]
           (text-area {:id "comment" :placeholder "comment" :tabindex 4} "content") [:br]
           (submit-button {:tabindex 5} "submit")))

(defpage [:post "/comment"] {:keys [blog-id captcha content author]}
  (when (and (or (session/get :admin) (= captcha (:text (session/get :captcha)))) 
             (not-empty author) 
             (not-empty content)) 
    (db/add-comment blog-id content author))  
  (resp/redirect (str "/blog/" blog-id)))

(defn gen-captcha-text []
  (->> #(rand-int 26) (repeatedly 6) (map (partial + 97)) (map char) (apply str)))

(defn gen-captcha []
  (let [text (gen-captcha-text)
        captcha (doto (new Captcha))]
    (session/put! :captcha {:text text :image (.gen captcha text 250 40)})))

(defpage "/captcha" []
  (gen-captcha)
  (resp/content-type 
    "image/jpeg" 
    (let [out (new ByteArrayOutputStream)]
      (ImageIO/write (:image (session/get :captcha)) "jpeg" out)
      (new ByteArrayInputStream (.toByteArray out)))))

