(ns storefront.components.stylist.gallery-image-picker
  (:require [storefront.accessors.auth :as auth]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.image-picker :as image-picker]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.effects :as effects]
            [storefront.hooks.uploadcare :as uploadcare]

            [storefront.component :as component :refer [defcomponent]]
            [storefront.component :as component :refer [defcomponent]]))

(defcomponent component [{:keys [loaded-uploadcare?] :as args} owner opts]
  [:div
   (when loaded-uploadcare?
     (component/build image-picker/component args opts))])

(defn query [data]
  {:loaded-uploadcare? (get-in data keypaths/loaded-uploadcare)
   :resizable-url      nil
   :on-success         events/uploadcare-api-success-upload-gallery
   ;; :widget-config      {:multiple true}
   :selector           "gallery-photo"
   :back-link          {:navigation-event events/navigate-gallery-edit
                        :back-copy        "back to gallery"}})

(defn ^:export built-component [data opts]
  (component/build component (query data) nil))

(defmethod effects/perform-effects events/navigate-gallery-image-picker [_ event args _ app-state]
  (if (auth/stylist? (auth/signed-in app-state))
    (uploadcare/insert)
    (effects/redirect events/navigate-store-gallery)))
