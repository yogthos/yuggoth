(ns yuggoth.pages.post
  (:require [reagent.core :as reagent :refer [atom]]
            [yuggoth.session :as session]
            [yuggoth.pages.home :refer [home-page]]
            [yuggoth.util
             :refer [GET
                     POST
                     text
                     text-input
                     markdown
                     set-page!
                     set-title!
                     set-location!
                     set-current-post!]]))

(defn set-tag-status! [id tag-status post]
  (if (= @tag-status "tagon")
    (do
      (swap! post update-in [:tags] #(remove #{id} %))
      (reset! tag-status "tagoff"))
    (do
      (swap! post update-in [:tags] conj id)
      (reset! tag-status "tagon")))
   (swap! post assoc :saved false))

(defn tag [id post]
  (let [tag-status (atom (if (some #{id} (:tags @post))
                           "tagon" "tagoff"))]
    (fn []
      [:span {:class @tag-status
              :on-click #(set-tag-status! id tag-status post)}
       id])))

(defn edit-tags [post tags]
  (let [new-tag (atom nil)]
    (fn []
      [:div.tags
        [:span "tags: "]
        (for [id @tags] ^{:key id} [tag id post])
        [text-input new-tag {:placeholder (text :other)
                             :on-change
                             #(do
                                (reset! new-tag (-> % .-target .-value))
                                (swap! post assoc :saved false))}]
        [:span.button
          {:on-click
            #(when (not-empty @new-tag)
               (swap! tags conj @new-tag))}
          (text :add-tag)]])))

(defn save! [post]
  (fn [result]
    (swap! post assoc :saved true)
    (set-current-post! result)
    (set-page! home-page)
    (set-location! "#/blog/" (:id @post))))

(defn save-post! [post]
  [:div.save-post
   (if (:saved @post)
     [:span (text :saved)]
     [:span.button
      {:on-click
       (fn []
         (POST "/save-post"
               {:params {:post (dissoc @post :saved)}
                         :handler (save! post)}))}
      "save post"])])

(defn select-post-keys [post]
  (when post
    (select-keys post [:id :title :tags :content :public])))

(defn post-page [& [post]]
  (let [post (atom (assoc (select-post-keys post) :saved true))
        tags (-> (session/get :tags) set atom)]
    (fn []
      [:div
       [:div
        [:div.edit-post
         [:input
          {:type "text"
           :class "edit-post-title"
           :value (:title @post)
           :on-change
           #(swap! post assoc
                   :saved false
                   :title (-> % .-target .-value))}]
         [:textarea.edit-post-text
          {:value (:content @post)
           :on-change
           #(swap! post assoc
                   :saved false
                   :content (-> % .-target .-value))}]]
        [:div.post-preview
         [:div.entry-title [:h2 (:title @post)]]
         [:div (markdown (:content @post))]]]
       [edit-tags post tags]
       (when (and (not-empty (:title @post))
                  (not-empty (:content @post)))
         [save-post! post])])))

(defn make-post-page []
  (set-title! (text :new-post))
  (post-page))

(defn edit-post-page []
  (set-title! (text :edit-post))
  (post-page (session/get :post)))


