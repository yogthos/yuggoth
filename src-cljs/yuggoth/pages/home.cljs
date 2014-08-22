(ns yuggoth.pages.home
  (:require [secretary.core :as secretary
             :include-macros true]
            [reagent.core :as reagent :refer [atom]]
            [yuggoth.session :as session]
            [yuggoth.components.sidebar :refer [sidebar]]
            [yuggoth.components.comments :refer [comments]]
            [yuggoth.util
             :refer [GET
                     POST
                     text
                     markdown
                     link
                     set-title!
                     set-recent!
                     set-current-post!
                     set-location!
                     set-post-url]]))

(defn toggle-public! []
  (POST "/toggle-post"
        {:params
          {:post
            (select-keys
              (session/get :post)
              [:id :public])}
         :handler
          (fn [[result]]
            (when (pos? result)
              (set-recent!)
              (session/update-in! [:post :public] not)))}))

(defn fetch-post [next?]
  (GET "/blog-post"
       {:params {:id (session/get-in [:post (if next? :next :last)])}
        :handler #(do (set-current-post! %)
                      (set-post-url %))}))

(defn tags []
  [:div (map (fn [tag]
               [link {:class "tag"} (str "#/tag/" tag) [:span.tagon tag]])
             (session/get-in [:post :tags]))])

(defn admin-forms []
  [:div.post-admin-menu
   [:div.leftmost
    [:span.button
     {:on-click #(toggle-public!)}
     (if (session/get-in [:post :public])
       (text :hide) (text :show))]]
   [:div.leftmost
    [:span.button
     {:on-click #(set-location! "#/edit-post")}
     (text :edit)]]])

(defn post-nav []
  [:div.postnav
   (when (session/get-in [:post :last])
     [:span.button.leftmost {:on-click #(fetch-post false)}
      (str "⪦" (text :previous))])
   (when (session/get-in [:post :next])
     [:span.button.rightmost {:on-click #(fetch-post true)}
      (str (text :next) "⪧")])])

(defn loading-spinner []
  [:div.spinner
   [:div.rect1]
   [:div.rect2]
   [:div.rect3]
   [:div.rect4]
   [:div.rect5]])

(defn home-page []
  (let [{:keys [id content time title author]}
        (session/get :post)]
    (set-title! title)
    [:div.contents
     (if id
       [:div{:class (if (session/get :mobile?) "post-mobile" "post")}
        [:div.entry-title [:h2 title ] [:span time]]
        (when (and (session/get :admin)
                   (pos? (session/get-in [:post :id])))
          [admin-forms])
        [:div.entry-content
         [:div.post-content (markdown content)]
         [tags]
         [post-nav]
         [:br]
         [comments]]]
       [loading-spinner])
     [sidebar]]))
