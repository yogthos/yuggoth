(ns yuggoth.pages.profile
  (:require [reagent.core :as reagent :refer [atom]]
            [yuggoth.session :as session]
            [yuggoth.util
             :refer [GET
                     POST
                     auth-hash
                     text
                     markdown
                     input-value
                     text-input]]))

(defn update-password! [pass]
  (let [old-pass (:old-pass @pass)
        new-pass (:new-pass @pass)
        repeat-pass (:repeat-pass @pass)]
    (POST "/change-admin-password"
          {:headers {"Authorization"
                     (auth-hash (session/get-in [:profile :handle]) old-pass)}
           :params {:pass new-pass
                    :repeat-pass repeat-pass}
           :handler
           (fn [_] (reset! pass {}))
           :error-handler
           (fn [response]
             (swap! pass assoc :error (get-in response [:response :error])))})))

(defn save! [uri id field]
  (fn []
    (let [value @field]
      (POST uri
        {:params {id value}
         :handler
         (fn [[status]]
           (when (= 1 status)
             (session/assoc-in! [:profile id] value)))}))))

(defn input-field [element uri id field]
  [:div
   element
   (when (not= @field (session/get-in [:profile id]))
     [:span.button
      {:on-click (save! uri id field)} (text :save)])])

(defn blog-title [title]
  [:div [:h2 (text :blog-title)]
   [input-field [text-input title] "/set-blog-title" :title title]])

(defn admin-handle [handle]
  [:div [:h2 (text :user)]
   [input-field [text-input handle] "/set-admin-handle" :handle handle]])

(defn admin-email [email]
  [:div [:h2 (text :email)]
   [input-field [text-input email] "/set-admin-email" :email email]])

(defn admin-details [about]
  [:div
   [:h2 (text :about-title)]
   [input-field
    [:textarea.edit-post-text
     {:value @about
      :on-change #(reset! about (input-value %))}]
    "/set-admin-details" :about about]])

(defn change-password []
  (let [pass (atom {})]
    (fn []
      [:div
       [:h2 (text :password)]
       [:input {:type "password"
                :placeholder (text :password)
                :value (:old-pass @pass)
                :on-change #(swap! pass assoc :old-pass (input-value %))}]
       [:br]
       [:input {:type "password"
                :placeholder (text :new-password)
                :value (:new-pass @pass)
                :on-change #(swap! pass assoc :new-pass (input-value %))}]
       [:br]
       [:input {:type "password"
                :placeholder (text :confirm-password)
                :value (:repeat-pass @pass)
                :on-change #(swap! pass assoc :repeat-pass (input-value %))}]
       [:br]
       (when-let [info (:info @pass)]
         [:div info])
       (when-let [error (:error @pass)]
         [:div.error error])
       (if (not= (:new-pass @pass) (:repeat-pass @pass))
         [:div.error (text :pass-mismatch)]
         (when (not-empty (:new-pass @pass))
          [:span.button {:on-click #(update-password! pass)} "change password"]))])))

(defn profile-page []
  (let [title (atom (session/get-in [:profile :title]))
        handle (atom (session/get-in [:profile :handle]))
        email (atom (session/get-in [:profile :email]))
        about (atom (session/get-in [:profile :about]))]
    (fn []
      [:div.contents
       [:div.edit-admin
        [blog-title title]
        [admin-handle handle]
        [admin-email email]
        [change-password]
        [admin-details about]]
       [:div.admin-preview
        [:div.entry-title [:h2 @title]]
        [:h3 @handle]
        [:h3 @email]
        [:div
         [:h2 (text :about)]
         [:div (markdown @about)]]]])))
